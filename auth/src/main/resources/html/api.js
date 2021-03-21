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

function importSessionData() {
    return fetch('session_data/' + document.cookie)
        .then(handleErrors)
        .then(response => {
            alert('got session data: ' + response);
            return response;
        })
        .then(response => response.json())
        .catch(error => console.error('Could not retrieve session data', error));
}

alert('cookie: '+document.cookie)
const dataPromise = importSessionData();
