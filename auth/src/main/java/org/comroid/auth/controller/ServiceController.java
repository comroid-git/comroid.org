package org.comroid.auth.controller;

import org.comroid.auth.entity.AuthService;
import org.comroid.auth.entity.UserAccount;
import org.comroid.auth.repo.AccountRepository;
import org.comroid.auth.repo.ServiceRepository;
import org.comroid.auth.web.WebPagePreparator;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpSession;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/service")
public class ServiceController {
    @Autowired
    private AccountRepository accounts;
    @Autowired
    private ServiceRepository services;

    @GetMapping("/{id}")
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public String view(Model model, @PathVariable("id") UUID id, HttpSession session) {
        if (session == null)
            return "redirect:/login";
        var account = accounts.findBySessionId(session.getId());
        var service = services.findById(id);
        var redirect = performChecks(model, account, service);
        if (redirect != null)
            return redirect;
        return new WebPagePreparator(model, "service/view")
                .userAccount(account.get())
                .authService(service.get())
                .complete();
    }

    @GetMapping("/{id}/edit")
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public String edit(Model model, @PathVariable("id") UUID id, HttpSession session) {
        if (session == null)
            return "redirect:/login";
        var account = accounts.findBySessionId(session.getId());
        var service = services.findById(id);
        var redirect = performChecks(model, account, service);
        if (redirect != null)
            return redirect;
        return new WebPagePreparator(model, "service/edit")
                .userAccount(account.get())
                .authService(service.get())
                .complete();
    }

    @GetMapping("/{id}/delete")
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public String delete(Model model, @PathVariable("id") UUID id, HttpSession session) {
        if (session == null)
            return "redirect:/login";
        var account = accounts.findBySessionId(session.getId());
        var service = services.findById(id);
        var redirect = performChecks(model, account, service);
        if (redirect != null)
            return redirect;
        return new WebPagePreparator(model, "service/delete")
                .userAccount(account.get())
                .authService(service.get())
                .complete();
    }

    private @Nullable String performChecks(Model model, Optional<UserAccount> account, Optional<AuthService> service) {
        if (account.isEmpty())
            return "redirect:/login";
        if (!account.get().hasPermission(UserAccount.Permit.Services))
            return new WebPagePreparator(model, "generic/unauthorized")
                    .userAccount(account.get())
                    .complete();
        if (service.isEmpty())
            return new WebPagePreparator(model, "service/not_found").complete();
        return null;
    }
}
