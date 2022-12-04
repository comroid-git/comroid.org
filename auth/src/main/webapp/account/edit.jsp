<%--suppress HtmlFormInputWithoutLabel --%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%--@elvariable id="widget" type="boolean"--%>
<%--@elvariable id="loggedIn" type="boolean"--%>
<%--@elvariable id="account" type="org.comroid.auth.entity.UserAccount"--%>
<%--@elvariable id="editing" type="org.comroid.auth.entity.UserAccount"--%>
<%--@elvariable id="self" type="boolean"--%>
<h3>Editing Account ${editing.id}</h3>
<form method="post" action="<c:url value="/account/${editing.id}/edit" />">
    <table>
        <tr>
            <td>ID</td>
            <td>${editing.id}</td>
        </tr>
        <tr>
            <td>Username</td>
            <td><input type="text" id="username" name="username" value="${editing.username}" style="width: 100%"></td>
        </tr>
        <tr>
            <td>E-Mail</td>
            <td><input type="text" id="email" name="email" value="${editing.email}" style="width: 100%"></td>
        </tr>
        <c:if test="${not self}">
            <tr>
                <td>Permit</td>
                <td><input type="number" id="permit" name="permit" value="${editing.permit}" min="0" max="255" style="width: 100%"></td>
            </tr>
            <tr>
                <td>Enabled</td>
                <td><input type="checkbox" id="enabled" name="enabled" <c:if test="${editing.enabled}">checked</c:if> style="width: 100%"></td>
            </tr>
            <tr>
                <td>Locked</td>
                <td><input type="checkbox" id="locked" name="locked" <c:if test="${not editing.accountNonLocked}">checked</c:if> style="width: 100%"></td>
            </tr>
            <tr>
                <td>Expired</td>
                <td><input type="checkbox" id="expired" name="expired" <c:if test="${not editing.accountNonExpired}">checked</c:if> style="width: 100%"></td>
            </tr>
            <tr>
                <td>Credentials Expired</td>
                <td><input type="checkbox" id="credentialsExpired" name="credentialsExpired" <c:if test="${not editing.credentialsNonExpired}">checked</c:if> style="width: 100%"></td>
            </tr>
        </c:if>
    </table>
    <input type="submit" value="Save Changes" />
</form>
