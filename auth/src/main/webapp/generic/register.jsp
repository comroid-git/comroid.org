<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%--@elvariable id="widget" type="boolean"--%>
<%--@elvariable id="registerData" type="org.comroid.auth.dto.RegisterData"--%>
<%--suppress RegExpRedundantEscape --%>
<h3>Register for an Account</h3>
<form id="registration" method="post" action="<c:url value="/register" />>">
    <c:if test="${registerData.invalidUsername}">
        <h4 class="error-text">Username already taken</h4>
    </c:if>
    <label for="username">Username:</label>
    <input type="text" id="username" placeholder="Username" value="${registerData.username}" />
    <br/>
    <br/>
    <c:if test="${registerData.invalidEmail}">
        <h4 class="error-text">E-Mail Address already in use</h4>
    </c:if>
    <label for="email">E-Mail Address:</label>
    <input type="email" id="email" placeholder="E-Mail Address" value="${registerData.email}" />
    <br/>
    <br/>
    <label for="password">Password:</label>
    <input type="password" id="password" placeholder="Password" />
    <br/>
    <label for="confirm-password">Confirm Password:</label>
    <input type="password" id="confirm-password" placeholder="Password" />
    <br/>
    <br/>
    <label for="tos">I agree to the <a href="https://comroid.org/#tos">Terms of Service</a></label>
    <input type="checkbox" id="tos" />
    <br/>
    <label for="dpp">I agree to the <a href="https://comroid.org/#privacy">Data Protection Policy</a></label>
    <input type="checkbox" id="dpp" />
    <br/>
    <br/>
    <input type="button" value="Confirm" onclick="validateForm()" />
</form>
<script type="application/javascript">
    function validateEmail() {
        if (!/^\w+([\.-]?\w+)*@\w+([\.-]?\w+)*(\.\w{2,3})+$/.test(document.getElementById('email').value)) {
            alert("You have entered an invalid E-Mail Address");
            return false;
        }
        return true;
    }

    function validatePasswordsMatch() {
        if (document.getElementById('password').value !== document.getElementById('confirm-password').value) {
            alert("Passwords do not match");
            return false;
        }
        return true;
    }

    function validateAgreement() {
        if (!document.getElementById('tos').checked) {
            alert("You have not agreed to the Terms of Service");
            return false;
        }
        if (!document.getElementById('dpp').checked) {
            alert("You have not agreed to the Data Protection Policy");
            return false;
        }
        return true;
    }

    function validateForm() {
        if (validateEmail() && validatePasswordsMatch() && validateAgreement())
            document.getElementById('registration').submit();
    }
</script>
