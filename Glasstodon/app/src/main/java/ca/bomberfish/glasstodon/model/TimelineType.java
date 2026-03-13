package ca.bomberfish.glasstodon.model;

public enum TimelineType {
    HOME,FEDERATED,LOCAL,BOOKMARKS;

    public String getEndpoint() {
        switch (this) {
            case HOME:
                return "/api/v1/timelines/following";
            case FEDERATED:
                return "/api/v1/timelines/public?local=false";
            case LOCAL:
                return "/api/v1/timelines/public?local=true";
            case BOOKMARKS:
                return "/api/v1/bookmarks";
            default:
                throw new IllegalArgumentException("Unknown timeline: " + this);
        }
    }
}
