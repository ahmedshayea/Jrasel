package com.rasel.common;

/**
 * protocol string format speciifcations for server RESPONSE :
 * ----------------------------------------
 * STATUS: [OK,FORBIDDEN,ERROR]
 * GROUP: {chat group identifier}
 * DATA_TYPE: [text, json]
 * DATA: {response data, can be anything}
 */

enum RM {
    STATUS,
    DATA_TYPE,
    GROUP,
    DATA,
    SENDER_ID,
    SENDER_NAME,
    TIMESTAMP,
}

public class ResponseBuilder implements Response {

    private ResponseStatus status;
    private String group;
    private DataType dataType;
    private String data;
    // Optional metadata
    private String senderId;
    private String senderName;
    private String timestamp;

    public ResponseBuilder() {
        this("");
    }

    public ResponseBuilder(String data) {
        this(data, DataType.TEXT, null, ResponseStatus.OK);
    }

    public ResponseBuilder(String data, DataType dataType) {
        this(data, dataType, null, ResponseStatus.OK);
    }

    public ResponseBuilder(String data, DataType dataType, String group) {
        this(data, dataType, group, ResponseStatus.OK);
    }

    public ResponseBuilder(
            String data,
            DataType dataType,
            String group,
            ResponseStatus status) {
        this.data = data;
        this.dataType = dataType;
        this.group = group;
        this.status = status;
    }

    public static ResponseBuilder ok(String data) {
        return new ResponseBuilder(
                data,
                DataType.TEXT,
                null,
                ResponseStatus.OK);
    }

    public static ResponseBuilder ok(Object data) {
        return new ResponseBuilder(
                data.toString(),
                DataType.TEXT,
                null,
                ResponseStatus.OK);
    }

    public static ResponseBuilder ok(String data, String group) {
        return new ResponseBuilder(
                data,
                DataType.TEXT,
                group,
                ResponseStatus.OK);
    }

    public static ResponseBuilder ok(Object data, String group) {
        return new ResponseBuilder(
                data.toString(),
                DataType.TEXT,
                group,
                ResponseStatus.OK);
    }

    public static ResponseBuilder okWithSender(String data, String group, String senderId, String senderName,
            String timestamp) {
        ResponseBuilder rb = ok(data, group);
        rb.setSenderId(senderId);
        rb.setSenderName(senderName);
        rb.setTimestamp(timestamp);
        return rb;
    }

    public static ResponseBuilder forbidden(String data) {
        return new ResponseBuilder(
                data,
                DataType.TEXT,
                null,
                ResponseStatus.FORBIDDEN);
    }

    public static ResponseBuilder error(String data) {
        return new ResponseBuilder(
                data,
                DataType.TEXT,
                null,
                ResponseStatus.ERROR);
    }

    /**
     *
     */
    public String getResponseString() {
        StringBuilder sb = new StringBuilder();
        sb.append(RM.STATUS.name() + ":").append(status.name()).append("\n");
        sb
                .append(RM.DATA_TYPE.name() + ":")
                .append(dataType.name())
                .append("\n");
        if (group != null) {
            sb.append(RM.GROUP.name() + ":").append(group).append("\n");
        } else {
            sb.append(RM.GROUP.name() + ":").append("").append("\n");
        }
        if (senderId != null) {
            sb.append(RM.SENDER_ID.name() + ":").append(senderId).append("\n");
        }
        if (senderName != null) {
            sb.append(RM.SENDER_NAME.name() + ":").append(senderName).append("\n");
        }
        if (timestamp != null) {
            sb.append(RM.TIMESTAMP.name() + ":").append(timestamp).append("\n");
        }
        sb.append(RM.DATA.name() + ":").append(data).append("\n");
        return sb.append("END_OF_RESPONSE").toString().trim();
    }

    /**
     * @deprecated , use getResponseString instead.
     * @return {String}
     */
    public String getResponse() {
        return getResponseString();
    }

    @Override
    public String toString() {
        return getResponseString();
    }

    public ResponseStatus getStatus() {
        return status;
    }

    public void setStatus(ResponseStatus status) {
        this.status = status;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
