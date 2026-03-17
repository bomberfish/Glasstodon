package ca.bomberfish.glasstodon;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;
import ca.bomberfish.glasstodon.model.Account;
import ca.bomberfish.glasstodon.model.Status;
import ca.bomberfish.glasstodon.model.TimelineType;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.AsyncTask;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.Spannable;
import android.text.style.ImageSpan;

import org.ocpsoft.prettytime.PrettyTime;

/**
 * Displays the timeline as a scrollable list of cards.
 * Currently shows the user's account info as a placeholder —
 * will be replaced with actual timeline content.
 */
public class TimelineActivity extends Activity {

    private CardScrollView mCardScroller;
    private View mView;

    AppStorage storage;
    ArrayList<Status> statuses = new ArrayList<>();

    private MastoAPI api;

    PrettyTime p = new PrettyTime();

    private boolean isLoading = true;
    private class FetchTimelineTask extends AsyncTask<Void, Void, ArrayList<Status>> {
        private IOException error;
        private final String maxId;

        public FetchTimelineTask(String maxId) {
            this.maxId = maxId;
        }

        @Override
        protected ArrayList<ca.bomberfish.glasstodon.model.Status> doInBackground(Void... voids) {
            isLoading = true;
            try {
                return api.getTimeline(TimelineType.HOME, 30, this.maxId);
            } catch (IOException e) {
                Log.e("TimelineActivity", "Failed to fetch account info: " + e.getMessage());
                error = e;
                return null;
            }
        }

        @Override
        protected void onPostExecute(ArrayList<ca.bomberfish.glasstodon.model.Status> result) {
            if (result != null) {
                statuses.addAll(result);
            } else {
                Log.e("TimelineActivity", "Failed to fetch account info: " + error.getMessage());
                mView = new CardBuilder(TimelineActivity.this, CardBuilder.Layout.TEXT)
                        .setText("Failed to load account info.\nPlease check your network connection and try again.")
                        .getView();
            }
            isLoading = false;
            mCardScroller.getAdapter().notifyDataSetChanged();
        }
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        storage = new AppStorage(this);
        api = new MastoAPI(storage.getInstanceUrl(), storage.getAccessToken(), false, this);
        setupView();
    }

    private void setupView() {
        mView = new CardBuilder(this, CardBuilder.Layout.TEXT)
            .setText("Loading...")
            .getView();

        mCardScroller = new CardScrollView(this);
        mCardScroller.setAdapter(new CardScrollAdapter() {
            @Override
            public int getCount() {
                return Math.max(1, statuses.size());
            }

            @Override
            public Object getItem(int position) {
                if (statuses.isEmpty()) return mView;
                return statuses.get(position);
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (statuses.isEmpty()) return mView;
                return buildStatusCard(statuses.get(position));
            }

            @Override
            public int getPosition(Object item) {
                if (statuses.isEmpty()) {
                    return item == mView ? 0 : AdapterView.INVALID_POSITION;
                }
                int i = statuses.indexOf(item);
                return i >= 0 ? i : AdapterView.INVALID_POSITION;
            }
        });
        mCardScroller.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                am.playSoundEffect(Sounds.DISALLOWED);
            }
        });
        mCardScroller.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                                    @Override
                                                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                                        Log.d("TimelineActivity", "Selected item " + position);
                                                        if (!isLoading && !statuses.isEmpty() && position >= statuses.size() - 5) {
                                                            Log.d("TimelineActivity", "Fetching next page of statuses");
                                                            new FetchTimelineTask(statuses.get(statuses.size() - 1).id).execute();
                                                        }
                                                    }

                                                    @Override
                                                    public void onNothingSelected(AdapterView<?> parent) {

                                                    }
                                                });
        setContentView(mCardScroller);
        mCardScroller.activate();

        new FetchTimelineTask(null).execute();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCardScroller != null) {
            mCardScroller.activate();
        }
    }

    @Override
    protected void onPause() {
        if (mCardScroller != null) {
            mCardScroller.deactivate();
        }
        super.onPause();
    }

//    private View buildView() {
//        CardBuilder card = new CardBuilder(this, CardBuilder.Layout.TEXT);
//
//        String displayName = account.getDisplayNameOrUsername();
//        String info = displayName + "\n@" + account.acct
//            + "\n\n" + account.followersCount + " followers · " + account.followingCount + " following";
//        card.setText(info);
//
//        return card.getView();
//    }

    private static CharSequence trimSpanned(Spanned spanned) {
        int start = 0;
        int end = spanned.length();
        while (start < end && Character.isWhitespace(spanned.charAt(start))) {
            start++;
        }
        while (end > start && Character.isWhitespace(spanned.charAt(end - 1))) {
            end--;
        }
        return spanned.subSequence(start, end);
    }

    private CharSequence buildFootnote(Status status) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        // Helper: append an icon + count
        appendIconCount(sb, android.R.drawable.btn_star_big_on, status.favouritesCount);
        sb.append("  ");
        appendIconCount(sb, android.R.drawable.ic_menu_share, status.reblogsCount);
        sb.append("  ");
        appendIconCount(sb, android.R.drawable.stat_notify_chat, status.repliesCount);
        return sb;
    }
    private void appendIconCount(SpannableStringBuilder sb, int drawableRes, int count) {
        Drawable icon = getResources().getDrawable(drawableRes);
        // Scale to match text height — roughly 24px on Glass
        int size = 24;
        icon.setBounds(0, 0, size, size);
        int start = sb.length();
        sb.append(" "); // placeholder character to replace with the icon
        sb.setSpan(new ImageSpan(icon, ImageSpan.ALIGN_BASELINE),
                start, start + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.append(" ");
        sb.append(String.valueOf(count));
    }

    private View buildStatusCard(Status status) {
        Status actionable = status.getActionableStatus();

        SpannableStringBuilder sb = new SpannableStringBuilder();

//        if (status.isReblog()) {
//            sb.append(status.account.getDisplayNameOrUsername());
//            sb.append(" boosted\n");
//        }

//        sb.append(actionable.account.getDisplayNameOrUsername());
//        sb.append("\n");

        if (actionable.hasSpoiler()) {
            sb.append("CW: ");
            sb.append(actionable.spoilerText);
        } else {
            CharSequence content = trimSpanned(Html.fromHtml(actionable.content));
            sb.append(content);
        }


        CardBuilder card;
        if (!actionable.hasSpoiler() && actionable.mediaAttachments != null && !actionable.mediaAttachments.isEmpty()) {
            card = new CardBuilder(this, CardBuilder.Layout.CAPTION)
                    .addImage(R.drawable.placeholder);
        } else {
            card = new CardBuilder(this, CardBuilder.Layout.TEXT);
        }

        card.setText(sb);
        card.setFootnote(buildFootnote(actionable));
        card.setTimestamp(actionable.account.getDisplayNameOrUsername() + " · " + p.format(Instant.parse(actionable.createdAt)));

        return card.getView();
    }
}
