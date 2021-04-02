const loginPanel = `<iframe src="login"></iframe>`;
const registerPanel = `<iframe src="register"></iframe>`;

function populateTag(data, tag, names, index) {
    if (names.length - 1 > index)
        return populateTag(data[names[index]], tag, names, index + 1)
    tag.innerText = data[names[index]]
}

function initData() {
    try {
        console.debug("sessionData = ", sessionData);
    } catch {
        var sessionData = undefined;
    }

    const content = document.getElementById('content');
    const sessionNav = document.getElementById('sessionNav');

    if (sessionData === undefined) {
        content.innerHTML = loginPanel;
        sessionNav.innerHTML = `<a onclick="content = loginPanel">Login</a> | <a onclick="content = registerPanel">Register</a>`;
    } else {
        content.innerHTML = loginPanel;
        sessionNav.innerHTML = `<a href="logout">Logout</a>`;
    }

    content
        .querySelectorAll('.inject')
        .forEach(e => {
            let vname = e.classList[1].split('.')
            populateTag(sessionData, e, vname, 0)
        })
}
