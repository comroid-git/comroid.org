<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%--@elvariable id="widget" type="boolean"--%>
<%--@elvariable id="username" type="java.lang.String"--%>
<h3>Login to your Account</h3>
<form id="login" method="post" action="<c:url value="/login" />">
    <c:if test="${not empty username}">
        <h4 class="error-text">Unable to log in. Please check your credentials.</h4>
    </c:if>
    <label for="username">Username:</label>
    <input type="text" id="username" name="username" placeholder="Username" value="${username}" />
    <br/>
    <label for="password">Password:</label>
    <input type="password" name="password" id="password" placeholder="Password" />
    <br/>
    <input type="submit" value="Confirm" />
</form>
