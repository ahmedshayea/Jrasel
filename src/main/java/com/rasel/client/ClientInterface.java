package com.rasel.client;

import java.io.IOException;
import java.util.function.Consumer;

import com.rasel.common.Credentials;
import com.rasel.common.RequestBuilder;
import com.rasel.common.ResponseParser;
import com.rasel.common.ResponseResource;

/**
 * High-level client API for connecting to the server, authenticating,
 * sending requests, and subscribing to asynchronous responses.
 *
 * Usage pattern:
 * 1) connect()
 * 2) authenticate(...) or signup(...)
 * 3) send requests (requestGroups/requestUsers/sendMessage/etc.)
 * 4) subscribe to resources
 * (onUsers/onGroups/onMessages/onAuthSuccess/onAuthFailure)
 *
 * All responses are received asynchronously and dispatched to subscribers
 * based on ResponseResource. Handlers typically run on a background thread;
 * marshal to your UI thread as needed.
 */
public interface ClientInterface {

    // Connection lifecycle

    /**
     * Open the TCP connection to the server and start the background receiver.
     *
     * @throws IOException if the socket cannot be opened
     */
    void connect() throws IOException;

    /**
     * Close the socket connection. Safe to call multiple times.
     * This stops the background receiver and frees resources.
     *
     * @throws IOException if closing the socket fails
     */
    void disconnect() throws IOException;

    /**
     * @return true if a socket is currently connected and not closed
     */
    boolean isConnected();

    // Authentication

    /**
     * Send an authentication request using the provided credentials.
     * This is asynchronous; listen to AUTH_SUCCESS/AUTH_FAILURE via subscriptions.
     *
     * @param credentials username/password
     */
    void authenticate(Credentials credentials);

    /**
     * reset authentication state
     */
    void clearSession();

    /**
     * Send a signup request (create account) using the provided credentials.
     * This is asynchronous; success/failure is delivered via subscriptions.
     *
     * @param credentials desired username/password
     */
    void signup(Credentials credentials);

    /**
     * @return last-known authentication state for this client instance
     */
    boolean isAuthenticated();

    // Requests

    /**
     * Send a chat message to a specific group.
     * The server will broadcast MESSAGES responses asynchronously.
     *
     * @param group   target group name
     * @param message message content
     */
    void sendMessage(String group, String message);

    /**
     * Request creation of a new group.
     * Result is delivered asynchronously (e.g., OK/ERROR via generic responses).
     *
     * @param groupName new group identifier
     * @throws Exception if request construction or send fails
     */
    void requestCreateGroup(String groupName);

    /**
     * Request the server to send groups relevant to the user.
     * Responses will be published with resource=GROUPS.
     */
    void requestGroups();

    /**
     * Request the server to send the list of users.
     * Responses will be published with resource=USERS.
     */
    void requestUsers();

    /**
     * Request the server to send the list of users.
     * Responses will be published with resource=USERS.
     */
    void requestUsers(String groupName);

    /**
     * Request messages for the specified group (if supported by the protocol).
     * Real-time messages will arrive via resource=MESSAGES.
     *
     * @param groupName group identifier
     */
    void requestMessages(String groupName);

    /**
     * Request adding a user to a group (admin-only).
     * Result is delivered asynchronously.
     *
     * @param groupName target group
     * @param username  username to add
     */
    void requestAddUserToGroup(String groupName, String username);

    /**
     * Low-level request sender for pre-built requests.
     * Prefer the typed helpers above when possible.
     *
     * @param request a RequestBuilder with a complete request payload
     */
    void sendRequest(RequestBuilder request);

    // Subscriptions (publish/subscribe by resource)

    /**
     * Subscribe to responses with resource=USERS.
     * Returns an AutoCloseable; call close() to unsubscribe.
     *
     * Note: handler runs on a background thread; marshal to UI thread if needed.
     *
     * @param handler consumer of parsed responses
     * @return unsubscribe handle
     */
    AutoCloseable onUsers(Consumer<ResponseParser> handler);

    /**
     * Subscribe to responses with resource=GROUPS.
     * Returns an AutoCloseable; call close() to unsubscribe.
     *
     * @param handler consumer of parsed responses
     * @return unsubscribe handle
     */
    AutoCloseable onGroups(Consumer<ResponseParser> handler);

    /**
     * Subscribe to responses with resource=MESSAGES.
     * Returns an AutoCloseable; call close() to unsubscribe.
     *
     * @param handler consumer of parsed responses
     * @return unsubscribe handle
     */
    AutoCloseable onMessages(Consumer<ResponseParser> handler);

    /**
     * Subscribe to authentication success events (resource=AUTH_SUCCESS).
     * Returns an AutoCloseable; call close() to unsubscribe.
     *
     * @param handler consumer of parsed responses
     * @return unsubscribe handle
     */
    AutoCloseable onAuthSuccess(Consumer<ResponseParser> handler);

    /**
     * Subscribe to authentication failure events (resource=AUTH_FAILURE).
     * Returns an AutoCloseable; call close() to unsubscribe.
     *
     * @param handler consumer of parsed responses
     * @return unsubscribe handle
     */
    AutoCloseable onAuthFailure(Consumer<ResponseParser> handler);

    /**
     * Generic subscription to any ResponseResource.
     * Returns an AutoCloseable; call close() to unsubscribe.
     *
     * @param resource resource type to listen for
     * @param handler  consumer of parsed responses
     * @return unsubscribe handle
     */
    AutoCloseable on(ResponseResource resource, Consumer<ResponseParser> handler);

    // Legacy (discouraged with async receiver running)

    /**
     * Deprecated: synchronous pull of a single response from the socket.
     * Using this in parallel with the async receiver will cause contention.
     * Prefer the subscription APIs above.
     *
     * @return a parsed response
     * @throws Exception on I/O or protocol errors
     */
    @Deprecated
    ResponseParser getResponse() throws Exception;
}
