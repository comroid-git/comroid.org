/**
 * Inits the grid of the status page, should contain all calls of addBox
 */
function initGrid() {
	var display_name = "Cobalton";
	var status = "offline";
	addBox("Status Server", status);
	for (i = 0; i < 9; i++) {
		addBox(display_name, status);
	}
}

/**
 * Creates a new div inside the flex container.
 *
 * @param {string} display_name The display name parsed of the service
 * @param {string} status The current status of the service
 */
function addBox(display_name, status) {
	// Create a new box in the status grid
	var newDiv = document.createElement("div");
	if (display_name == "Status Server") {
		newDiv.id = "head_wrapper";
	} else {
		newDiv.className = "cell";
	}
	
	// Add text and status div depending on previous JSON parsing
	var statusDiv = document.createElement("div");
	var statusText = document.createElement("p");	
	statusText.appendChild(document.createTextNode(display_name + ":"));
	statusDiv.className = status;
	statusDiv.innerHTML = status.toUpperCase();
	
	// Append text and status div to the box
	newDiv.appendChild(statusText);
	newDiv.appendChild(statusDiv);
	
	// Add the box to the flex container
	document.getElementById('flex_container').appendChild(newDiv);
}
