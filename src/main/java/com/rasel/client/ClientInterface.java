package com.rasel.client;

import com.rasel.common.Credentials;
import com.rasel.common.RequestBuilder;
import com.rasel.common.RequestIntent;
import com.rasel.common.ResponseParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public interface ClientInterface {
    /**
     * connect the current client with the server,
     * throws IOException if connection failed
     * 
     * @throws IOException
     */
    void connect() throws IOException;

    /**
     * authenticate current client
     *
     * @param credentials, instance of class Credentials, username and password.
     * @return boolean , true if auth succeeded, false otherwise
     * @throws Exception
     */
    boolean authenticate(Credentials credentials) throws Exception;

    /**
     * create new account for the current client
     * 
     * @param credentials
     * @return boolean, true if account created, faslse if something went wrong
     * @throws Exception
     */
    boolean signup(Credentials credentials) throws Exception;

    /**
     * self explainatory
     * 
     * @return boolean
     */
    boolean isAuthenticated();

    /**
     * send message to group
     *
     * @param group,  group to send message to it.
     *                **note**: request will fail if you are not member of that
     *                group
     * @param message
     */
    void sendMessage(String group, String message);

    /**
     * manually send request, this method is used internally to send requests,
     * it is rarely accessed directly ,
     * I just made it public to make client class more flexible.
     *
     * @param request, request instnace to be send
     */
    void sendRequest(RequestBuilder request);

    /**
     * retrieve the most recent response.
     *
     * @return ResponseParser, ResponseParser instance
     * @throws Exception, throws exception when malformed response recieved and
     *                    couldn't be parsed.
     */
    ResponseParser getResponse() throws Exception;

    /**
     * create new group , set admin of group to the current client ( user ).
     *
     * @param groupName, unique name for the group
     * @return boolean, true when group created successfully, false otherwise
     * @throws Exception
     */
    boolean createGroup(String groupName) throws Exception;

    /**
     *
     * @return
     * @throws Exception
     */
    String[] listGroups() throws Exception;

    /**
     * return list of usernames in the specific group
     * 
     * @return String[]
     */
    String[] getUsersInGroup(String groupName);

    /**
     * list all usernames in the system.
     * 
     * @return String[]
     */
    String[] getAvailableUsers();

    /**
     * request server to response with the list of groups
     */
    public void requestListGroups();

    /**
     * close socket connection
     */
    void disconnect() throws IOException;

    /**
     * @return boolean, true if client is connected to the server, false otherwise
     */
    boolean isConnected();
}
