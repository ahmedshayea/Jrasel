package com.rasel.common;

import com.rasel.server.logging.Log;

public class ResponseParser extends Parser {

    ResponseStatus status;
    DataType dataType;
    ResponseResource resource;
    String group;
    String data;

    private static final String STATUS = "STATUS";
    private static final String RESOURCE = "RESOURCE";
    private static final String DATA_TYPE = "DATA_TYPE";
    private static final String GROUP = "GROUP";
    private static final String DATA = "DATA";

    static {
    String[] macroKeys = { STATUS, RESOURCE, DATA_TYPE, GROUP, DATA };
        for (String key : macroKeys) {
            macros.put(key, "");
        }
    }

    public ResponseParser(String stream) throws Exception {
        super(stream);

        String statusString = macros.get(STATUS);
        try {
            status = ResponseStatus.valueOf(statusString.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            Log.error("Invalid response status: %s", statusString);
            throw new Exception("status: {" + statusString + "} is invalid");
        }

        String resourceString = macros.get(RESOURCE);
        if (resourceString != null && !resourceString.trim().isEmpty()) {
            try {
                resource = ResponseResource.valueOf(resourceString.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                Log.warn("Invalid resource type: %s", resourceString);
                resource = null;
            }
        } else {
            resource = null;
        }

        String dataTypeString = macros.get(DATA_TYPE);
        try {
            dataType = DataType.valueOf(dataTypeString.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            Log.error("Invalid data type: %s", dataTypeString);
            throw new Exception("data type: {" + dataTypeString + "} is invalid");
        }

        group = macros.get(GROUP);
        data = macros.get(DATA);
    // sender info is part of DATA (JSON) when resource=MESSAGES
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ResponseParser{");
        sb.append("status=").append(status);
        sb.append(", resource=").append(resource);
        sb.append(", dataType=").append(dataType);
        sb.append(", group='").append(group).append("'");
        sb.append(", data='").append(data).append("'");
        sb.append('}');
        return sb.toString();
    }

    public ResponseStatus getStatus() {
        return status;
    }

    public ResponseResource getResource() {
        return resource;
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


    public boolean isJson() {
        return this.dataType == DataType.JSON;
    }

    public boolean isText() {
        return this.dataType == DataType.TEXT;
    }

}
