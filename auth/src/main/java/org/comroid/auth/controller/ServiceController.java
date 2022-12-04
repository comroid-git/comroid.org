package org.comroid.auth.controller;

import org.comroid.auth.entity.AuthService;
import org.comroid.auth.entity.UserAccount;
import org.comroid.auth.repo.AccountRepository;
import org.comroid.auth.repo.ServiceRepository;
import org.comroid.auth.web.WebPagePreparator;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

@Controller
@RequestMapping("/services")
@SuppressWarnings("OptionalGetWithoutIsPresent")
public class ServiceController {
    @Autowired
    private AccountRepository accounts;
    @Autowired
    private ServiceRepository services;

    @GetMapping
    public String index(Model model, HttpSession session) {
        if (session == null)
            return "redirect:/login";
        var account = accounts.findBySessionId(session.getId());
        var redirect = performChecks(model, account);
        if (redirect != null)
            return redirect;
        return new WebPagePreparator(model, "service/list")
                .userAccount(account.get())
                .authServiceList(StreamSupport.stream(services.findAll().spliterator(), false).toList())
                .complete();
    }

    @GetMapping("/{id}")
    public String view(Model model, @PathVariable("id") String id, HttpSession session) {
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
    public String edit(Model model, @PathVariable("id") String id, HttpSession session) {
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
    public String delete(Model model, @PathVariable("id") String id, HttpSession session) {
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

    @GetMapping("/bulk_delete")
    public String bulkDelete(Model model, @RequestParam("ids") String ids, HttpSession session) {
        if (session == null)
            return "redirect:/login";
        var account = accounts.findBySessionId(session.getId());
        var redirect = performChecks(model, account);
        if (redirect != null)
            return redirect;
        return new WebPagePreparator(model, "generic/confirm")
                .setAttribute("action", "delete all selected Services")
                .setAttribute("actionConfirm", "/service/bulk_delete/confirm?ids=" + ids)
                .setAttribute("actionCancel", "/service")
                .userAccount(account.get())
                .complete();
    }

    @PostMapping("/bulk_delete")
    public String bulkDeleteConfirm(Model model, @RequestParam("ids") String ids, HttpSession session) {
        if (session == null)
            return "redirect:/login";
        var account = accounts.findBySessionId(session.getId());
        var redirect = performChecks(model, account);
        if (redirect != null)
            return redirect;
        Arrays.stream(ids.split(";"))
                .filter(Predicate.not(String::isBlank))
                .forEach(services::deleteById);
        return "redirect:/services";
    }

    private @Nullable String performChecks(Model model, Optional<UserAccount> account) {
        return performChecks(model, account, Optional.empty(), false);
    }

    private @Nullable String performChecks(Model model, Optional<UserAccount> account, Optional<AuthService> service) {
        return performChecks(model, account, service, true);
    }

    private @Nullable String performChecks(Model model, Optional<UserAccount> account, Optional<AuthService> service, boolean requiresService) {
        if (account.isEmpty())
            return "redirect:/login";
        if (!account.get().hasPermission(UserAccount.Permit.Services))
            return new WebPagePreparator(model, "generic/unauthorized")
                    .userAccount(account.get())
                    .complete();
        if (requiresService && service.isEmpty())
            return new WebPagePreparator(model, "service/not_found").complete();
        return null;
    }
}
