package ca.bomberfish.glasstodon.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Represents a user of Mastodon and their associated profile.
 * https://docs.joinmastodon.org/entities/Account/
 */
public class Account {

    @SerializedName("id")
    public String id;

    @SerializedName("username")
    public String username;

    /** username for local users, username@domain for remote users */
    @SerializedName("acct")
    public String acct;

    @SerializedName("display_name")
    public String displayName;

    /** Profile bio (HTML) */
    @SerializedName("note")
    public String note;

    @SerializedName("url")
    public String url;

    @SerializedName("avatar")
    public String avatar;

    @SerializedName("avatar_static")
    public String avatarStatic;

    @SerializedName("header")
    public String header;

    @SerializedName("header_static")
    public String headerStatic;

    @SerializedName("locked")
    public boolean locked;

    @SerializedName("bot")
    public boolean bot;

    @SerializedName("group")
    public boolean group;

    @SerializedName("discoverable")
    public Boolean discoverable;

    @SerializedName("created_at")
    public String createdAt;

    @SerializedName("last_status_at")
    public String lastStatusAt;

    @SerializedName("statuses_count")
    public int statusesCount;

    @SerializedName("followers_count")
    public int followersCount;

    @SerializedName("following_count")
    public int followingCount;

    @SerializedName("fields")
    public List<Field> fields;

    @SerializedName("emojis")
    public List<CustomEmoji> emojis;

    /**
     * Returns the best display string for this account.
     * Uses display_name if set, otherwise falls back to username.
     */
    public String getDisplayNameOrUsername() {
        if (displayName != null && !displayName.isEmpty()) {
            return displayName;
        }
        return username;
    }

    /**
     * Profile metadata name-value pair.
     * https://docs.joinmastodon.org/entities/Account/#Field
     */
    public static class Field {

        @SerializedName("name")
        public String name;

        /** HTML value */
        @SerializedName("value")
        public String value;

        @SerializedName("verified_at")
        public String verifiedAt;
    }
}
