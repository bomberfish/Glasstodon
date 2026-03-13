package ca.bomberfish.glasstodon.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Represents a status (post/toot) posted by an account.
 * https://docs.joinmastodon.org/entities/Status/
 */
public class Status {

    @SerializedName("id")
    public String id;

    /** URI used for federation */
    @SerializedName("uri")
    public String uri;

    @SerializedName("created_at")
    public String createdAt;

    @SerializedName("account")
    public Account account;

    /** HTML-encoded status content */
    @SerializedName("content")
    public String content;

    /** public, unlisted, private, direct */
    @SerializedName("visibility")
    public String visibility;

    @SerializedName("sensitive")
    public boolean sensitive;

    /** Content warning / subject line */
    @SerializedName("spoiler_text")
    public String spoilerText;

    @SerializedName("media_attachments")
    public List<MediaAttachment> mediaAttachments;

    @SerializedName("application")
    public Application application;

    @SerializedName("mentions")
    public List<Mention> mentions;

    @SerializedName("tags")
    public List<Tag> tags;

    @SerializedName("emojis")
    public List<CustomEmoji> emojis;

    @SerializedName("reblogs_count")
    public int reblogsCount;

    @SerializedName("favourites_count")
    public int favouritesCount;

    @SerializedName("replies_count")
    public int repliesCount;

    /** Link to the HTML representation */
    @SerializedName("url")
    public String url;

    @SerializedName("in_reply_to_id")
    public String inReplyToId;

    @SerializedName("in_reply_to_account_id")
    public String inReplyToAccountId;

    /** The status being reblogged, if this is a boost */
    @SerializedName("reblog")
    public Status reblog;

    @SerializedName("language")
    public String language;

    @SerializedName("poll")
    public Poll poll;

    @SerializedName("edited_at")
    public String editedAt;

    // -- Auth-dependent optional fields --

    @SerializedName("favourited")
    public Boolean favourited;

    @SerializedName("reblogged")
    public Boolean reblogged;

    @SerializedName("muted")
    public Boolean muted;

    @SerializedName("bookmarked")
    public Boolean bookmarked;

    @SerializedName("pinned")
    public Boolean pinned;

    /**
     * Returns the actual content status. If this is a boost,
     * returns the reblogged status; otherwise returns this.
     */
    public Status getActionableStatus() {
        return reblog != null ? reblog : this;
    }

    /**
     * Whether this status has a content warning.
     */
    public boolean hasSpoiler() {
        return spoilerText != null && !spoilerText.isEmpty();
    }

    /**
     * Whether this status is a boost of another status.
     */
    public boolean isReblog() {
        return reblog != null;
    }

    /**
     * Mention of a user within status content.
     * https://docs.joinmastodon.org/entities/Status/#Mention
     */
    public static class Mention {

        @SerializedName("id")
        public String id;

        @SerializedName("username")
        public String username;

        @SerializedName("acct")
        public String acct;

        @SerializedName("url")
        public String url;
    }

    /**
     * Hashtag used within status content.
     * https://docs.joinmastodon.org/entities/Status/#Tag
     */
    public static class Tag {

        @SerializedName("name")
        public String name;

        @SerializedName("url")
        public String url;
    }
}
