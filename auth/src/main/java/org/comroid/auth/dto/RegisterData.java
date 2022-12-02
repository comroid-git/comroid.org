package org.comroid.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RegisterData {
    @JsonProperty(required = true)
    public String username;
    @JsonProperty(required = true)
    public String email;
    @JsonProperty(required = true)
    public String password;
    @JsonProperty
    public boolean invalidUsername = false;
    @JsonProperty
    public boolean invalidEmail = false;
}
