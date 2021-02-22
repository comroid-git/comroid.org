const statusArray = [
    {
        'classname': 'offline',
        'display': 'UNREACHABLE'
    },
    {
        'classname': 'offline',
        'display': 'OFFLINE'
    },
    {
        'classname': 'offline',
        'display': 'CRASHED'
    },
    {
        'classname': 'maintenance',
        'display': 'MAINTENANCE'
    },
    {
        'classname': 'busy',
        'display': 'NOT_RESPONDING'
    },
    {
        'classname': 'online',
        'display': 'ONLINE'
    }
]

/**
 * Function to handle failed HTTP responses.
 *
 * @param {Object} response The response object of the fetch API
 */
function handleErrors(response) {
    if (!response.ok) {
        throw Error(response.statusText);
    }
    return response;
}

// taken from https://stackoverflow.com/a/46946573
// Rough implementation. Untested.
function timeout(ms, promise) {
    return new Promise(function(resolve, reject) {
        setTimeout(function() {
            reject(new Error("timeout"))
        }, ms)
        promise.then(resolve, reject)
    })
}
