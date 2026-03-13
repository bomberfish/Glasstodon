package ca.bomberfish.glasstodon.model;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a notification of an event relevant to the user.
 * https://docs.joinmastodon.org/entities/Notification/
 *
 * Included for future use -- notifications are a core feature
 * of any Mastodon client.
 */
public class Notification {

    @SerializedName("id")
    public String id;

    /**
     * mention, status, reblog, follow, follow_request,
     * favourite, poll, update, admin.sign_up, admin.report
     */
    @SerializedName("type")
    public String type;

    @SerializedName("created_at")
    public String createdAt;

    /** The account that performed the action */
    @SerializedName("account")
    public Account account;

    /** Status that was the object of the notification (for mention, status, reblog, favourite, poll, update) */
    @SerializedName("status")
    public Status status;

    public boolean isMention() {
        return "mention".equals(type);
    }

    public boolean isBoost() {
        return "reblog".equals(type);
    }

    public boolean isFavourite() {
        return "favourite".equals(type);
    }

    public boolean isFollow() {
        return "follow".equals(type);
    }
}
