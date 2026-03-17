package ca.bomberfish.glasstodon;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;
import ca.bomberfish.glasstodon.model.Account;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import java.io.IOException;

/**
 * Displays the timeline as a scrollable list of cards.
 * Currently shows the user's account info as a placeholder —
 * will be replaced with actual timeline content.
 */
public class TimelineActivity extends Activity {

    private CardScrollView mCardScroller;
    private View mView;

    AppStorage storage;
    Account account;

    private class FetchAccountTask extends AsyncTask<Void, Void, Account> {
        private IOException error;

        @Override
        protected Account doInBackground(Void... voids) {
            try {
                MastoAPI api = new MastoAPI(storage.getInstanceUrl(), storage.getAccessToken(), false, TimelineActivity.this);
                return api.getMe();
            } catch (IOException e) {
                Log.e("TimelineActivity", "Failed to fetch account info: " + e.getMessage());
                error = e;
                return null;
            }
        }

        @Override
        protected void onPostExecute(Account result) {
            if (result != null) {
                account = result;
                mView = buildView();
            } else {
                Log.e("TimelineActivity", "Failed to fetch account info: " + error.getMessage());
                mView = new CardBuilder(TimelineActivity.this, CardBuilder.Layout.TEXT)
                        .setText("Failed to load account info.\nPlease check your network connection and try again.")
                        .getView();
            }
            mCardScroller.getAdapter().notifyDataSetChanged();
        }
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        storage = new AppStorage(this);
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
                return 1;
            }

            @Override
            public Object getItem(int position) {
                return mView;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                return mView;
            }

            @Override
            public int getPosition(Object item) {
                if (mView.equals(item)) {
                    return 0;
                }
                return AdapterView.INVALID_POSITION;
            }
        });
        mCardScroller.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                am.playSoundEffect(Sounds.DISALLOWED);
            }
        });
        setContentView(mCardScroller);
        mCardScroller.activate();

        new FetchAccountTask().execute();
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

    private View buildView() {
        CardBuilder card = new CardBuilder(this, CardBuilder.Layout.TEXT);

        String displayName = account.getDisplayNameOrUsername();
        String info = displayName + "\n@" + account.acct
            + "\n\n" + account.followersCount + " followers · " + account.followingCount + " following";
        card.setText(info);

        return card.getView();
    }
}
