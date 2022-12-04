<%--suppress HtmlFormInputWithoutLabel --%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%--@elvariable id="widget" type="boolean"--%>
<%--@elvariable id="loggedIn" type="boolean"--%>
<%--@elvariable id="account" type="org.comroid.auth.entity.UserAccount"--%>
<%--@elvariable id="services" type="java.util.List<org.comroid.auth.entity.AuthService>"--%>
<h3>Services</h3>
<table>
    <tr>
        <th><input type="checkbox" id="selectAll" onclick="selectAll()"></th>
        <th>ID</th>
        <th>Name</th>
        <th><input type="button" value="Add"></th>
        <th><!-- row for edit --></th>
        <th><input type="button" value="Delete" id="deleteSelection" onclick="deleteSelection()" disabled></th>
    </tr>
    <c:forEach items="${services}" var="service">
        <tr>
            <td><input type="checkbox" id="select${service.UUID}" onclick="select(${service.UUID})"></td>
            <td>${service.UUID}</td>
            <td>${service.name}</td>
            <td>
                <input type="button" value="Details" onclick="gotoDetails(${service.UUID})">
                <input type="button" value="Edit" onclick="gotoEdit(${service.UUID})">
                <input type="button" value="Delete" onclick="gotoDelete(${service.UUID})">
            </td>
        </tr>
    </c:forEach>
</table>
<%--suppress JSPrimitiveTypeWrapperUsage --%>
<script type="application/javascript">
    const baseUrl = window.location.protocol + "//" + window.location.hostname;
    let bulkSelection = false;
    let selected = [];

    function selectAll() {
        for (let input in document.getElementsByClassName('input')) {
            if (input.type !== 'checkbox')
                continue;
            if (!input.id.startsWith('select'))
                continue;
            input.checked = document.getElementById(input.id).checked;
        }
        bulkSelection = true;
    }

    function select(uuid) {
        let box = document.getElementById('select' + uuid);
        let state = !selected.contains(uuid);

        if (state) {
            box.checked = true;
            selected.push(uuid);
        } else {
            box.checked = false;
            selected.remove(uuid);
        }
        if (bulkSelection)
            document.getElementById('selectAll').indeterminate = true;
    }

    function deleteSelection() {
        let query = '?ids=';
        for (let id of selected)
            query += id + ';';
        window.location.assign(baseUrl + "/service/bulk_delete" + query);
    }

    function gotoDetails(uuid) {
        window.location.assign(baseUrl + "/service/" + uuid);
    }

    function gotoEdit(uuid) {
        window.location.assign(baseUrl + "/service/" + uuid + "/edit");
    }

    function gotoDelete(uuid) {
        window.location.assign(baseUrl + "/service/" + uuid + "/delete");
    }
</script>
