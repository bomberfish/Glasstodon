package ca.bomberfish.glasstodon;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ca.bomberfish.glasstodon.model.*;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import ca.bomberfish.glasstodon.BuildConfig;

import okhttp3.ConnectionSpec;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MastoAPI {
    private final String instanceUrl;
    private final String accessToken;
    public final OkHttpClient httpClient;
    private final Gson gson;
    private final boolean debug;

    public MastoAPI(String instanceUrl, String accessToken, boolean debug, Context context) {
        OkHttpClient client = new OkHttpClient();

        try {
            KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
            store.load(null, null);
            int[] certResources = new int[]{
                    R.raw.isrgrootx1,               // ISRG Root X1
                    R.raw.isrgrootx2,               // ISRG Root X2
                    R.raw.isrgrootx2_crosssigned,   // ISRG Root X2 (cross-signed by ISRG Root X1)
                    R.raw.isrgrootx2_x1,            // ISRG Root X2 (second cross-sign by ISRG Root X1)
                    R.raw.isrgrootye,               // ISRG Root YE
                    R.raw.isrgrootye_x2,            // ISRG Root YE (cross-signed by ISRG Root X2)
                    R.raw.isrgrootyr,               // ISRG Root YR
                    R.raw.isrgrootyr_x1             // ISRG Root YR (cross-signed by ISRG Root X1)
            };
            CertificateFactory x509Factory = CertificateFactory.getInstance("X.509");
            for (int certResource : certResources) {
                try {
                    String alias = context.getResources().getResourceEntryName(certResource);
                    Log.d("MastoAPI", "Loading \"" + alias + "\" from bundled certificates");
                    InputStream certStream = context.getResources().openRawResource(certResource);
                    Certificate cert = x509Factory.generateCertificate(certStream);
                    store.setCertificateEntry(alias, cert);
                } catch (Exception e) {
                    Log.w("MastoAPI", "Failed to load certificate resource: " + e.getMessage(), e);
                }
            }

            TrustManagerFactory newTmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            newTmf.init(store);
            X509TrustManager newManager = CombinedX509TrustManager.managerFromTrustManagerFactory(newTmf);

            TrustManagerFactory systemTmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            systemTmf.init((KeyStore) null);
            X509TrustManager systemManager = CombinedX509TrustManager.managerFromTrustManagerFactory(systemTmf);

            X509TrustManager customManager = new CombinedX509TrustManager(newManager, systemManager);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] { customManager }, null);
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            client = new OkHttpClient.Builder()
                    .sslSocketFactory(sslSocketFactory, customManager)
                    .build();
        } catch (Exception e) {
            Log.w("MastoAPI", "Failed to load certificate store: " + e.getMessage(), e);
        }

        this.instanceUrl = instanceUrl;
        this.accessToken = accessToken;
        this.httpClient = client;
        this.gson = new Gson();
        this.debug = debug;
    }

    // ---- Generic request helpers ----

    private <T> T get(String endpoint, Type type) throws IOException {
        Log.d("MastoAPI", "Fetching " + endpoint);
        Request request = new Request.Builder()
                .url(instanceUrl + "/api" + endpoint)
                .header("Authorization", "Bearer " + accessToken)
                .build();
        Response response = httpClient.newCall(request).execute();
        try {
            ResponseBody body = response.body();
            if (!response.isSuccessful()) {
                throw new IOException("API error: " + response.code() + " " + response.message() + "\n" + (body != null ? body.string() : "No response body"));
            }
            if (body == null) {
                throw new IOException("Empty response body");
            }
            if (debug) {
                // Log the raw JSON response for easier debugging of parsing issues
                String rawJson = body.string();
                Log.d("MastoAPI", "Raw JSON response for " + endpoint + ": " + rawJson);
                return gson.fromJson(rawJson, type);
            }

            return gson.fromJson(body.charStream(), type);
        } finally {
            response.close();
        }
    }

    /** Convenience overload for when you have a simple Class (not a generic list). */
    private <T> T get(String endpoint, Class<T> clazz) throws IOException {
        return get(endpoint, (Type) clazz);
    }

    /**
     * POST with a form body and parse a typed JSON response.
     */
    private <T> T post(String endpoint, RequestBody requestBody, Type type) throws IOException {
        Request request = new Request.Builder()
                .url(instanceUrl + endpoint)
                .header("Authorization", "Bearer " + accessToken)
                .post(requestBody)
                .build();
        Response response = httpClient.newCall(request).execute();
        try {
            if (!response.isSuccessful()) {
                throw new IOException("API error: " + response.code() + " " + response.message());
            }
            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("Empty response body");
            }
            return gson.fromJson(body.charStream(), type);
        } finally {
            response.close();
        }
    }

    /** POST with a form body and parse response into a simple Class. */
    private <T> T post(String endpoint, RequestBody requestBody, Class<T> clazz) throws IOException {
        return post(endpoint, requestBody, (Type) clazz);
    }

    /** POST with an empty body (for action endpoints like favourite, reblog, etc.) */
    private <T> T post(String endpoint, Class<T> clazz) throws IOException {
        RequestBody emptyBody = new FormBody.Builder().build();
        return post(endpoint, emptyBody, clazz);
    }

    /** POST with an empty body, ignoring the response body (fire-and-forget actions). */
    private void post(String endpoint) throws IOException {
        RequestBody emptyBody = new FormBody.Builder().build();
        Request request = new Request.Builder()
                .url(instanceUrl + endpoint)
                .header("Authorization", "Bearer " + accessToken)
                .post(emptyBody)
                .build();
        Response response = httpClient.newCall(request).execute();
        try {
            if (!response.isSuccessful()) {
                throw new IOException("API error: " + response.code() + " " + response.message());
            }
        } finally {
            response.close();
        }
    }

    /** DELETE request, ignoring the response body. */
    private void delete(String endpoint) throws IOException {
        Request request = new Request.Builder()
                .url(instanceUrl + endpoint)
                .header("Authorization", "Bearer " + accessToken)
                .delete()
                .build();
        Response response = httpClient.newCall(request).execute();
        try {
            if (!response.isSuccessful()) {
                throw new IOException("API error: " + response.code() + " " + response.message());
            }
        } finally {
            response.close();
        }
    }

    // ---- Timeline ----

    public ArrayList<Status> getTimeline(TimelineType type, int limit, String sinceId) throws IOException {
        String endpoint = type.getEndpoint() + "?limit=" + limit + (sinceId != null ? "&since_id=" + sinceId : "");
        return get(endpoint, new TypeToken<ArrayList<Status>>(){}.getType());
    }

    public ArrayList<Status> getTimeline(TimelineType type, int limit) throws IOException {
        return getTimeline(type, limit, null);
    }

    public ArrayList<Status> getTimeline(TimelineType type, String sinceId) throws IOException {
        return getTimeline(type, 20, sinceId);
    }

    public ArrayList<Status> getTimeline(TimelineType type) throws IOException {
        return getTimeline(type, 20, null);
    }


    // ---- Accounts ----

    public Account getMe() throws IOException {
        return get("/v1/accounts/verify_credentials", Account.class);
    }

    public Account getAccount(String id) throws IOException {
        return get("/v1/accounts/" + id, Account.class);
    }

    public ArrayList<Status> getAccountStatuses(String accountId, AccountTimelineType timelineType) throws IOException {
        String endpoint = timelineType.getEndpoint(accountId);
        return get(endpoint, new TypeToken<ArrayList<Status>>(){}.getType());
    }

    public ArrayList<Account> getFollowers(String accountId) throws IOException {
        return get("/v1/accounts/" + accountId + "/followers", new TypeToken<ArrayList<Account>>(){}.getType());
    }

    public ArrayList<Account> getFollowing(String accountId) throws IOException {
        return get("/v1/accounts/" + accountId + "/following", new TypeToken<ArrayList<Account>>(){}.getType());
    }

    public void follow(String accountId) throws IOException {
        post("/v1/accounts/" + accountId + "/follow");
    }

    public void unfollow(String accountId) throws IOException {
        post("/v1/accounts/" + accountId + "/unfollow");
    }

    public void block(String accountId) throws IOException {
        post("/v1/accounts/" + accountId + "/block");
    }

    public void unblock(String accountId) throws IOException {
        post("/v1/accounts/" + accountId + "/unblock");
    }

    // ---- Single status + context ----

    /** Fetch a single status by ID. */
    public Status getStatus(String id) throws IOException {
        return get("/v1/statuses/" + id, Status.class);
    }

    /** Fetch the thread context (ancestors + descendants) for a status. */
    public StatusContext getStatusContext(String id) throws IOException {
        return get("/v1/statuses/" + id + "/context", StatusContext.class);
    }

    // ---- Status actions ----

    public Status favourite(String statusId) throws IOException {
        return post("/v1/statuses/" + statusId + "/favourite", Status.class);
    }

    public Status unfavourite(String statusId) throws IOException {
        return post("/v1/statuses/" + statusId + "/unfavourite", Status.class);
    }

    public Status boost(String statusId) throws IOException {
        return post("/v1/statuses/" + statusId + "/reblog", Status.class);
    }

    public Status unboost(String statusId) throws IOException {
        return post("/v1/statuses/" + statusId + "/unreblog", Status.class);
    }

    public Status bookmark(String statusId) throws IOException {
        return post("/v1/statuses/" + statusId + "/bookmark", Status.class);
    }

    public Status unbookmark(String statusId) throws IOException {
        return post("/v1/statuses/" + statusId + "/unbookmark", Status.class);
    }

    public Status pin(String statusId) throws IOException {
        return post("/v1/statuses/" + statusId + "/pin", Status.class);
    }

    public Status unpin(String statusId) throws IOException {
        return post("/v1/statuses/" + statusId + "/unpin", Status.class);
    }

    // ---- Compose / delete statuses ----

    /**
     * Post a new status.
     *
     * @param text        The status text content
     * @param inReplyToId ID of the status being replied to, or null
     * @param visibility  "public", "unlisted", "private", or "direct" — or null for default
     * @param sensitive   Whether to mark media as sensitive
     * @param spoilerText Content warning text, or null for none
     * @return The newly created Status
     */
    public Status postStatus(String text, String inReplyToId, String visibility,
                             boolean sensitive, String spoilerText) throws IOException {
        FormBody.Builder form = new FormBody.Builder()
                .add("status", text);
        if (inReplyToId != null) {
            form.add("in_reply_to_id", inReplyToId);
        }
        if (visibility != null) {
            form.add("visibility", visibility);
        }
        if (sensitive) {
            form.add("sensitive", "true");
        }
        if (spoilerText != null) {
            form.add("spoiler_text", spoilerText);
        }
        return post("/v1/statuses", form.build(), Status.class);
    }

    /** Delete one of the user's own statuses. */
    public void deleteStatus(String statusId) throws IOException {
        delete("/v1/statuses/" + statusId);
    }

    // ---- Polls ----

    /** Vote on a poll. choices is a list of option indices (0-based). */
    public Poll votePoll(String pollId, List<Integer> choices) throws IOException {
        FormBody.Builder form = new FormBody.Builder();
        for (Integer choice : choices) {
            form.add("choices[]", String.valueOf(choice));
        }
        return post("/v1/polls/" + pollId + "/votes", form.build(), Poll.class);
    }

    // ---- Notifications ----

    public ArrayList<Notification> getNotifications() throws IOException {
        return get("/v1/notifications", new TypeToken<ArrayList<Notification>>(){}.getType());
    }

    /** Dismiss a single notification. */
    public void dismissNotification(String notificationId) throws IOException {
        post("/v1/notifications/" + notificationId + "/dismiss");
    }
}
