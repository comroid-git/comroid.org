package org.comroid.auth.web;

import org.comroid.auth.entity.AuthService;
import org.comroid.auth.entity.UserAccount;
import org.comroid.auth.dto.RegisterData;
import org.springframework.ui.Model;

public class WebPagePreparator {
    private final Model model;
    private final String page;
    private boolean widget;

    public WebPagePreparator(Model model, String page) {
        this.model = model;
        this.page = page;
    }

    public WebPagePreparator userAccount(UserAccount account) {
        setAttribute("account", account);
        setAttribute("admin", account.hasPermission(UserAccount.Permit.Admin));
        return this;
    }

    public WebPagePreparator authService(AuthService service) {
        return setAttribute("service", service);
    }

    public WebPagePreparator registerData(RegisterData data) {
        return setAttribute("registerData", data);
    }

    public WebPagePreparator setAttribute(String name, Object value) {
        model.addAttribute(name, value);
        return this;
    }

    public WebPagePreparator setWidget(boolean widget) {
        this.widget = widget;
        return this;
    }

    public String complete() {
        setAttribute("page", page);
        setAttribute("widget", widget);
        setAttribute("loggedIn", model.getAttribute("account") != null);
        return "page/frame";
    }
}
