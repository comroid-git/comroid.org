<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%--@elvariable id="widget" type="boolean"--%>
<%--@elvariable id="loggedIn" type="boolean"--%>
<%--@elvariable id="hubAccess" type="boolean"--%>
<%--@elvariable id="admin" type="boolean"--%>
<%--@elvariable id="serviceAdmin" type="boolean"--%>
<%--@elvariable id="account" type="org.comroid.auth.entity.UserAccount"--%>
<h1><a href="https://comroid.org">comroid</a></h1>
<h2><a href="<c:url value="/account" />">Manage your comroid Account</a></h2>
<c:choose>
    <c:when test="${loggedIn}">
        <c:if test="${hubAccess}">
            <a href="https://hub.comroid.org">Hub</a> |
        </c:if>
        <c:if test="${serviceAdmin}">
            <a href="<c:url value="/service" />">Services</a> |
        </c:if>
        <c:if test="${admin}">
            <a href="<c:url value="/admin" />">Admin</a> |
        </c:if>
        <a href="<c:url value="/logout" />">Logout</a>
    </c:when>
    <c:otherwise>
        <a href="<c:url value="/login" />">Login</a> | <a href="<c:url value="/register" />">Register</a>
    </c:otherwise>
</c:choose>
