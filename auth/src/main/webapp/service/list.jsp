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
<input type="button" value="Add" onclick="gotoAdd()">
<table>
    <tr>
        <th style="width: 1px;"><input type="checkbox" id="selectAll" onclick="selectAll()"></th>
        <th>Name</th>
        <th style="width: 1px;"><!-- row for details --></th>
        <th style="width: 1px;"><!-- row for edit --></th>
        <th style="width: 1px;"><input type="button" value="Delete" id="deleteSelection" onclick="deleteSelection()" disabled></th>
    </tr>
    <c:forEach items="${services}" var="service">
        <tr>
            <td><input type="checkbox" id="select${service.UUID}" onclick="select(${service.UUID})"></td>
            <td>${service.name}</td>
            <td><input type="button" value="Details" onclick="gotoDetails(${service.UUID})"></td>
            <td><input type="button" value="Edit" onclick="gotoEdit(${service.UUID})"></td>
            <td><input type="button" value="Delete" onclick="gotoDelete(${service.UUID})"></td>
        </tr>
    </c:forEach>
</table>
<%--suppress JSPrimitiveTypeWrapperUsage --%>
<script type="application/javascript">
    let baseUrl = window.location.protocol + "//" + window.location.hostname;
    if (location.port !== '')
        baseUrl += ':' + location.port;
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
        window.location.assign(baseUrl + "/services/bulk_delete" + query);
    }

    function gotoAdd() {
        window.location.assign(baseUrl + "/services/add");
    }

    function gotoDetails(uuid) {
        window.location.assign(baseUrl + "/services/" + uuid);
    }

    function gotoEdit(uuid) {
        window.location.assign(baseUrl + "/services/" + uuid + "/edit");
    }

    function gotoDelete(uuid) {
        window.location.assign(baseUrl + "/services/" + uuid + "/delete");
    }
</script>
