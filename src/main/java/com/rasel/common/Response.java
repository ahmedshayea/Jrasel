package com.rasel.common;

public interface Response {
    /**
     * @return
     */
    public String getResponseString();

    public ResponseStatus getStatus();

    public void setStatus(ResponseStatus status);

    public DataType getDataType();

    public void setDataType(DataType dataType);

    public String getGroup();

    public void setGroup(String group);

    public String getData();

    public void setData(String data);

    public boolean isOk();

    public boolean isError();

    public boolean isForbidden();

    public ResponseResource getResource();
}
