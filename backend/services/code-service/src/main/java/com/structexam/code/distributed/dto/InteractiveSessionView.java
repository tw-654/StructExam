package com.structexam.code.distributed.dto;

import java.net.URI;

public class InteractiveSessionView {
    private String gatewaySessionId;
    private URI sandboxUri;
    private String status;

    public String getGatewaySessionId() {
        return gatewaySessionId;
    }

    public void setGatewaySessionId(String gatewaySessionId) {
        this.gatewaySessionId = gatewaySessionId;
    }

    public URI getSandboxUri() {
        return sandboxUri;
    }

    public void setSandboxUri(URI sandboxUri) {
        this.sandboxUri = sandboxUri;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
