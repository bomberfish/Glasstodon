package ca.bomberfish.glasstodon.model;

public enum AccountTimelineType {
    DEFAULT,WITH_REPLIES,MEDIA_ONLY,PINNED;
    
    public String getEndpoint(String accountId) {
        String base = "/v1/accounts/" + accountId + "/statuses";
        switch (this) {
            case DEFAULT:
                return base;
            case WITH_REPLIES:
                return base + "?exclude_replies=false";
            case MEDIA_ONLY:
                return base + "?only_media=true";
            case PINNED:
                return base + "?pinned=true";
            default:
                throw new IllegalArgumentException("Unknown timeline type: " + this);
        }
    }
}
