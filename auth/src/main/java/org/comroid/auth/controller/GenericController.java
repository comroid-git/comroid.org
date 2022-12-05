package org.comroid.auth.controller;

import org.comroid.auth.dto.RegisterData;
import org.comroid.auth.entity.UserAccount;
import org.comroid.auth.repo.AccountRepository;
import org.comroid.auth.web.WebPagePreparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.Optional;

@Controller
public class GenericController {
    @Autowired
    private AccountRepository accounts;
    @Autowired
    private JavaMailSender mailSender;
    private CaptchaService

    @GetMapping("/")
    public String index(HttpSession session) {
        if (session == null || accounts.findBySessionId(session.getId()).isEmpty())
            return "redirect:/login";
        return "redirect:/account";
    }

    @GetMapping("/register")
    public String register(Model model) {
        return new WebPagePreparator(model, "generic/register")
                .needLogin(false)
                .complete();
    }

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String doRegister(
            Model model,
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @Autowired BCryptPasswordEncoder encoder
    ) {
        boolean invalidUsername = accounts.findByUsername(username).isPresent();
        boolean invalidEmail = accounts.findByEmail(email).isPresent();
        if (invalidUsername || invalidEmail)
            return new WebPagePreparator(model, "generic/register")
                    .registerData(new RegisterData(username, email, invalidUsername, invalidEmail))
                    .complete();
        var account = new UserAccount(username, email, encoder.encode(password));
        accounts.save(account);
        AccountController.initiateEmailVerification(accounts, mailSender, account);
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login(Model model) {
        return new WebPagePreparator(model, "generic/login")
                .needLogin(false)
                .complete();
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String doLogin(
            Model model,
            HttpSession session,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @Autowired BCryptPasswordEncoder encoder
    ) {
        Optional<UserAccount> byUsername = accounts.findByEmail(email);
        if (byUsername.map(account -> !encoder.matches(password, account.getPassword())).orElse(true)) {
            return new WebPagePreparator(model, "generic/login")
                    .setAttribute("email", email)
                    .needLogin(false)
                    .complete();
        }
        var found = byUsername.get();
        if (!found.isEnabled() || !found.isAccountNonLocked() || !found.isAccountNonExpired())
            // todo: unauthorized account error page
            return "redirect:/login";
        if (!found.isCredentialsNonExpired())
            // todo: ask for password change
            return "redirect:/account/start_change_password";
        accounts.setSessionId(found.getId(), session.getId());
        return "redirect:/account";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
