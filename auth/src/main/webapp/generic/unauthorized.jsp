<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%--@elvariable id="message" type="java.lang.String"--%>
<c:choose>
    <c:when test="${not empty message}">
        <h3 class="error-text">${message}</h3>
    </c:when>
    <c:otherwise>
        <h3 class="error-text">You are not authorized to view this page</h3>
    </c:otherwise>
</c:choose>
