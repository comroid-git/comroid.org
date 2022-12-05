<%--suppress HtmlFormInputWithoutLabel --%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%--@elvariable id="widget" type="boolean"--%>
<%--@elvariable id="code" type="java.lang.String"--%>
<%--@elvariable id="message" type="java.lang.String"--%>
<%--@elvariable id="stacktrace" type="java.lang.String"--%>
<h3>Something went wrong :(</h3>
<h4 class="error-text">Error Code ${code}</h4>
<c:if test="${not empty stacktrace}">
    <button id="toggleException" onclick="toggleException()" style="width: 100%">Show Exception Details</button>
    <br/>
    <div id="exception" style="display: none;">
        <c:if test="${not empty message}">
            <span>${message}</span>
        </c:if>
        <textarea rows="999" style="white-space: pre-wrap; line-break: auto; width: 100%; height: 35%; resize: vertical;" readonly>${stacktrace}</textarea>
    </div>
    <br/>
</c:if>
<button onclick="history.back()">Go Back</button>
<script type="application/javascript">
    let shown = false;

    function toggleException() {
        let div = document.getElementById('exception');
        let btn = document.getElementById('toggleException');
        if (shown) {
            div.style.display = 'none';
            btn.textContent = "Show Exception Details";
            shown = false;
        } else {
            div.style.display = 'block';
            btn.textContent = "Hide Exception Details";
            shown = true;
        }
    }
</script>
