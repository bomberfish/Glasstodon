package ca.bomberfish.glasstodon;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class CombinedX509TrustManager implements X509TrustManager {
    private final X509TrustManager[] managers;

    public CombinedX509TrustManager(X509TrustManager... managers) {
        this.managers = managers;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        CertificateException lastException = null;
        for (X509TrustManager manager : managers) {
            try {
                manager.checkClientTrusted(x509Certificates, s);
                return;
            } catch (CertificateException e) {
                lastException = e;
            }
        }
        throw lastException;
    }

    @Override
    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        CertificateException lastException = null;
        for (X509TrustManager manager : managers) {
            try {
                manager.checkServerTrusted(x509Certificates, s);
                return;
            } catch (CertificateException e) {
                lastException = e;
            }
        }
        throw lastException;
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        List<X509Certificate> issuers = new ArrayList<>();
        for (X509TrustManager manager : managers) {
            X509Certificate[] managerIssuers = manager.getAcceptedIssuers();
            if (managerIssuers != null) {
                issuers.addAll(Arrays.asList(managerIssuers));
            }
        }
        return issuers.toArray(new X509Certificate[issuers.size()]);
    }

    public static X509TrustManager managerFromTrustManagerFactory(TrustManagerFactory tmf) throws IllegalStateException {
        X509TrustManager mgr = null;
        for (TrustManager tm : tmf.getTrustManagers()) {
            if (tm instanceof X509TrustManager) {
                mgr = (X509TrustManager) tm;
                break;
            }
        }

        if (mgr == null) {
            throw new IllegalStateException("No X509TrustManager found");
        }
        return mgr;
    }
}
