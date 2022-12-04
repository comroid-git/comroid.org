<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%--@elvariable id="widget" type="boolean"--%>
<%--@elvariable id="loggedIn" type="boolean"--%>
<%--@elvariable id="account" type="org.comroid.auth.entity.UserAccount"--%>
<%--@elvariable id="service" type="org.comroid.auth.entity.AuthService"--%>
<h3>Add a Service</h3>
<form method="post" action="<c:url value="/services/add" />">
    <label for="name">Service Name</label>
    <input type="text" id="name" name="name">
    <br/>
    <label for="url">Service Homepage URL</label>
    <input type="text" id="url" name="url">
    <br/>
    <label for="callbackUrl">Service Callback URL</label>
    <input type="text" id="callbackUrl" name="callbackUrl">
    <br/>
    <label for="requiredScope">Required Scope for Access</label>
    <input type="text" id="requiredScope" name="requiredScope">
    <br/>
    <input type="submit" value="Save Service">
</form>