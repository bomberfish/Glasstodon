package ca.bomberfish.glasstodon.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Represents a poll attached to a status.
 * https://docs.joinmastodon.org/entities/Poll/
 */
public class Poll {

    @SerializedName("id")
    public String id;

    @SerializedName("expires_at")
    public String expiresAt;

    @SerializedName("expired")
    public boolean expired;

    /** Whether the poll allows multiple choices */
    @SerializedName("multiple")
    public boolean multiple;

    /** Total number of votes cast across all options */
    @SerializedName("votes_count")
    public int votesCount;

    /** Total number of unique voters (null if multiple = false) */
    @SerializedName("voters_count")
    public Integer votersCount;

    @SerializedName("options")
    public List<Option> options;

    @SerializedName("emojis")
    public List<CustomEmoji> emojis;

    // -- Auth-dependent fields --

    /** Whether the current user has voted */
    @SerializedName("voted")
    public Boolean voted;

    /** Indices of options the current user chose */
    @SerializedName("own_votes")
    public List<Integer> ownVotes;

    /**
     * Whether the poll is still accepting votes.
     */
    public boolean isOpen() {
        return !expired && expiresAt != null;
    }

    /**
     * A single option in a poll.
     * https://docs.joinmastodon.org/entities/Poll/#Option
     */
    public static class Option {

        @SerializedName("title")
        public String title;

        /** Number of votes for this option (null if results not yet published) */
        @SerializedName("votes_count")
        public Integer votesCount;
    }
}
