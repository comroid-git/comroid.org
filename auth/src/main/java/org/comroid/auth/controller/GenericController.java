package org.comroid.auth.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.comroid.auth.dto.RegisterData;
import org.comroid.auth.entity.UserAccount;
import org.comroid.auth.repo.AccountRepository;
import org.comroid.auth.web.WebPagePreparator;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.NestedServletException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.Optional;

@Controller
public class GenericController implements ErrorController {
    @Autowired
    private AccountRepository accounts;
    @Autowired
    private JavaMailSender mailSender;

    @GetMapping("/")
    public String index(HttpSession session) {
        if (session == null || accounts.findBySessionId(session.getId()).isEmpty())
            return "redirect:/login";
        return "redirect:/account";
    }

    @GetMapping("/error")
    public String error(Model model, HttpSession session, HttpServletRequest request) {
        var ex = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        var sw = new StringWriter();
        var pw = new PrintWriter(sw);
        if (ex instanceof NestedServletException) {
            ex = ex.getCause();
            ex.printStackTrace(pw);
        }
        int code = (int)request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        String codeMessage = code + " - ";
        HttpStatus status = HttpStatus.resolve(code);
        if (status == null)
            codeMessage += "Internal Server Error";
        else codeMessage += status.getReasonPhrase();
        if (code == 404)
            codeMessage += ": " + request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        return new WebPagePreparator(model, "generic/error")
                .userAccount(accounts.findBySessionId(session.getId()).orElse(null))
                .setAttribute("code", codeMessage)
                .setAttribute("message", request.getAttribute(RequestDispatcher.ERROR_MESSAGE))
                .setAttribute("stacktrace", sw.toString().replace("\r\n", "\n"))
                .complete();
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

    @RequestMapping
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String doLogin(
            Model model,
            HttpSession session,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @Autowired BCryptPasswordEncoder encoder
    ) {
        var redir = tryLogin(accounts, model, session, email, password, encoder, null);
        if (redir == null)
            redir = "redirect:/login";
        return redir;
    }

    public static String tryLogin(
            AccountRepository accounts,
            @Nullable Model model,
            HttpSession session,
            String email,
            String password,
            PasswordEncoder encoder,
            @Nullable Map<String, Object> additionalAttributes
    ) {
        Optional<UserAccount> byUsername = accounts.findByEmail(email);
        if (model != null && byUsername.map(account -> !encoder.matches(password, account.getPassword())).orElse(true)) {
            WebPagePreparator wpp = new WebPagePreparator(model, "generic/login");
            if (additionalAttributes != null)
                additionalAttributes.forEach(wpp::setAttribute);
            return wpp
                    .setAttribute("email", email)
                    .needLogin(false)
                    .complete();
        }
        if (byUsername.isEmpty())
            return null;
        var found = byUsername.get();
        if (!found.isEnabled() || !found.isAccountNonLocked() || !found.isAccountNonExpired())
            return null;
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
