package com.rasel.common;

import com.rasel.common.Credentials;
import com.rasel.common.RequestIntent;
import java.util.HashMap;

/**
 * parse the clientMessage
 * protocol string format speciifcations for client REQUEST :
 *
 * INTENT*:[connect, send, create, get]
 * CREDENTIALS: [username:password]
 * GROUP: {chat group identifier}
 * DATA: {Message}
 *
 *
 * protocol specification for RESPONSE:
 * STATUS: [OK,FORBIDDEN,ERROR]
 * GROUP: {chat group identifier}
 * DATA_TYPE: [text, json]
 * DATA: {response data, can be anything}
 *
 */
public class RequestParser extends Parser {

    private RequestIntent intent;
    private Credentials credentials;
    private String group;
    private String data;

    private static final String INTENT = "INTENT";
    private static final String CREDENTIALS = "CREDENTIALS";
    private static final String GROUP = "GROUP";
    private static final String DATA = "DATA";

    static {
        // List of macro keys to initialize
        String[] macroKeys = { INTENT, CREDENTIALS, GROUP, DATA };
        for (String key : macroKeys) {
            macros.put(key, "");
        }
    }

    /**
     * dynamically parse intent to corresponding enum value
     *
     * @param intentString
     * @return
     * @throws Exception
     */
    RequestIntent parseIntent(String intentString) throws Exception {
        for (RequestIntent intent : RequestIntent.values()) {
            if (intent.name().equals(intentString)) {
                return intent;
            }
        }
        throw new Exception("Invalid intent \'" + intentString + "'");
    }

    /**
     *
     * @param stream
     */
    public RequestParser(String stream) throws Exception {
        super(stream);
        // validate INTENT value
        String intentString = macros.get(INTENT);

        intent = parseIntent(intentString);

        String credString = macros.get(CREDENTIALS);
        /**
         * build a Credentials object if username and password was provided,if not
         * username can't have ":" Character
         */
        String[] credentialsArray = credString.split(":", 2);

        if (credentialsArray.length == 2) {
            String username = credentialsArray[0];
            String password = credentialsArray[1];
            credentials = new Credentials(username, password);
        }

        // TODO: validate group and data value later
        group = macros.get(GROUP);
        data = macros.get(DATA);
    }

    /**
     * print request in human readable format.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("RequestParser{");
        sb.append("intent=").append(intent);
        sb.append(", credentials=");
        if (credentials != null) {
            sb.append(credentials.toString());
        } else {
            sb.append("null");
        }
        sb.append(", group='").append(group).append("'");
        sb.append(", data='").append(data).append("'");
        sb.append('}');
        return sb.toString();
    }

    public RequestIntent getIntent() {
        return intent;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public String getGroup() {
        return group;
    }

    public String getData() {
        return data;
    }

    public Boolean isAuth() {
        return intent == RequestIntent.AUTH;
    }

    public Boolean isSend() {
        return intent == RequestIntent.SEND;
    }

    public Boolean isCreate() {
        return intent == RequestIntent.CREATE;
    }

    public Boolean isGet() {
        return intent == RequestIntent.GET;
    }

    public Boolean isSignup() {
        return intent == RequestIntent.SIGNUP;
    }
}
