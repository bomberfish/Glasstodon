package ca.bomberfish.glasstodon.model;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a custom emoji.
 * https://docs.joinmastodon.org/entities/CustomEmoji/
 */
public class CustomEmoji {

    @SerializedName("shortcode")
    public String shortcode;

    @SerializedName("url")
    public String url;

    @SerializedName("static_url")
    public String staticUrl;

    @SerializedName("visible_in_picker")
    public boolean visibleInPicker;
}
