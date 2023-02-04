<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%--@elvariable id="widget" type="boolean"--%>
<%--@elvariable id="email" type="java.lang.String"--%>
<%--@elvariable id="action" type="java.lang.String"--%>
<%--@elvariable id="pending" type="java.lang.String"--%>
<h3>Login to your Account</h3>
<form id="login" method="post" action="<c:choose><c:when test="${empty action}"><c:url value="/login" /></c:when><c:otherwise><c:url value="${action}" /></c:otherwise></c:choose>">
    <c:if test="${not empty email}">
        <h4 class="error-text">Unable to log in. Please check your credentials.</h4>
    </c:if>
    <label for="email">Email:</label>
    <input type="text" id="email" name="email" placeholder="E-Mail" value="${email}" />
    <br/>
    <label for="password">Password:</label>
    <input type="password" name="password" id="password" placeholder="Password" />
    <br/>
    <input type="hidden" name="pending" value="${pending}" />
    <input type="submit" value="Confirm" />
</form>
