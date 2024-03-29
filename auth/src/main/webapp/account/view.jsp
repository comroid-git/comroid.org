<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%--@elvariable id="widget" type="boolean"--%>
<%--@elvariable id="loggedIn" type="boolean"--%>
<%--@elvariable id="account" type="org.comroid.auth.entity.UserAccount"--%>
<p> Account ID:
    <span>${account.id}</span>
</p>
<p> Username:
    <span>${account.username}</span>
</p>
<p> E-Mail:
    <span>${account.email}</span>
</p>
<c:if test="${not widget}">
    <c:if test="${not account.emailVerified}">
        <a href="<c:url value="/account/email_verification" />" style="text-decoration: underline">Verify E-Mail Address</a>
        <br/>
    </c:if>
    <a href="<c:url value="/account/edit" />" style="text-decoration: underline">Edit Account</a>
    <br/>
    <a href="<c:url value="/account/start_change_password" />" style="text-decoration: underline">Change Password</a>
</c:if>