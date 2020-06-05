/**
 * Inits the grid of the status page, performs the JSON fetch.
 */
function initGrid() {
    fetch('https://api.status.comroid.org/services')
		.then(handleErrors)
		.then(response => response.json())
        .then(data => createBoxes(data))
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
 */
function createBoxes(data) {
    for (var i = 0; i < data.length; i++) {
        var obj = data[i];
        addBox(obj.name, obj.display_name, obj.status);
    }
}

/**
 * Creates a new div inside the flex container.
 *
 * @param {string} name The service name parsed from JSON data
 * @param {string} display_name The display name parsed from JSON data
 * @param {string} status The current status of the service
 */
function addBox(name, display_name, status) {
    // Create a new box in the status grid
    var newDiv = document.createElement("div");
    if (name == "status-server") {
        newDiv.id = "head_wrapper";
    } else {
        newDiv.className = "cell";
    }

    // Add text and status div depending on previous JSON parsing
    var statusDiv = document.createElement("div");
    var statusText = document.createElement("p");
    statusText.appendChild(document.createTextNode(display_name + ":"));

    if (status == "1") {
        statusDiv.className = "offline";
        statusDiv.innerHTML = "OFFLINE";
    } else if (status == "2") {
        statusDiv.className = "maintenance";
        statusDiv.innerHTML = "MAINTENANCE";
    } else if (status == "3") {
        statusDiv.className = "busy";
        statusDiv.innerHTML = "REPORTED PROBLEMS";
    } else if (status == "4") {
        statusDiv.className = "online";
        statusDiv.innerHTML = "ONLINE";
    } else {
        statusDiv.innerHTML = "UNKNOWN";
    }


    // Append text and status div to the box
    newDiv.appendChild(statusText);
    newDiv.appendChild(statusDiv);

    // Add the box to the flex container
    // If the box is for the status server, check if there were already boxes created
    // If true, create the status server box before the first child node
    var container = document.getElementById('flex_container');
    if (name == "status-server" && container.hasChildNodes()) {
        var child = container.childNodes[1];
        container.insertBefore(newDiv, child);
    } else {
        document.getElementById('flex_container').appendChild(newDiv);
    }
}
