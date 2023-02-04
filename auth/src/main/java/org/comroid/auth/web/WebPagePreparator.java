package org.comroid.auth.web;

import org.comroid.auth.dto.RegisterData;
import org.comroid.auth.entity.AuthService;
import org.comroid.auth.entity.UserAccount;
import org.jetbrains.annotations.Nullable;
import org.springframework.ui.Model;

import java.util.List;

public class WebPagePreparator {
    private final Model model;
    private final String page;
    private boolean widget;
    private boolean needLogin;
    private String frame = "page/frame";

    public WebPagePreparator setWidget(boolean widget) {
        this.widget = widget;
        return this;
    }

    public WebPagePreparator(Model model, String page) {
        this.model = model;
        this.page = page;
    }

    public WebPagePreparator frame(String frame) {
        this.frame = frame;
        return this;
    }

    public WebPagePreparator userAccount(@Nullable UserAccount account) {
        setAttribute("account", account);
        setAttribute("hubAccess", account != null && account.hasPermission(UserAccount.Permit.Hub));
        setAttribute("serviceAdmin", account != null && account.hasPermission(UserAccount.Permit.AdminServices));
        setAttribute("accountAdmin", account != null && account.hasPermission(UserAccount.Permit.AdminAccounts));
        return this;
    }

    public WebPagePreparator userAccountList(List<UserAccount> accounts) {
        return setAttribute("accounts", accounts);
    }

    public WebPagePreparator authService(AuthService service) {
        return setAttribute("service", service);
    }

    public WebPagePreparator authServiceList(List<AuthService> services) {
        return setAttribute("services", services);
    }

    public WebPagePreparator registerData(RegisterData data) {
        return setAttribute("registerData", data);
    }

    public WebPagePreparator setAttribute(String name, Object value) {
        model.addAttribute(name, value);
        return this;
    }

    public WebPagePreparator needLogin(boolean needLogin) {
        this.needLogin = needLogin;
        return this;
    }

    public String complete() {
        setAttribute("page", page);
        setAttribute("widget", widget);
        setAttribute("needLogin", needLogin);
        setAttribute("loggedIn", model.getAttribute("account") != null);
        return frame;
    }
}
