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

/**
 * Inits the grid of the status page, performs the JSON fetch.
 *
 * @param {boolean} slim Whether to print the Status Server seperately
 */
function initGrid(slim) {
    timeout(5000, fetch('https://api.status.comroid.org/services'))
        .then(handleErrors)
        .then(response => response.json())
        .then(data => createBoxes(data, slim))
        .catch(error => {
            addBox(false, "status-server", "Status Server", 0);
            console.error('There has been a problem with your fetch operation:', error);
        });
}

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

/**
 * Creates all boxes for the status page grid.
 *
 * @param {Object} data The JSON data delivered by the Fetch API request
 * @param {boolean} slim Whether to print the Status Server separately
 */
function createBoxes(data, slim) {
    for (let i = 0; i < data.length; i++) {
        const obj = data[i];
        // noinspection JSUnresolvedVariable
        addBox(slim, obj.name, obj.display_name, obj.status);
    }
}

/**
 * Creates a new div inside the flex container.
 *
 * @param {boolean} slim Whether to print the Status Server seperately
 * @param {string} name The service name parsed from JSON data
 * @param {string} display_name The display name parsed from JSON data
 * @param {int} status The current status of the service
 */
function addBox(slim, name, display_name, status) {
    // Create a new box in the status grid
    const newDiv = document.createElement("div");
    if (slim && name === "status-server") {
        newDiv.id = "head_wrapper";
    } else {
        newDiv.className = "cell";
    }

    // Add text and status div depending on previous JSON parsing
    const statusDiv = document.createElement("div");
    const statusText = document.createElement("p");
    statusText.appendChild(document.createTextNode(display_name + ":"));

    const enm = statusArray[status];

    statusDiv.className = enm['classname'];
    statusDiv.innerHTML = enm['display'];


    // Append text and status div to the box
    newDiv.appendChild(statusText);
    newDiv.appendChild(statusDiv);

    // Add the box to the flex container
    // If the box is for the status server, check if there were already boxes created
    // If true, create the status server box before the first child node
    const container = document.getElementById('flex_container');
    if (name === "status-server" && container.hasChildNodes()) {
        let child = container.childNodes[1];
        container.insertBefore(newDiv, child);
    } else {
        document.getElementById('flex_container').appendChild(newDiv);
    }
}
