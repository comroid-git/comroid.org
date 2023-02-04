package org.comroid.auth.controller;

import org.comroid.auth.model.AuthorizationRequest;
import org.comroid.auth.repo.AccountRepository;
import org.comroid.auth.web.WebPagePreparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/flow")
public class FlowController {

    public static final Map<String, AuthorizationRequest> pendingAuthorizations = new HashMap<>();
    @Autowired
    private AccountRepository accounts;

    @GetMapping(value = "/login/{pending}")
    public String flowLogin(
            Model model,
            @PathVariable("pending") String pending
    ) {
        return new WebPagePreparator(model, "generic/login")
                .frame("page/flow")
                .setAttribute("action", "/oauth2/authorize")
                .setAttribute("pending", pending)
                .complete();
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String doFlowLogin(
            Model model,
            HttpSession session,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("pending") String pending,
            @Autowired BCryptPasswordEncoder encoder
    ) {
        var redir = GenericController.tryLogin(accounts, model, session, email, password, encoder, Map.of(
                "pending", pending,
                "action", "/flow/login"
        ));
        if ("redirect:/account".equals(redir)) {
            return "redirect:" + pendingAuthorizations.get(pending).externalForm();
        }
        if (redir != null)
            return redir;
        throw new ResponseStatusException(401, "Unable to authenticate", null);
    }
}
