const hash = window.location.hash.substr(1);

function initNavigation() {
    function generateLabel(key, page, inside) {
        const label = document.createElement('div')
        const pageLoc = page['path'];

        label.className = 'nav-button';
        label.id = `nav-button-${key}`
        label.innerText = page['display_name'];

        if (key === hash || (hash.length === 0 && key === 'home'))
            label.style.textDecoration = '#e3e3e3 underline';
        let url;
        let instantRedir = isSet(policy['instant_redir'], getPolicy(page));
        if (instantRedir) {
            url = pageLoc;
        } else {
            url = (key === hash && pageLoc.startsWith('http')) ? pageLoc : `./#${key}`;
        }
        const targetUrl = url;
        inside.onclick = function () {
            location.href = targetUrl;
            if (!instantRedir)
                location.reload();
        };

        inside.appendChild(label);

        return label;
    }

    function generateDropdown(key, li) {
        const dropdown = document.createElement('div')
        const dropdownId = `nav-dropdown-${key}`;

        dropdown.className = 'nav-dropdown'
        dropdown.id = dropdownId

        li.onmouseenter = function () {
            document.getElementById(dropdownId).style.display = 'block';
        };
        li.onmouseleave = function () {
            document.getElementById(dropdownId).style.display = 'none';
        };

        li.appendChild(dropdown);

        return dropdown;
    }

    const ul = document.createElement('ul')

    for (let id in navigation) {
        const blob = navigation[id];

        switch (blob['type']) {
            case 'box':
                const page = pages[blob['name']];

                if (isSet(policy['skip_nav'], getPolicy(page)))
                    continue;

                const pageLoc = page['path'];
                const li = document.createElement('li')

                generateLabel(blob['name'], page, li);

                ul.appendChild(li);
                break;
            case 'drop':
                const cont = document.createElement('li')

                const label = document.createElement('div')
                label.className = 'nav-button';
                label.id = `nav-button-${blob['name']}`
                label.innerText = blob['display'];
                cont.appendChild(label);

                const dropdownBox = generateDropdown(blob['name'], cont);

                for (let pageNameId in blob['content']) {
                    const subpage = pages[blob['content'][pageNameId]];
                    const label = generateLabel(blob['content'][pageNameId], subpage, dropdownBox)

                    label.className += " nav-dropdown-box"
                }

                ul.appendChild(cont);

                break;
        }
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
