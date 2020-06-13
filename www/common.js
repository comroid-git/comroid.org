const urlBase = 'https://comroid.org/'

function initContent() {
    const pages = resolveTarget(window.location.hash);

    if (pages.length < 1) {
        console.error("No pages could be found")
        return;
    }

    const wrapper = generateContentWrapper(document, pages);

    for (page in pages) {
        resolveContent(page)
            .then(content => insertContent(wrapper, content, page.name))
            .catch(function (it) {
                console.error("something happened: " + it.toString())
            })
    }
}

// content insertion
function insertContent(dom, content, pageName) {
    const div = dom.createElement('div');

    div.className = 'content-container';
    div.id = `content-container-${pageName}`
    div.name = pageName;
    div.innerHTML = content;

    return div;
}

function generateContentWrapper(dom, pages) {
    if (pages.length === 1)
        return dom;

    const wrapper = dom.createElement('div')
    wrapper.id = 'content-wrapper';

    // todo add select buttons

    return wrapper;
}

// content resolving
function resolveTarget(mask) {
    let yields = [];

    for (page in pages.entries()) {
        if (isSet(page.id, mask))
            yields += page;
    }

    if (yields.length === 0)
        yields += pages.not_found;

    return yields;
}

function resolveContent(page) {
    return $("#result")
        .load(urlBase + page.path)
        .then(it => it.data)
}

function missingPage(page) {
    return "Unable to fetch content of " + page.name;
}

function isSet(flag, inMask) {
    return (inMask & flag) !== 0;
}
