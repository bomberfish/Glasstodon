package ca.bomberfish.glasstodon;

import android.content.Context;
import android.content.SharedPreferences;

public class AppStorage {
    private static final String PREFS_NAME = "GlasstodonPrefs";
    private static final String KEY_INSTANCE_URL = "InstanceURL";
    private static final String KEY_ACCESS_TOKEN = "AccessToken";

    private final SharedPreferences prefs;
    public AppStorage(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveCredentials(String instanceUrl, String accessToken) {
        prefs.edit()
                .putString(KEY_INSTANCE_URL, instanceUrl)
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .apply();
    }

    public String getInstanceUrl() {
        return prefs.getString(KEY_INSTANCE_URL, null);
    }

    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    public boolean isLoggedIn() {
        return getAccessToken() != null;
    }

    public void clearCredentials() {
        prefs.edit()
                .remove(KEY_INSTANCE_URL)
                .remove(KEY_ACCESS_TOKEN)
                .apply();
    }
}
