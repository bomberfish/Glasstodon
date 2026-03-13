package ca.bomberfish.glasstodon.model;

import com.google.gson.annotations.SerializedName;

/**
 * Represents the application used to post a status.
 * https://docs.joinmastodon.org/entities/Status/#application
 */
public class Application {

    @SerializedName("name")
    public String name;

    @SerializedName("website")
    public String website;
}
