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
    RESOURCE,
    DATA_TYPE,
    GROUP,
    DATA,
}

public class ResponseBuilder implements Response {
    /** Protocol terminator for responses (kept public for reuse by clients). */
    public static final String END_OF_RESPONSE = "END_OF_RESPONSE";

    private ResponseStatus status;
    private ResponseResource resource;
    private String group;
    private DataType dataType;
    private String data;

    // Main constructor with all fields
    public ResponseBuilder(String data, DataType dataType, String group, ResponseStatus status,
            ResponseResource resource) {
        this.data = data != null ? data : "";
        this.dataType = dataType != null ? dataType : DataType.TEXT;
        this.group = group;
        this.status = status != null ? status : ResponseStatus.OK;
        this.resource = resource;
    }

    // All possible constructor combinations
    public ResponseBuilder() {
    this("", DataType.TEXT, null, ResponseStatus.OK, null);
    }

    public ResponseBuilder(String data) {
    this(data, DataType.TEXT, null, ResponseStatus.OK, null);
    }

    public ResponseBuilder(String data, DataType dataType) {
    this(data, dataType, null, ResponseStatus.OK, null);
    }

    public ResponseBuilder(String data, DataType dataType, String group) {
    this(data, dataType, group, ResponseStatus.OK, null);
    }

    public ResponseBuilder(String data, DataType dataType, String group, ResponseStatus status) {
    this(data, dataType, group, status, null);
    }

    // no extra constructor; the above serves as the main one

    // Static factories for OK
    public static ResponseBuilder ok(String data) {
    return new ResponseBuilder(data, DataType.TEXT, null, ResponseStatus.OK, null);
    }

    public static ResponseBuilder ok(String data, ResponseResource resource) {
    return new ResponseBuilder(data, DataType.TEXT, null, ResponseStatus.OK, resource);
    }

    public static ResponseBuilder ok(String data, String group) {
    return new ResponseBuilder(data, DataType.TEXT, group, ResponseStatus.OK, null);
    }

    public static ResponseBuilder ok(String data, String group, ResponseResource resource) {
    return new ResponseBuilder(data, DataType.TEXT, group, ResponseStatus.OK, resource);
    }

    public static ResponseBuilder ok(String data, DataType dataType, String group, ResponseResource resource) {
    return new ResponseBuilder(data, dataType, group, ResponseStatus.OK, resource);
    }

    // Static factories for FORBIDDEN
    public static ResponseBuilder forbidden(String data) {
    return new ResponseBuilder(data, DataType.TEXT, null, ResponseStatus.FORBIDDEN, null);
    }

    public static ResponseBuilder forbidden(String data, ResponseResource resource) {
    return new ResponseBuilder(data, DataType.TEXT, null, ResponseStatus.FORBIDDEN, resource);
    }

    public static ResponseBuilder forbidden(String data, String group, ResponseResource resource) {
    return new ResponseBuilder(data, DataType.TEXT, group, ResponseStatus.FORBIDDEN, resource);
    }

    // Static factories for ERROR
    public static ResponseBuilder error(String data) {
    return new ResponseBuilder(data, DataType.TEXT, null, ResponseStatus.ERROR, null);
    }

    public static ResponseBuilder error(String data, ResponseResource resource) {
    return new ResponseBuilder(data, DataType.TEXT, null, ResponseStatus.ERROR, resource);
    }

    public static ResponseBuilder error(String data, String group, ResponseResource resource) {
    return new ResponseBuilder(data, DataType.TEXT, group, ResponseStatus.ERROR, resource);
    }

    // Convenience JSON factories
    public static ResponseBuilder okJson(String json) {
    return new ResponseBuilder(json, DataType.JSON, null, ResponseStatus.OK, null);
    }

    public static ResponseBuilder okJson(String json, String group) {
    return new ResponseBuilder(json, DataType.JSON, group, ResponseStatus.OK, null);
    }

    public static ResponseBuilder errorJson(String json) {
    return new ResponseBuilder(json, DataType.JSON, null, ResponseStatus.ERROR, null);
    }

    // Fluent builder-style API (non-breaking, returns this)
    public static ResponseBuilder builder() {
        return new ResponseBuilder();
    }

    public ResponseBuilder status(ResponseStatus status) {
        this.status = status != null ? status : ResponseStatus.OK;
        return this;
    }

    public ResponseBuilder resource(ResponseResource resource) {
        this.resource = resource;
        return this;
    }

    public ResponseBuilder group(String group) {
        this.group = group;
        return this;
    }

    public ResponseBuilder dataType(DataType dataType) {
        this.dataType = dataType != null ? dataType : DataType.TEXT;
        return this;
    }

    public ResponseBuilder data(String data) {
        this.data = data != null ? data : "";
        return this;
    }

    // Sender fields removed from protocol; include sender info inside DATA when needed (e.g., message payload JSON).

    /** No-op for fluency; returns the same instance. */
    public ResponseBuilder build() {
        return this;
    }

    @Override
    public String getResponseString() {
        StringBuilder sb = new StringBuilder();
        sb.append(RM.STATUS.name()).append(":").append(status.name()).append("\n");
        sb.append(RM.RESOURCE.name()).append(":").append(resource != null ? resource.name() : "").append("\n");
        sb.append(RM.DATA_TYPE.name()).append(":").append(dataType.name()).append("\n");
        sb.append(RM.GROUP.name()).append(":").append(group != null ? group : "").append("\n");
        sb.append(RM.DATA.name()).append(":").append(data).append("\n");
        return sb.append(END_OF_RESPONSE).toString().trim();
    }

    @Deprecated
    public String getResponse() {
        return getResponseString();
    }

    @Override
    public String toString() {
        return getResponseString();
    }

    @Override
    public ResponseStatus getStatus() {
        return status;
    }

    @Override
    public void setStatus(ResponseStatus status) {
        this.status = status;
    }

    @Override
    public ResponseResource getResource() {
        return resource;
    }

    public void setResource(ResponseResource resource) {
        this.resource = resource;
    }

    @Override
    public DataType getDataType() {
        return dataType;
    }

    @Override
    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public void setGroup(String group) {
        this.group = group;
    }

    @Override
    public String getData() {
        return data;
    }

    @Override
    public void setData(String data) {
        this.data = data;
    }

    // Sender fields removed from protocol

    @Override
    public boolean isOk() {
        return this.status == ResponseStatus.OK;
    }

    @Override
    public boolean isError() {
        return this.status == ResponseStatus.ERROR;
    }

    @Override
    public boolean isForbidden() {
        return this.status == ResponseStatus.FORBIDDEN;
    }

    public boolean isJson() {
        return this.dataType == DataType.JSON;
    }

    public boolean isText() {
        return this.dataType == DataType.TEXT;
    }
}
