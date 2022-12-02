package org.comroid.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LoginData {
    @JsonProperty(required = true)
    public String username;
    @JsonProperty(required = true)
    public String password;

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
