package org.comroid.status.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("org.comroid.status")
public class StatusConnectionConfig {
    private String serviceName;
    private String accessToken;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
