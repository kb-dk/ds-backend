package dk.kb.discover.util.responses.header;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * A standard solr response header.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"zkConnected", "status", "QTime"})
public class ResponseHeader {
    @JsonProperty("zkConnected")
    boolean zkConnected;

    @JsonProperty("status")
    int status;

    @JsonProperty("QTime")
    Long QTime;

    @JsonProperty("params")
    Params params;

    public boolean isZkConnected() {
        return zkConnected;
    }

    public void setZkConnected(boolean zkConnected) {
        this.zkConnected = zkConnected;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Long getQTime() {
        return QTime;
    }

    public void setQTime(Long QTime) {
        this.QTime = QTime;
    }

    public Params getParams() {
        return params;
    }

    public void setParams(Params params) {
        this.params = params;
    }

    @Override
    public String toString() {
        return "ResponseHeader{" +
                "zkConnected=" + zkConnected +
                ", status=" + status +
                ", QTime=" + QTime +
                ", params=" + params +
                '}';
    }
}
