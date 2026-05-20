package com.structexam.code.distributed.service;

import org.springframework.cloud.client.ServiceInstance;

import java.net.URI;
import java.util.Map;

public class StaticSandboxServiceInstance implements ServiceInstance {

    private final String serviceId;
    private final String host;
    private final int port;
    private final Map<String, String> metadata;

    public StaticSandboxServiceInstance(String serviceId, String host, int port, Map<String, String> metadata) {
        this.serviceId = serviceId;
        this.host = host;
        this.port = port;
        this.metadata = metadata;
    }

    @Override
    public String getServiceId() {
        return serviceId;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public URI getUri() {
        return URI.create("http://" + host + ":" + port);
    }

    @Override
    public Map<String, String> getMetadata() {
        return metadata;
    }
}
