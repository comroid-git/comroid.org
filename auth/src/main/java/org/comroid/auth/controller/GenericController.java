package org.comroid.auth.controller;

import org.comroid.auth.dto.RegisterData;
import org.comroid.auth.entity.UserAccount;
import org.comroid.auth.repo.AccountRepository;
import org.comroid.auth.web.WebPagePreparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/")
public class GenericController {
    @Autowired
    private AccountRepository accounts;

    @GetMapping("/")
    public String index(HttpSession session) {
        if (session == null)
            return "redirect:/login";
        return "redirect:/account";
    }

    @GetMapping("/register")
    public String register(Model model) {
        return new WebPagePreparator(model, "generic/register").complete();
    }

    @PostMapping("/register")
    public String doRegister(Model model, @RequestBody RegisterData data, @Autowired BCryptPasswordEncoder encoder) {
        boolean invalidUsername = accounts.findByUsername(data.username).isPresent();
        boolean invalidEmail = accounts.findByEmail(data.email).isPresent();
        if (invalidUsername || invalidEmail) {
            data.invalidUsername = invalidUsername;
            data.invalidEmail = invalidEmail;
            return new WebPagePreparator(model, "generic/register")
                    .registerData(data)
                    .complete();
        }
        var account = new UserAccount(data.username, data.email, encoder.encode(data.password));
        accounts.save(account);
        return "redirect:/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

}
