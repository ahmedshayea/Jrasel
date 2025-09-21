package com.rasel.common;

/**
 * A builder class for creating requests to be sent to the Rasel server.
 * This class provides a fluent API for constructing requests with different intents,
 * credentials, groups, and data.
 * <p>
 * The request protocol is a simple text-based format with key-value pairs.
 * Each request has an intent, and optional credentials, group, and data fields.
 * The request is terminated by the "END_OF_REQUEST" string.
 * </p>
 */
public class RequestBuilder {

    private RequestIntent intent;
    private Credentials credentials;
    private String group;
    private String data;

    /**
     * Constructs a new RequestBuilder with the specified intent.
     *
     * @param intent The intent of the request.
     */
    public RequestBuilder(RequestIntent intent) {
        this(intent, null, null, null);
    }

    /**
     * Constructs a new RequestBuilder with the specified intent, credentials, group, and data.
     *
     * @param intent        The intent of the request.
     * @param credentials   The user's credentials for authentication.
     * @param group         The target group for the request.
     * @param data          The data payload of the request.
     */
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

    /**
     * Builds the request string based on the configured parameters.
     *
     * @return The formatted request string.
     */
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

    /**
     * Creates an authentication request.
     *
     * @param credentials The user's credentials.
     * @return The formatted authentication request string.
     */
    public static String authRequest(Credentials credentials) {
        return new RequestBuilder(
            RequestIntent.AUTH,
            credentials,
            null,
            null
        ).getRequest();
    }

    /**
     * Creates a request to send a message to a group.
     *
     * @param credentials The user's credentials.
     * @param group       The target group.
     * @param message     The message to send.
     * @return The formatted send message request string.
     */
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

    /**
     * Creates a request to create a new group.
     *
     * @param credentials The user's credentials.
     * @param groupName   The name of the group to create.
     * @return The formatted create group request string.
     */
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

    /**
     * Creates a request to get information about a group.
     *
     * @param credentials The user's credentials.
     * @param groupName   The name of the group to get.
     * @return The formatted get group request string.
     */
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

    /**
     * Creates a request to get a list of all groups.
     *
     * @param credentials The user's credentials.
     * @return The formatted get groups request string.
     */
    public static String getGroupsRequest(Credentials credentials) {
        return new RequestBuilder(
            RequestIntent.GET_GROUPS,
            credentials,
            null,
            null
        ).getRequest();
    }

    /**
     * Creates a request to get a list of all users.
     *
     * @param credentials The user's credentials.
     * @return The formatted get users request string.
     */
    public static String getUsersRequest(Credentials credentials) {
        return new RequestBuilder(
            RequestIntent.GET_USERS,
            credentials,
            null,
            null
        ).getRequest();
    }

    /**
     * Creates a request to get a list of users in a specific group.
     *
     * @param credentials The user's credentials.
     * @param groupName   The name of the group.
     * @return The formatted get users request string.
     */
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

    /**
     * Creates a request to add a user to a group.
     *
     * @param credentials The user's credentials.
     * @param groupName   The name of the group.
     * @param username    The username of the user to add.
     * @return The formatted add user to group request string.
     */
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

    /**
     * Sets the credentials for the request.
     *
     * @param credentials The user's credentials.
     * @return This RequestBuilder instance for chaining.
     */
    public RequestBuilder withCredentials(Credentials credentials) {
        this.credentials = credentials;
        return this;
    }

    /**
     * Sets the group for the request.
     *
     * @param group The target group.
     * @return This RequestBuilder instance for chaining.
     */
    public RequestBuilder withGroup(String group) {
        this.group = group;
        return this;
    }

    /**
     * Sets the data for the request.
     *
     * @param data The data payload.
     * @return This RequestBuilder instance for chaining.
     */
    public RequestBuilder withData(String data) {
        this.data = data;
        return this;
    }
}
