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


    AudioManager am;

    private enum LoadDirection { OLDER, NEWER }
    private boolean isLoadingOlder = false;
    private boolean isLoadingNewer = false;
    private static final int MAX_STATUSES = 60;
    private static final int LOAD_THRESHOLD = 5;

    private class FetchTimelineTask extends AsyncTask<Void, Void, ArrayList<Status>> {
        private IOException error;
        private final LoadDirection dir;

        public FetchTimelineTask(LoadDirection dir) {
            this.dir = dir;
        }

        @Override
        protected ArrayList<ca.bomberfish.glasstodon.model.Status> doInBackground(Void... voids) {
            Log.d("FetchTimelineTask", "Fetching statuses");
            try {
                if (statuses.isEmpty()) {
                    return api.getTimeline(TimelineType.HOME, 30);
                } else if (dir == LoadDirection.OLDER) {
                    return api.getTimeline(TimelineType.HOME, 30, statuses.get(statuses.size() - 1).id);
                } else {
                    return api.getTimeline(TimelineType.HOME, 30, null, statuses.get(0).id);
                }
            } catch (IOException e) {
                Log.e("FetchTimelineTask", "Failed to fetch account info: " + e.getMessage());
                error = e;
                return null;
            }
        }

        @Override
        protected void onPostExecute(ArrayList<ca.bomberfish.glasstodon.model.Status> result) {
            if (result == null || result.isEmpty()) {
                isLoadingOlder = false;
                isLoadingNewer = false;
                if (result == null) {
                    mView = new CardBuilder(TimelineActivity.this, CardBuilder.Layout.ALERT)
                            .setText("Failed to fetch timeline.")
                            .setIcon(android.R.drawable.stat_notify_error)
                            .getView();
                    mCardScroller.getAdapter().notifyDataSetChanged();
                    am.playSoundEffect(Sounds.ERROR);
                }
                return;
            }

            if (dir == null) {
                statuses.addAll(result);
                mCardScroller.getAdapter().notifyDataSetChanged();
                return;
            }

            int currentPos = mCardScroller.getSelectedItemPosition();
            switch (dir) {
                case OLDER:
                    statuses.addAll(result);
                    if (statuses.size() > MAX_STATUSES) {
                        int excess = statuses.size() - MAX_STATUSES;
                        statuses.subList(0, excess).clear();
                        mCardScroller.getAdapter().notifyDataSetChanged();
                        mCardScroller.setSelection(currentPos - excess);
                    } else {
                        mCardScroller.getAdapter().notifyDataSetChanged();
                    }
                    isLoadingOlder = false;
                    break;
                case NEWER:
                    statuses.addAll(0, result);
                    if (statuses.size() > MAX_STATUSES) {
                        statuses.subList(MAX_STATUSES, statuses.size()).clear();
                    }
                    mCardScroller.getAdapter().notifyDataSetChanged();
                    mCardScroller.setSelection(currentPos + result.size());
                    isLoadingNewer = false;
                    break;
            }
        }
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
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
                am.playSoundEffect(Sounds.DISALLOWED);
            }
        });
        mCardScroller.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                                    @Override
                                                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                                        Log.d("TimelineActivity", "Selected item " + position);
                                                        if (!statuses.isEmpty()) {
                                                            if (!isLoadingOlder && position >= statuses.size() - LOAD_THRESHOLD) {
                                                                isLoadingOlder = true;
                                                                new FetchTimelineTask(LoadDirection.OLDER).execute();
                                                            } else if (!isLoadingNewer && position > 0 && position <= LOAD_THRESHOLD) {
                                                                isLoadingNewer = true;
                                                                new FetchTimelineTask(LoadDirection.NEWER).execute();
                                                            }
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
        appendIconCount(sb, android.R.drawable.btn_star_big_on, status.favouritesCount);
        sb.append("    ");
        appendIconCount(sb, android.R.drawable.ic_menu_share, status.reblogsCount);
        sb.append("    ");
        appendIconCount(sb, android.R.drawable.stat_notify_chat, status.repliesCount);
        return sb;
    }
    private void appendIconCount(SpannableStringBuilder sb, int drawableRes, int count) {
        Drawable icon = getResources().getDrawable(drawableRes);
        int size = 48;
        icon.setBounds(0, 0, size, size);
        int start = sb.length();
        sb.append(" "); // placeholder character to replace with the icon
        sb.setSpan(new ImageSpan(icon, ImageSpan.ALIGN_CENTER),
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
        boolean hasMedia = !actionable.hasSpoiler() && actionable.mediaAttachments != null && !actionable.mediaAttachments.isEmpty();
        if (hasMedia) {
            card = new CardBuilder(this, CardBuilder.Layout.CAPTION)
                    .addImage(R.drawable.placeholder);
        } else {
            card = new CardBuilder(this, CardBuilder.Layout.AUTHOR);
        }

        card.setText(sb);
        card.setIcon(R.drawable.ic_glass_logo);
        card.setFootnote(buildFootnote(actionable));
        card.setTimestamp(p.format(Instant.parse(actionable.createdAt)) + "  ");
        card.setHeading(actionable.account.getDisplayNameOrUsername());
        card.setSubheading(actionable.account.acct);
        if (hasMedia) {
            if (actionable.content.isEmpty()) {
                card.setText(actionable.account.getDisplayNameOrUsername() + " posted:");
            } else {
                card.setTimestamp(actionable.account.getDisplayNameOrUsername() + " · " + p.format(Instant.parse(actionable.createdAt)) + "  ");
            }
        }
        card.setAttributionIcon(getIconForPrivacy(actionable.visibility));

        return card.getView();
    }

    private int getIconForPrivacy(String privacy) {
        switch (privacy) {
            case "unlisted":
                return android.R.drawable.ic_lock_silent_mode;
            case "private":
                return android.R.drawable.ic_lock_lock;
            case "direct":
                return android.R.drawable.ic_dialog_email;
            default: // includes "public"
                return android.R.drawable.stat_notify_chat;
        }
    }
}