package ca.bomberfish.glasstodon.model;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a file or media attachment that can be added to a status.
 * https://docs.joinmastodon.org/entities/MediaAttachment/
 */
public class MediaAttachment {

    @SerializedName("id")
    public String id;

    /** image, gifv, video, audio, unknown */
    @SerializedName("type")
    public String type;

    /** URL of the original full-size attachment */
    @SerializedName("url")
    public String url;

    /** URL of a scaled-down preview */
    @SerializedName("preview_url")
    public String previewUrl;

    /** URL of the remote original (for remote media) */
    @SerializedName("remote_url")
    public String remoteUrl;

    /** Alt text description */
    @SerializedName("description")
    public String description;

    /** Blurhash for generating placeholder previews */
    @SerializedName("blurhash")
    public String blurhash;

    public boolean isImage() {
        return "image".equals(type);
    }

    public boolean isVideo() {
        return "video".equals(type) || "gifv".equals(type);
    }
}
