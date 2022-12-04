package org.comroid.auth.entity;

import org.comroid.api.BitmaskAttribute;
import org.comroid.util.Bitmask;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Collection;
import java.util.UUID;

@Entity
@Table(name = "user_accounts")
public class UserAccount implements AuthEntity, UserDetails {
    @Id
    private String uuid;
    @Column(unique = true)
    private String username;
    @Column
    private String email;
    @Column
    private String passwordHash;
    @Column
    private String sessionId;
    @Column
    private int permit;
    @Column
    private boolean enabled = true;
    @Column
    private boolean emailVerified = false;
    @Column
    private boolean locked = false;
    @Column
    private boolean expired = false;
    @Column
    private boolean credentialsExpired = false;

    @Override
    public UUID getUUID() {
        return UUID.fromString(uuid);
    }

    public String getId() {
        return uuid;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return BitmaskAttribute.valueOf(permit, Permit.class);
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return !expired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !locked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return !credentialsExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public int getPermit() {
        return permit;
    }

    public void setPermit(int permit) {
        this.permit = permit;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public void setExpired(boolean expired) {
        this.expired = expired;
    }

    public void setCredentialsExpired(boolean credentialsExpired) {
        this.credentialsExpired = credentialsExpired;
    }

    public UserAccount(String username, String email, String passwordHash) {
        this.uuid = UUID.randomUUID().toString();
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public UserAccount() {
    }

    public void changePermit(Permit permission, boolean state) {
        setPermit(Bitmask.modifyFlag(permit, permission.getValue(), state));
    }

    public boolean hasPermission(Permit permission) {
        return Bitmask.isFlagSet(getPermit(), permission.getValue());
    }

    public enum Permit implements BitmaskAttribute<Permit>, GrantedAuthority {
        Hub,
        AdminServices,
        AdminAccounts;

        @Override
        public String getAuthority() {
            return name();
        }
    }
}
