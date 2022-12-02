package org.comroid.auth.controller;

import org.comroid.auth.repo.AccountRepository;
import org.comroid.auth.repo.SessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/")
public class GenericController {
    @Autowired
    private AccountRepository accounts;

    @GetMapping("/")
    public String index(@Autowired(required = false) HttpSession session) {
        if (session == null)
            return "redirect:/login";
        return "redirect:/account";
    }

    @GetMapping("/register")
    public String register(Model model) {
        return null; // todo
    }

    @GetMapping("/login")
    public String login(Model model) {
        return null; // todo
    }

    @GetMapping("/logout")
    public String logout(@Autowired(required = false) HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
