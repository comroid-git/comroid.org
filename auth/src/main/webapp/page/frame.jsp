<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%--@elvariable id="widget" type="boolean"--%>
<%--@elvariable id="loggedIn" type="boolean"--%>
<%--@elvariable id="page" type="java.lang.String"--%>
<html>
<head>
    <title>comroid Auth Server</title>
    <c:import url="/page/head.jsp" />
</head>
<body>
<header>
    <c:import url="/page/header.jsp" />
</header>
<div id="content">
    <c:choose>
        <c:when test="${loggedIn}">
            <c:import url="/${page}.jsp" />
        </c:when>
        <c:otherwise>
            <h3 class="error-text">You are not logged in</h3>
        </c:otherwise>
    </c:choose>
</div>
<footer>
    <c:import url="/page/footer.jsp" />
</footer>
</body>
</html>
