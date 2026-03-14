package ca.bomberfish.glasstodon.model;

public enum TimelineType {
    HOME,FEDERATED,LOCAL,BOOKMARKS;

    public String getEndpoint() {
        switch (this) {
            case HOME:
                return "/v1/timelines/home";
            case FEDERATED:
                return "/v1/timelines/public?remote=true";
            case LOCAL:
                return "/v1/timelines/public?local=true";
            case BOOKMARKS:
                return "/v1/bookmarks";
            default:
                throw new IllegalArgumentException("Unknown timeline: " + this);
        }
    }
}
