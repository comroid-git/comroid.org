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
            document.getElementById('content').innerHTML = "<p>You are not logged in</p>";
            return;
        }
    } catch {
        document.getElementById('content').innerHTML = "<p>Unable to fetch session data</p>";
        return;
    }

    document.getElementById('content')
        .querySelectorAll('.inject')
        .forEach(e => {
            let vname = e.classList[1].split('.')
            populateTag(sessionData, e, vname, 0)
        })
}
