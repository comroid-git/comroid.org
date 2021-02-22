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
