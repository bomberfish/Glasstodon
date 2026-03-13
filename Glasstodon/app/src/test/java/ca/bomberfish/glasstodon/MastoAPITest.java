package ca.bomberfish.glasstodon;

import ca.bomberfish.glasstodon.model.*;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Local JVM tests for MastoAPI — no Android device needed.
 *
 * Set these environment variables before running:
 *   MASTODON_INSTANCE  - e.g. "https://mastodon.social"
 *   MASTODON_TOKEN     - your access token (get one from Settings > Development > New Application)
 *
 * Run with:
 *   MASTODON_INSTANCE="https://mastodon.social" MASTODON_TOKEN="yourtoken" ./gradlew test
 *
 * Or set them in your shell profile so you don't have to type them every time.
 */
public class MastoAPITest {

    private MastoAPI api;

    @Before
    public void setUp() {
        String instance = System.getenv("MASTODON_INSTANCE");
        String token = System.getenv("MASTODON_TOKEN");

        if (instance == null || token == null) {
            fail("Set MASTODON_INSTANCE and MASTODON_TOKEN environment variables.\n"
               + "Example: MASTODON_INSTANCE=\"https://mastodon.social\" "
               + "MASTODON_TOKEN=\"yourtoken\" ./gradlew test");
        }

        api = new MastoAPI(instance, token);
    }

    @Test
    public void testGetMe() throws IOException {
        Account me = api.getMe();

        assertNotNull("getMe() returned null", me);
        assertNotNull("id is null", me.id);
        assertNotNull("username is null", me.username);
        assertNotNull("acct is null", me.acct);

        System.out.println("=== getMe() ===");
        System.out.println("ID:        " + me.id);
        System.out.println("Username:  " + me.username);
        System.out.println("Display:   " + me.getDisplayNameOrUsername());
        System.out.println("Acct:      @" + me.acct);
        System.out.println("Followers: " + me.followersCount);
        System.out.println("Following: " + me.followingCount);
        System.out.println("Statuses:  " + me.statusesCount);
        System.out.println("Bot:       " + me.bot);
        System.out.println("URL:       " + me.url);
        System.out.println();
    }

    @Test
    public void testGetHomeTimeline() throws IOException {
        ArrayList<Status> timeline = api.getTimeline(TimelineType.HOME);

        assertNotNull("getTimeline(HOME) returned null", timeline);
        assertFalse("Home timeline is empty — does this account follow anyone?", timeline.isEmpty());

        System.out.println("=== Home Timeline (first 5) ===");
        int count = Math.min(5, timeline.size());
        for (int i = 0; i < count; i++) {
            Status s = timeline.get(i);
            Status actual = s.getActionableStatus();
            System.out.println("[" + (i + 1) + "] @" + actual.account.acct);
            System.out.println("    " + stripHtml(actual.content));
            if (s.isReblog()) {
                System.out.println("    (boosted by @" + s.account.acct + ")");
            }
            if (actual.poll != null) {
                System.out.println("    [POLL] " + actual.poll.options.size() + " options, "
                    + (actual.poll.expired ? "closed" : "open"));
            }
            System.out.println("    Favs: " + actual.favouritesCount
                + " | Boosts: " + actual.reblogsCount
                + " | Replies: " + actual.repliesCount);
            System.out.println();
        }
    }

    @Test
    public void testGetLocalTimeline() throws IOException {
        ArrayList<Status> timeline = api.getTimeline(TimelineType.LOCAL);

        assertNotNull("getTimeline(LOCAL) returned null", timeline);
        assertFalse("Local timeline is empty", timeline.isEmpty());

        System.out.println("=== Local Timeline (first 3) ===");
        int count = Math.min(3, timeline.size());
        for (int i = 0; i < count; i++) {
            Status s = timeline.get(i);
            System.out.println("[" + (i + 1) + "] @" + s.account.acct + ": " + stripHtml(s.content));
        }
        System.out.println();
    }

    @Test
    public void testGetNotifications() throws IOException {
        ArrayList<Notification> notifs = api.getNotifications();

        assertNotNull("getNotifications() returned null", notifs);

        System.out.println("=== Notifications (first 5) ===");
        if (notifs.isEmpty()) {
            System.out.println("(none)");
        }
        int count = Math.min(5, notifs.size());
        for (int i = 0; i < count; i++) {
            Notification n = notifs.get(i);
            System.out.println("[" + (i + 1) + "] " + n.type + " from @" + n.account.acct);
            if (n.status != null) {
                System.out.println("    " + stripHtml(n.status.content));
            }
        }
        System.out.println();
    }

    @Test
    public void testGetStatusAndContext() throws IOException {
        // Grab the first status from the home timeline to test with
        ArrayList<Status> timeline = api.getTimeline(TimelineType.HOME);
        assertNotNull(timeline);
        assertFalse("Need at least one status to test context", timeline.isEmpty());

        Status first = timeline.get(0).getActionableStatus();
        String statusId = first.id;

        // Fetch the single status
        Status fetched = api.getStatus(statusId);
        assertNotNull("getStatus() returned null", fetched);
        assertEquals("Status ID mismatch", statusId, fetched.id);

        System.out.println("=== Single Status ===");
        System.out.println("@" + fetched.account.acct + ": " + stripHtml(fetched.content));

        // Fetch its context
        StatusContext ctx = api.getStatusContext(statusId);
        assertNotNull("getStatusContext() returned null", ctx);
        assertNotNull("ancestors is null", ctx.ancestors);
        assertNotNull("descendants is null", ctx.descendants);

        System.out.println("Ancestors:   " + ctx.ancestors.size());
        System.out.println("Descendants: " + ctx.descendants.size());
        System.out.println();
    }

    @Test
    public void testModelParsing() throws IOException {
        // Verify that Gson correctly deserializes nested objects
        Account me = api.getMe();
        assertNotNull(me);

        // Fields list should be non-null (may be empty)
        assertNotNull("fields should not be null", me.fields);

        // Emojis list should be non-null (may be empty)
        assertNotNull("emojis should not be null", me.emojis);

        System.out.println("=== Model Parsing ===");
        System.out.println("Fields:  " + me.fields.size());
        for (Account.Field f : me.fields) {
            System.out.println("  " + f.name + " = " + stripHtml(f.value));
        }
        System.out.println("Emojis:  " + me.emojis.size());
        System.out.println();
    }

    /** Crude HTML strip — good enough for test output. */
    private static String stripHtml(String html) {
        if (html == null) return "(null)";
        return html.replaceAll("<[^>]+>", "").replaceAll("&amp;", "&").replaceAll("&lt;", "<")
                   .replaceAll("&gt;", ">").replaceAll("&#39;", "'").replaceAll("&quot;", "\"")
                   .trim();
    }
}
