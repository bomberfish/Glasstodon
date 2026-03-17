package ca.bomberfish.glasstodon;
import android.app.Application;
import android.util.Log;

import org.conscrypt.Conscrypt;

import java.security.KeyStore;
import java.security.Security;
public class GlasstodonApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // enable modern tls with conscrypt
        setupConscrypt();
    }

    private void setupConscrypt() {
        Security.insertProviderAt(Conscrypt.newProvider(), 1);
    }
}