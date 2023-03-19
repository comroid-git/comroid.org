package org.comroid.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.comroid.api.BitmaskAttribute;
import org.comroid.util.Bitmask;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

@Entity
@Table(name = "user_accounts")
public class UserAccount implements AuthEntity, UserDetails {
    @Id
    private String uuid;
    @Column(unique = true)
    private String username;
    @Column(unique = true)
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
    @Column(unique = true)
    private String emailVerifyCode;
    @Column(unique = true)
    private String changePasswordCode;

    public String getChangePasswordCode() {
        return changePasswordCode;
    }

    public void setChangePasswordCode(String changePasswordCode) {
        this.changePasswordCode = changePasswordCode;
    }

    public String getEmailVerifyCode() {
        return emailVerifyCode;
    }

    public void setEmailVerifyCode(String emailVerifyCode) {
        this.emailVerifyCode = emailVerifyCode;
    }

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
        this.permit = Permit.User.getValue();
    }

    public UserAccount() {
    }

    public void setPermit(Permit permission, boolean state) {
        this.permit = Bitmask.modifyFlag(permit, permission.getValue(), state);
    }

    public boolean hasPermission(Permit permission) {
        return Bitmask.isFlagSet(getPermit(), permission.getValue());
    }

    public Authentication createAuthentication(PasswordEncoder encoder, Duration validDuration) {
        return new AuthImpl(encoder, validDuration);
    }

    private final class AuthImpl implements Authentication {
        private final PasswordEncoder encoder;
        private final String token;
        private final Instant validUntil;

        private AuthImpl(PasswordEncoder encoder, Duration validDuration) {
            this.encoder = encoder;
            this.token = encoder.encode(composePassword());
            this.validUntil = Instant.now().plus(validDuration);
        }

        private String composePassword() {
            return getId() + ':' + getSessionId();
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return BitmaskAttribute.valueOf(getPermit(), Permit.class);
        }

        @Override
        public Object getCredentials() {
            return token;
        }

        @Override
        public Object getDetails() {
            return UserAccount.this;
        }

        @Override
        public Object getPrincipal() {
            return UserAccount.this;
        }

        @Override
        public boolean isAuthenticated() {
            return validUntil.isBefore(Instant.now()) && encoder.matches(composePassword(), token);
        }

        @Override
        public String getName() {
            return getEmail();
        }

        @Override
        public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
            throw new UnsupportedOperationException();
        }
    }

    public enum Permit implements BitmaskAttribute<Permit>, GrantedAuthority {
        User,
        Hub,
        AdminServices,
        AdminAccounts;

        @Override
        public String getAuthority() {
            return name();
        }
    }
}
