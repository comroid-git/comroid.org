package org.comroid.status.spring;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.stereotype.Component;

@Component
@ConfigurationPropertiesScan(basePackageClasses = StatusConnectionConfig.class)
public class StatusConnection {
    @Autowired
    private StatusConnectionConfig config;
    @SuppressWarnings("FieldCanBeLocal")
    private org.comroid.status.StatusConnection connection;

    @PostConstruct
    public void init() {
        this.connection = new org.comroid.status.StatusConnection(config.getServiceName(), config.getAccessToken());

        connection.startPolling();
    }
}
