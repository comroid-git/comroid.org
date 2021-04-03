let loginPanel = `<iframe src="login"></iframe>`;
let registerPanel = `<iframe src="register"></iframe>`;
let isLoggedIn = false;

function populateTag(data, tag, names, index) {
    if (names.length - 1 > index)
        return populateTag(data[names[index]], tag, names, index + 1)
    tag.innerText = data[names[index]]
}

function content() {
    return document.getElementById('content');
}

function sessionNav() {
    return document.getElementById('sessionNav');
}

function initData() {
    try {
        console.debug("auth - sessionData = ", sessionData);
    } catch {
        sessionData = undefined;
    }

    const isWidget = sessionNav() === null;

    if (sessionData === undefined) {
        console.log("auth - Invalid Session; loading login Panel")
        content().innerHTML = loginPanel;

        if (!isWidget)
            sessionNav().innerHTML = `<a onclick="content().innerHTML = loginPanel">Login</a> | <a onclick="content().innerHTML = registerPanel">Register</a>`;
        return;
    } else {
        console.log("auth - Session found; ")
        isLoggedIn = true;

        if (!isWidget)
            sessionNav().innerHTML = `<a href="logout">Logout</a>`;
    }

    content()
        .querySelectorAll('.inject')
        .forEach(e => {
            let vname = e.classList[1].split('.')
            populateTag(sessionData, e, vname, 0)
        })
}
