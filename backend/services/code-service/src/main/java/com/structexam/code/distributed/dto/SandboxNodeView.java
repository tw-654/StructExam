package com.structexam.code.distributed.dto;

import java.net.URI;
import java.util.Map;

public class SandboxNodeView {
    private String serviceId;
    private String host;
    private int port;
    private URI uri;
    private boolean healthy;
    private int runningTasks;
    private Map<String, String> metadata;

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public boolean isHealthy() {
        return healthy;
    }

    public void setHealthy(boolean healthy) {
        this.healthy = healthy;
    }

    public int getRunningTasks() {
        return runningTasks;
    }

    public void setRunningTasks(int runningTasks) {
        this.runningTasks = runningTasks;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
}
