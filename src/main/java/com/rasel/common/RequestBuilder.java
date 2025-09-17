package com.rasel.common;

/**
 * build Request to send to Rasel server
 * Request
 */
public class RequestBuilder {

    RequestIntent intent;
    Credentials credentials; // can be null
    String group; // can be null
    String data; // can be null

    public RequestBuilder(RequestIntent intent) {
        this(intent, null, null, null);
    }

    public RequestBuilder(
        RequestIntent intent,
        Credentials credentials,
        String group,
        String data
    ) {
        this.intent = intent;
        this.credentials = credentials;
        this.group = group;
        this.data = data;
    }

    public String getRequest() {
        StringBuilder sb = new StringBuilder();
        sb.append("INTENT:").append(intent.name()).append("\n");
        if (credentials != null) {
            sb
                .append("CREDENTIALS:")
                .append(credentials.username)
                .append(":")
                .append(credentials.password)
                .append("\n");
        }
        if (group != null) {
            sb.append("GROUP:").append(group).append("\n");
        }
        if (data != null) {
            sb.append("DATA:").append(data).append("\n");
        }
        return sb.append("END_OF_REQUEST").toString().trim(); // remove the last \n
    }

    // --- Utility methods for common requests ---

    public static String authRequest(Credentials credentials) {
        return new RequestBuilder(
            RequestIntent.AUTH,
            credentials,
            null,
            null
        ).getRequest();
    }

    public static String sendMessageRequest(
        Credentials credentials,
        String group,
        String message
    ) {
        return new RequestBuilder(
            RequestIntent.SEND,
            credentials,
            group,
            message
        ).getRequest();
    }

    public static String createGroupRequest(
        Credentials credentials,
        String groupName
    ) {
        return new RequestBuilder(
            RequestIntent.CREATE,
            credentials,
            groupName,
            null
        ).getRequest();
    }

    public static String getGroupRequest(
        Credentials credentials,
        String groupName
    ) {
        return new RequestBuilder(
            RequestIntent.GET,
            credentials,
            groupName,
            null
        ).getRequest();
    }

    public static String getGroupsRequest(Credentials credentials) {
        return new RequestBuilder(
            RequestIntent.GET_GROUPS,
            credentials,
            null,
            null
        ).getRequest();
    }

    public static String getUsersRequest(Credentials credentials) {
        return new RequestBuilder(
            RequestIntent.GET_USERS,
            credentials,
            null,
            null
        ).getRequest();
    }

    public static String getUsersRequest(
        Credentials credentials,
        String groupName
    ) {
        return new RequestBuilder(
            RequestIntent.GET_USERS,
            credentials,
            groupName,
            null
        ).getRequest();
    }

    public static String addUserToGroupRequest(
        Credentials credentials,
        String groupName,
        String username
    ) {
        // Use DATA field to carry the target username
        return new RequestBuilder(
            RequestIntent.ADD,
            credentials,
            groupName,
            username
        ).getRequest();
    }

    // --- Builder-style methods for chaining (optional) ---

    public RequestBuilder withCredentials(Credentials credentials) {
        this.credentials = credentials;
        return this;
    }

    public RequestBuilder withGroup(String group) {
        this.group = group;
        return this;
    }

    public RequestBuilder withData(String data) {
        this.data = data;
        return this;
    }
}
