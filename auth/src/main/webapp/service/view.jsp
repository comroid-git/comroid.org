<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%--@elvariable id="widget" type="boolean"--%>
<%--@elvariable id="loggedIn" type="boolean"--%>
<%--@elvariable id="account" type="org.comroid.auth.entity.UserAccount"--%>
<%--@elvariable id="service" type="org.comroid.auth.entity.AuthService"--%>
<h3>Service ${service.name}</h3>
<table>
    <tr>
        <td>ID</td>
        <td>${service.id}</td>
    </tr>
    <tr>
        <td>Name</td>
        <td>${service.name}</td>
    </tr>
    <tr>
        <td>URL</td>
        <td><a href="${service.url}">${service.url}</a></td>
    </tr>
    <tr>
        <td>Callback URL</td>
        <td>${service.callbackUrl}</td>
    </tr>
    <tr>
        <td>Required Scope for Access</td>
        <td>${service.requiredScope}</td>
    </tr>
</table>