package org.comroid.auth.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "services")
public class AuthService implements AuthEntity {
    @Id
    private String uuid;
    @Column
    private String name;
    @Column
    private String url;
    @Column
    private String callbackUrl;
    @Column
    private String requiredScope;

    @Override
    public UUID getUUID() {
        return UUID.fromString(uuid);
    }

    public String getId() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public String getRequiredScope() {
        return requiredScope;
    }

    public void setRequiredScope(String requiredScope) {
        this.requiredScope = requiredScope;
    }

    public AuthService() {
    }

    public AuthService(String name, String url, String callbackUrl, String requiredScope) {
        this.uuid = UUID.randomUUID().toString();
        this.name = name;
        this.url = url;
        this.callbackUrl = callbackUrl;
        this.requiredScope = requiredScope;
    }
}
