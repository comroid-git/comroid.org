function loadServices() {
    console.log('ja lol ey')

    for (let service in services.entries()) {
        console.log('service: '+service)

        serviceAvailability(service)
    }
}

async function serviceAvailability(service) {
    console.log('processing: '+service)

    const stat = await requestState(service.url)

    document.getElementById('service_' + service.id + '_status')
        .innerText = stat.string;
}

async function requestState(service) {
    const started = Date.now();
    console.log('fetching')
    const response = await fetch(service.url, {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json'
        }
    });
    const finished = Date.now();
    const delta = (finished - started);
    console.log('delta'+delta)

    let val = undefined;
    for (let it in state.entries()) {
        // noinspection JSUnfilteredForInLoop
        if (it.timeout < delta)
            val = it;
    }

    return val;
}
