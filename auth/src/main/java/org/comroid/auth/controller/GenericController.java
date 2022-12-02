package org.comroid.auth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class GenericController {
    @GetMapping("/login")
    public String login() {
        return null; // todo
    }

    @GetMapping("/logout")
    public String logout() {
        return null; // todo
    }

    @GetMapping("/register")
    public String register() {
        return null; // todo
    }
}
