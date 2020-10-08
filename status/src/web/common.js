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
 * Inits the grid of the status page, performs the JSON fetch.
 *
 * @param {bool} slim Whether to print the Status Server seperately
 */
function initGrid(slim) {
    fetch('https://api.status.comroid.org/services')
        .then(handleErrors)
        .then(response => response.json())
        .then(data => createBoxes(data, slim))
        .catch(error => {
            addBox("status-server", "Status Server", "unknown");
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
 * @param {bool} slim Whether to print the Status Server seperately
 */
function createBoxes(data, slim) {
    for (var i = 0; i < data.length; i++) {
        var obj = data[i];
        addBox(slim, obj.name, obj.display_name, obj.status);
    }
}

/**
 * Creates a new div inside the flex container.
 *
 * @param {bool} slim Whether to print the Status Server seperately
 * @param {string} name The service name parsed from JSON data
 * @param {string} display_name The display name parsed from JSON data
 * @param {string} status The current status of the service
 */
function addBox(slim, name, display_name, status) {
    // Create a new box in the status grid
    var newDiv = document.createElement("div");
    if (slim && name === "status-server") {
        newDiv.id = "head_wrapper";
    } else {
        newDiv.className = "cell";
    }

    // Add text and status div depending on previous JSON parsing
    var statusDiv = document.createElement("div");
    var statusText = document.createElement("p");
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
    var container = document.getElementById('flex_container');
    if (name === "status-server" && container.hasChildNodes()) {
        var child = container.childNodes[1];
        container.insertBefore(newDiv, child);
    } else {
        document.getElementById('flex_container').appendChild(newDiv);
    }
}
