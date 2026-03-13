package ca.bomberfish.glasstodon;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import ca.bomberfish.glasstodon.model.*;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MastoAPI {
    private final String instanceUrl;
    private final String accessToken;
    private final OkHttpClient httpClient;
    private final Gson gson;

    public MastoAPI(String instanceUrl, String accessToken) {
        this.instanceUrl = instanceUrl;
        this.accessToken = accessToken;
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();
    }

    // ---- Generic request helpers ----

    private <T> T get(String endpoint, Type type) throws IOException {
        Request request = new Request.Builder()
                .url(instanceUrl + endpoint)
                .header("Authorization", "Bearer " + accessToken)
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

    public ArrayList<Status> getTimeline(TimelineType type) throws IOException {
        String endpoint = type.getEndpoint();
        return get(endpoint, new TypeToken<ArrayList<Status>>(){}.getType());
    }

    // ---- Accounts ----

    public Account getMe() throws IOException {
        return get("/api/v1/accounts/verify_credentials", Account.class);
    }

    public Account getAccount(String id) throws IOException {
        return get("/api/v1/accounts/" + id, Account.class);
    }

    // ---- Single status + context ----

    /** Fetch a single status by ID. */
    public Status getStatus(String id) throws IOException {
        return get("/api/v1/statuses/" + id, Status.class);
    }

    /** Fetch the thread context (ancestors + descendants) for a status. */
    public StatusContext getStatusContext(String id) throws IOException {
        return get("/api/v1/statuses/" + id + "/context", StatusContext.class);
    }

    // ---- Status actions ----

    public Status favourite(String statusId) throws IOException {
        return post("/api/v1/statuses/" + statusId + "/favourite", Status.class);
    }

    public Status unfavourite(String statusId) throws IOException {
        return post("/api/v1/statuses/" + statusId + "/unfavourite", Status.class);
    }

    public Status boost(String statusId) throws IOException {
        return post("/api/v1/statuses/" + statusId + "/reblog", Status.class);
    }

    public Status unboost(String statusId) throws IOException {
        return post("/api/v1/statuses/" + statusId + "/unreblog", Status.class);
    }

    public Status bookmark(String statusId) throws IOException {
        return post("/api/v1/statuses/" + statusId + "/bookmark", Status.class);
    }

    public Status unbookmark(String statusId) throws IOException {
        return post("/api/v1/statuses/" + statusId + "/unbookmark", Status.class);
    }

    public Status pin(String statusId) throws IOException {
        return post("/api/v1/statuses/" + statusId + "/pin", Status.class);
    }

    public Status unpin(String statusId) throws IOException {
        return post("/api/v1/statuses/" + statusId + "/unpin", Status.class);
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
        return post("/api/v1/statuses", form.build(), Status.class);
    }

    /** Delete one of the user's own statuses. */
    public void deleteStatus(String statusId) throws IOException {
        delete("/api/v1/statuses/" + statusId);
    }

    // ---- Polls ----

    /** Vote on a poll. choices is a list of option indices (0-based). */
    public Poll votePoll(String pollId, List<Integer> choices) throws IOException {
        FormBody.Builder form = new FormBody.Builder();
        for (Integer choice : choices) {
            form.add("choices[]", String.valueOf(choice));
        }
        return post("/api/v1/polls/" + pollId + "/votes", form.build(), Poll.class);
    }

    // ---- Notifications ----

    public ArrayList<Notification> getNotifications() throws IOException {
        return get("/api/v1/notifications", new TypeToken<ArrayList<Notification>>(){}.getType());
    }

    /** Dismiss a single notification. */
    public void dismissNotification(String notificationId) throws IOException {
        post("/api/v1/notifications/" + notificationId + "/dismiss");
    }
}
