const hash = window.location.hash.substr(1);

function initNavigation() {
    const ul = document.createElement('ul')

    for (key in pages) {
        const page = pages[key];
        const pagePolicy = getPolicy(page);

        if (isSet(policy['skip_nav'], pagePolicy))
            continue;

        const pageLoc = page['path'];
        const absUrl = pageLoc.startsWith('http');
        const li = document.createElement('li')
        const div = document.createElement('div')

        div.className = 'nav_button';
        div.innerText = page['display_name'];

        let url;
        let instantRedir = isSet(policy['instant_redir'], pagePolicy);
        if (instantRedir) {
            url = pageLoc;
        } else {
            url = (key === hash && absUrl) ? pageLoc : `./#${key}`;
        }
        const targetUrl = url;

        li.onclick = function () {
            location.href = targetUrl;
            if (!instantRedir)
                location.reload();
        };

        li.appendChild(div);
        ul.appendChild(li);
    }

    document.getElementsByTagName("nav")
        .item(0)
        .appendChild(ul);
}

function initContent() {
    console.debug("Hash: " + hash);

    const contentBox = document.getElementById('content');
    if (hash.length === 0) {
        const homepage = pages['home'];
        insertFrame(contentBox, homepage['path'], homepage['id'])
        return;
    }

    const print = resolveTarget();

    if (isSet(policy['instant_redir'], getPolicy(print)))
        location.href = print['path']

    insertFrame(contentBox, print['path'], print['id']);
}

function getPolicy(page) {
    const x = page['policy'];

    if (x === undefined)
        return 0;
    return x;
}

// content insertion
function insertFrame(parent, url, pageId) {
    const frame = document.createElement('iframe');

    frame.className = 'content-container';
    frame.id = `content-container-${pageId}`
    frame.setAttribute('src', url);

    parent.appendChild(frame);
    return frame;
}

// content resolving
function resolveTarget() {
    for (key in pages) {
        const page = pages[key];

        if (key === hash)
            return page;
    }

    return pages['not_found'];
}

function isSet(flag, inMask) {
    return (inMask & flag) !== 0;
}
