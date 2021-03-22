const loginPanel = `
    <div id="login_panel">
        <h2>Login to your comroid Account</h2>
        <form action="./login" method="post">
            <label for="emailInput">E-Mail</label>
            <input id="emailInput" name="email" title="E-Mail" type="text">
            <br/>
            <label for="passwordInput">Password</label>
            <input id="passwordInput" name="password" title="Password" type="password">
            <br/>
            <button type="submit">Login now</button>
        </form>
    </div>`;

function handleErrors(response) {
    if (!response.ok) {
        throw Error(response.statusText);
    }
    return response;
}

function timeout(ms, promise) {
    return new Promise(function (resolve, reject) {
        setTimeout(function () {
            reject(new Error("timeout"))
        }, ms)
        promise.then(resolve, reject)
    })
}

function populateTag(data, tag, names, index) {
    if (names.length - 1 > index)
        return populateTag(data[names[index]], tag, names, index + 1)
    tag.innerText = data[names[index]]
}

function initData() {
    try {
        if (sessionData === undefined) {
            document.getElementById('content').innerHTML = loginPanel;
            return;
        }
    } catch {
        document.getElementById('content').innerHTML = loginPanel;
        return;
    }

    document.getElementById('content')
        .querySelectorAll('.inject')
        .forEach(e => {
            let vname = e.classList[1].split('.')
            populateTag(sessionData, e, vname, 0)
        })
}
