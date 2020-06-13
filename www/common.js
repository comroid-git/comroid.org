const urlBase = 'https://comroid.org/'

function initContent() {
    const pages = resolveTarget(window.location.hash);

    if (pages.length < 1) {
        console.error("No pages could be found")
        return;
    }

    const wrapper = generateContentWrapper(document.getElementById('content'), pages);

    pages.forEach(function (page) {
        const content = resolveContent(page);
        insertContent(wrapper, content, page.name);
    });
}

// content insertion
function insertContent(parent, content, pageName) {
    const div = document.createElement('div');

    //div.parentNode = parent;
    div.className = 'content-container';
    div.id = `content-container-${pageName}`
    div.innerHTML = content;

    parent.appendChild(div);
    return div;
}

function generateContentWrapper(parent, pages) {
    if (pages.length === 1)
        return parent;

    const wrapper = document.createElement('div')
    //wrapper.parentNode = parent;
    wrapper.id = 'content-wrapper';

    // todo add select buttons

    parent.appendChild(wrapper);
    return wrapper;
}

// content resolving
function resolveTarget(mask) {
    let yields = [];

    for (page in pages) {
        if (isSet(page.id, mask))
            yields.push(page);
    }

    if (yields === undefined || yields.length === 0)
        yields.push(pages[0]);

    return yields;
}

function resolveContent(page) {
    // based on https://stackoverflow.com/questions/10932226/how-do-i-get-source-code-from-a-webpage

    let url = "./" + page['path'], xmlhttp; //Remember, same domain

    if ("XMLHttpRequest" in window)
        xmlhttp = new XMLHttpRequest();
    if ("ActiveXObject" in window)
        xmlhttp = new ActiveXObject("Msxml2.XMLHTTP");

    if (xmlhttp === undefined)
        return "Could not request content of " + page['name'];

    xmlhttp.open('GET', url, false);
    xmlhttp.send(null);

    if (xmlhttp.response.status)
        return missingPage(page);
    return xmlhttp.responseText;
}

function missingPage(page) {
    return "Unable to fetch content of " + page['name'];
}

function isSet(flag, inMask) {
    return (inMask & flag) !== 0;
}
