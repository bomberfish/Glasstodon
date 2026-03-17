package ca.bomberfish.glasstodon;
import android.app.Application;
import android.util.Log;

import androidx.multidex.MultiDexApplication;

import org.conscrypt.Conscrypt;

import java.security.KeyStore;
import java.security.Security;
public class GlasstodonApp extends MultiDexApplication {
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