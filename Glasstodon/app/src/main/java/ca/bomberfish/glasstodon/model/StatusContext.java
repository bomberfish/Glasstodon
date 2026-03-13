package ca.bomberfish.glasstodon.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Represents the tree of statuses (ancestors + descendants) around a given status.
 * https://docs.joinmastodon.org/entities/Context/
 */
public class StatusContext {

    /** Statuses that were posted before and lead up to this status */
    @SerializedName("ancestors")
    public List<Status> ancestors;

    /** Statuses that were posted after and are replies to this status */
    @SerializedName("descendants")
    public List<Status> descendants;
}
