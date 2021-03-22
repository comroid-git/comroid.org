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

function initData() {
    if (sessionData === undefined) {
        document.getElementById('content').innerHTML = "<p>You are not logged in</p>";
        return;
    }

    document.getElementById('data_id').innerText = sessionData['account']['uuid'];
    document.getElementById('data_email').innerText = sessionData['account']['email'];
}
