const loginPanel = `<iframe src="login"></iframe>`;

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
