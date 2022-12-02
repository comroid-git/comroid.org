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
<c:if test="!widget">
    <a href="<c:url value="/account/change_password" />">Change Password</a>
</c:if>