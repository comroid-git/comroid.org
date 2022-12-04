package org.comroid.auth.controller;

import org.comroid.auth.entity.UserAccount;
import org.comroid.auth.repo.AccountRepository;
import org.comroid.auth.web.WebPagePreparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.ServletForwardingController;

import javax.servlet.http.HttpSession;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Controller
@RequestMapping("/account")
public class AccountController {
    @Autowired
    private AccountRepository accounts;

    @GetMapping
    public String index(HttpSession session) {
        if (session == null)
            return "redirect:/login";
        var account = accounts.findBySessionId(session.getId());
        if (account.isEmpty())
            return "redirect:/login";
        return "redirect:/account/" + account.get().getId();
    }

    @GetMapping("/list")
    public String list(Model model, HttpSession session) {
        if (session == null)
            return "redirect:/login";
        var account = accounts.findBySessionId(session.getId());
        if (account.isEmpty() || !account.get().hasPermission(UserAccount.Permit.AdminAccounts))
            return new WebPagePreparator(model, "generic/unauthorized")
                    .complete();
        return new WebPagePreparator(model, "account/list")
                .userAccount(account.get())
                .userAccountList(StreamSupport.stream(accounts.findAll().spliterator(), false).toList())
                .complete();
    }

    @GetMapping("/{id}")
    public String view(Model model, @PathVariable("id") String id) {
        var account = accounts.findById(id);
        if (account.isEmpty())
            return new WebPagePreparator(model, "account/not_found")
                    .complete();
        return new WebPagePreparator(model, "account/view")
                .userAccount(account.get())
                .complete();
    }

    @GetMapping("/edit")
    public String edit(Model model, HttpSession session) {
        if (session == null)
            return "redirect:/login";
        var account = accounts.findBySessionId(session.getId());
        if (account.isEmpty())
            return new WebPagePreparator(model, "account/not_found")
                    .complete();
        return new WebPagePreparator(model, "account/edit")
                .userAccount(account.orElse(null))
                .complete();
    }

    @GetMapping("/change_password")
    public String changePassword(Model model, HttpSession session) {
        if (session == null)
            return "redirect:/login";
        var account = accounts.findBySessionId(session.getId());
        if (account.isEmpty())
            return new WebPagePreparator(model, "generic/unauthorized")
                    .complete();
        return new WebPagePreparator(model, "account/change_password")
                .userAccount(account.orElse(null))
                .complete();
    }
}
