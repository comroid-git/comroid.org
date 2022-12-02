package org.comroid.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RegisterData {
    @JsonProperty(required = true)
    public String username;
    @JsonProperty(required = true)
    public String email;
    @JsonProperty
    public boolean invalidUsername = false;
    @JsonProperty
    public boolean invalidEmail = false;

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public boolean isInvalidUsername() {
        return invalidUsername;
    }

    public boolean isInvalidEmail() {
        return invalidEmail;
    }

    public RegisterData(String username, String email, boolean invalidUsername, boolean invalidEmail) {
        this.username = username;
        this.email = email;
        this.invalidUsername = invalidUsername;
        this.invalidEmail = invalidEmail;
    }
}
