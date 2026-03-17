package ca.bomberfish.glasstodon;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Entry point for the app. Checks login state and dispatches
 * to the appropriate activity.
 */
public class MainActivity extends Activity {

    private static final int REQUEST_CODE_SCAN = 1;

    private AppStorage storage;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        storage = new AppStorage(this);

        if (!storage.isLoggedIn()) {
            Intent intent = new Intent(this, ScanActivity.class);
            startActivityForResult(intent, REQUEST_CODE_SCAN);
        } else {
            launchTimeline();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SCAN) {
            if (resultCode == RESULT_OK) {
                String instanceUrl = data.getStringExtra("InstanceURL");
                String accessToken = data.getStringExtra("AccessToken");
                storage.saveCredentials(instanceUrl, accessToken);
                launchTimeline();
            } else {
                finish();
            }
        }
    }

    private void launchTimeline() {
        Intent intent = new Intent(this, TimelineActivity.class);
        startActivity(intent);
        finish();
    }
}
