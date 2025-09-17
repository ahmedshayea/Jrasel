package com.rasel.common;

public class ResponseParser extends Parser {

    ResponseStatus status;
    DataType dataType;
    String group;
    String data;
    String senderId;
    String senderName;
    String timestamp;

    private static final String STATUS = "STATUS";
    private static final String DATA_TYPE = "DATA_TYPE";
    private static final String GROUP = "GROUP";
    private static final String DATA = "DATA";
    private static final String SENDER_ID = "SENDER_ID";
    private static final String SENDER_NAME = "SENDER_NAME";
    private static final String TIMESTAMP = "TIMESTAMP";

    static {
        // List of macro keys to initialize
    String[] macroKeys = { STATUS, DATA_TYPE, GROUP, DATA, SENDER_ID, SENDER_NAME, TIMESTAMP };
        for (String key : macroKeys) {
            macros.put(key, "");
        }
    }

    public ResponseParser(String stream) throws Exception {
        super(stream);
        String statusString = macros.get(STATUS);
        // Set intent value
        status = switch (statusString.trim().toUpperCase()) {
            case "OK" -> ResponseStatus.OK;
            case "FORBIDDEN" -> ResponseStatus.FORBIDDEN;
            case "ERROR" -> ResponseStatus.ERROR;
            default -> {
                System.out.println(
                        "[ERROR] Invalid response status: " + statusString);
                throw new Exception(
                        "status: " + "{" + statusString + "}" + " is invalid");
            }
        };

        String dataTypeString = macros.get(DATA_TYPE);
        dataType = switch (dataTypeString.trim().toUpperCase()) {
            case "TEXT" -> DataType.TEXT;
            case "JSON" -> DataType.JSON;
            default -> {
                System.out.println(
                        "[ERROR] Invalid data type : " + dataTypeString);
                throw new Exception(
                        "data type: " + "{" + dataTypeString + "}" + " is invalid");
            }
        };

    group = macros.get(GROUP);
    data = macros.get(DATA);
    senderId = macros.get(SENDER_ID);
    senderName = macros.get(SENDER_NAME);
    timestamp = macros.get(TIMESTAMP);
    }

    /**
     * print request in human readable format.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ResponseParser{");
        sb.append("status=").append(status);
        sb.append(", dataType=");
        sb.append(dataType);
        sb.append(", group='").append(group).append("'");
        sb.append(", data='").append(data).append("'");
        sb.append('}');
        return sb.toString();
    }

    public ResponseStatus getStatus() {
        return status;
    }

    public DataType getDataType() {
        return dataType;
    }

    public String getGroup() {
        return group;
    }

    public String getData() {
        return data;
    }

    public boolean isOk() {
        return status == ResponseStatus.OK;
    }

    public boolean isForbidden() {
        return status == ResponseStatus.FORBIDDEN;
    }

    public boolean isError() {
        return status == ResponseStatus.ERROR;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
