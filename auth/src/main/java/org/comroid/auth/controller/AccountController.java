package org.comroid.auth.controller;

import org.comroid.auth.repo.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/account")
public class AccountController {
    @Autowired
    private AccountRepository accounts;

    @GetMapping("/")
    public String viewAccount(Model model, @CookieValue("SESSION") String sessionId) {
        var account = accounts.findBySessionId(sessionId);
        model.addAttribute("loggedIn", account.isPresent());
        model.addAttribute("account", account.orElse(null));
        return "account/view";
    }
}
