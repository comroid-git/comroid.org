<%--suppress HtmlFormInputWithoutLabel --%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%--@elvariable id="widget" type="boolean"--%>
<%--@elvariable id="loggedIn" type="boolean"--%>
<%--@elvariable id="account" type="org.comroid.auth.entity.UserAccount"--%>
<%--@elvariable id="service" type="org.comroid.auth.entity.AuthService"--%>
<h3>Editing Service ${service.name}</h3>
<form method="post" action="<c:url value="/services/${service.id}/edit" />">
    <table>
        <tr>
            <td>ID</td>
            <td>${service.id}</td>
        </tr>
        <tr>
            <td>Name</td>
            <td><input type="text" id="name" name="name" value="${service.name}" style="width: 100%"></td>
        </tr>
        <tr>
            <td>URL</td>
            <td><input type="text" id="url" name="url" value="${service.url}" style="width: 100%"></td>
        </tr>
        <tr>
            <td>Callback URL</td>
            <td><input type="text" id="callbackUrl" name="callbackUrl" value="${service.callbackUrl}" style="width: 100%"></td>
        </tr>
        <tr>
            <td>Required Scope for Access</td>
            <td><input type="text" id="requiredScope" name="requiredScope" value="${service.requiredScope}" style="width: 100%"></td>
        </tr>
        <tr>
            <td>Secret</td>
            <td><textarea style="width: 100%; resize: none;" rows="1" readonly>${service.clientSecret}</textarea></td>
        </tr>
        <tr>
            <td>Regenerate Secret</td>
            <td><input type="checkbox" id="regenerateSecret" name="regenerateSecret" style="width: 100%"></td>
        </tr>
    </table>
    <input type="submit" value="Save Changes" />
</form>
