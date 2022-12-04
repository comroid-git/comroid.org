<%--suppress CheckEmptyScriptTag --%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%--@elvariable id="widget" type="boolean"--%>
<%--@elvariable id="loggedIn" type="boolean"--%>
<%--@elvariable id="account" type="org.comroid.auth.entity.UserAccount"--%>
<%--@elvariable id="action" type="java.lang.String"--%>
<%--@elvariable id="actionConfirm" type="java.lang.String"--%>
<%--@elvariable id="actionCancel" type="java.lang.String"--%>
<h3>Do you really want to ${action}?</h3>
<h4 class="error-text">Warning: This is a potentially destructive action!</h4>
<form method="post" action="<c:url value="${actionConfirm}" />">
  <input type="submit" value="Yes">
  <input type="button" value="No" onclick="window.location.assign(<c:url value="${actionCancel}" />)">
</form>
<button value="Yes" onclick="confirm()" />
<button value="No" onclick="cancel()" />
