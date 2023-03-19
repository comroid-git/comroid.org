package org.comroid.auth.controller;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;
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
import org.springframework.web.servlet.ModelAndView;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Controller
@RequestMapping("/services")
@SuppressWarnings("OptionalGetWithoutIsPresent")
public class ServiceController {
    @Autowired
    private AccountRepository accounts;
    @Autowired
    private ServiceRepository services;

    @PostConstruct
    public void migrateDB() {
        services.migrateDB();
        StreamSupport.stream(services.findAll().spliterator(), false)
                .filter(it -> it.getSecret() == null)
                .peek(AuthService::regenerateSecret)
                .forEach(services::save);
    }

    @GetMapping
    @ResponseBody
    public ModelAndView index(Model model, HttpSession session) {
        if (session == null)
            return new ModelAndView("redirect:/login");
        var account = accounts.findBySessionId(session.getId());
        var redirect = performChecks(model, account);
        if (redirect != null)
            return redirect;
        return new WebPagePreparator(model, "service/list")
                .userAccount(account.get())
                .authServiceList(StreamSupport.stream(services.findAll().spliterator(), false).collect(Collectors.toList()))
                .complete();
    }

    @GetMapping("/add")
    @ResponseBody
    public ModelAndView add(Model model, HttpSession session) {
        if (session == null)
            return new ModelAndView("redirect:/login");
        var account = accounts.findBySessionId(session.getId());
        var redirect = performChecks(model, account);
        if (redirect != null)
            return redirect;
        return new WebPagePreparator(model, "service/add")
                .userAccount(account.get())
                .complete();
    }

    @PostMapping(value = "/add", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @ResponseBody
    public ModelAndView add(
            Model model,
            HttpSession session,
            @RequestParam("name") String name,
            @RequestParam("url") String url,
            @RequestParam("callbackUrl") String callbackUrl,
            @RequestParam("requiredScope") String requiredScope
    ) {
        if (session == null)
            return new ModelAndView("redirect:/login");
        var account = accounts.findBySessionId(session.getId());
        var redirect = performChecks(model, account);
        if (redirect != null)
            return redirect;
        services.save(new AuthService(name, url, callbackUrl, requiredScope));
        return new ModelAndView("redirect:/services");
    }

    @GetMapping("/{id}")
    @ResponseBody
    public ModelAndView view(Model model, @PathVariable("id") String id, HttpSession session) {
        if (session == null)
            return new ModelAndView("redirect:/login");
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
    @ResponseBody
    public ModelAndView edit(Model model, @PathVariable("id") String id, HttpSession session) {
        if (session == null)
            return new ModelAndView("redirect:/login");
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

    @PostMapping(value = "/{id}/edit", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @ResponseBody
    public ModelAndView edit(
            Model model,
            @PathVariable("id") String id,
            @RequestParam("name") String name,
            @RequestParam("url") String url,
            @RequestParam("callbackUrl") String callbackUrl,
            @RequestParam("requiredScope") String requiredScope,
            @RequestParam(value = "regenerateSecret", required = false, defaultValue = "false") boolean regenerateSecret,
            HttpSession session
    ) {
        if (session == null)
            return new ModelAndView("redirect:/login");
        var account = accounts.findBySessionId(session.getId());
        var service = services.findById(id);
        var redirect = performChecks(model, account, service);
        if (redirect != null)
            return redirect;
        var found = service.get();
        found.setName(name);
        found.setUrl(url);
        found.setCallbackUrl(callbackUrl);
        found.setRequiredScope(requiredScope);
        if (regenerateSecret)
            found.regenerateSecret();
        services.save(found);
        return new ModelAndView("redirect:/services");
    }

    @GetMapping("/{id}/delete")
    @ResponseBody
    public ModelAndView delete(Model model, @PathVariable("id") String id, HttpSession session) {
        if (session == null)
            return new ModelAndView("redirect:/login");
        var account = accounts.findBySessionId(session.getId());
        var service = services.findById(id);
        var redirect = performChecks(model, account, service);
        if (redirect != null)
            return redirect;
        services.delete(service.get());
        return new ModelAndView("redirect:/services");
    }

    @GetMapping("/bulk_delete")
    @ResponseBody
    public ModelAndView bulkDelete(Model model, @RequestParam("ids") String ids, HttpSession session) {
        if (session == null)
            return new ModelAndView("redirect:/login");
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
    @ResponseBody
    public ModelAndView bulkDeleteConfirm(Model model, @RequestParam("ids") String ids, HttpSession session) {
        if (session == null)
            return new ModelAndView("redirect:/login");
        var account = accounts.findBySessionId(session.getId());
        var redirect = performChecks(model, account);
        if (redirect != null)
            return redirect;
        Arrays.stream(ids.split(";"))
                .filter(Predicate.not(String::isBlank))
                .forEach(services::deleteById);
        return new ModelAndView("redirect:/services");
    }

    private @Nullable ModelAndView performChecks(Model model, Optional<UserAccount> account) {
        return performChecks(model, account, Optional.empty(), false);
    }

    private @Nullable ModelAndView performChecks(Model model, Optional<UserAccount> account, Optional<AuthService> service) {
        return performChecks(model, account, service, true);
    }

    private @Nullable ModelAndView performChecks(Model model, Optional<UserAccount> account, Optional<AuthService> service, boolean requiresService) {
        if (account.isEmpty())
            return new ModelAndView("redirect:/login");
        if (!account.get().hasPermission(UserAccount.Permit.AdminServices))
            return new WebPagePreparator(model, "generic/unauthorized")
                    .userAccount(account.get())
                    .complete();
        if (requiresService && service.isEmpty())
            return new WebPagePreparator(model, "service/not_found").complete();
        return null;
    }
}
