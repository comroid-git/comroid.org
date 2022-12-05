<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%--@elvariable id="widget" type="boolean"--%>
<%--@elvariable id="loggedIn" type="boolean"--%>
<%--@elvariable id="account" type="org.comroid.auth.entity.UserAccount"--%>
<%--@elvariable id="code" type="java.lang.String"--%>
<h3>Change your Password</h3>
<form id="update-password" method="post" action="<c:url value="/account/change_password" />">
    <input type="hidden" name="code" value="${code}">
    <label for="password">New password:</label>
    <input type="password" id="password" name="password">
    <br/>
    <label for="confirm-password">Confirm new password:</label>
    <input type="password" id="confirm-password" name="confirm-password">
    <br/>
    <input type="button" value="Update Password" onclick="validateForm()">
</form>
<script type="application/javascript">
    function validatePasswordsMatch() {
        if (document.getElementById('password').value !== document.getElementById('confirm-password').value) {
            alert("Passwords do not match");
            return false;
        }
        return true;
    }

    function validateForm() {
        if (validatePasswordsMatch())
            document.getElementById('update-password').submit();
    }
</script>
