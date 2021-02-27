const policy = {
    'skip_nav': 1,
    'instant_redir': 2
}
const pages = {
    'not_found': {
        'display_name': "404 Not Found",
        'path': 'part/not-found.html',
        'policy': policy['skip_nav']
    },

    'home': {
        'display_name': "Homepage",
        'path': 'part/homepage.html'
    },
    'status': {
        'display_name': "Status Page",
        'path': 'https://status.comroid.org/slim'
    },
    'github': {
        'display_name': "GitHub",
        'path': 'https://github.com/comroid-git'
    },
    'contact': {
        'display_name': "Contact",
        'path': 'part/contact.html'
    },
    'privacy': {
        'display_name': "Privacy",
        'path': 'part/privacy.html'
    }
}

const navigation = [
    {
        'type': 'box',
        'name': 'home'
    },
    {
        'type': 'box',
        'name': 'status'
    },
    {
        'type': 'box',
        'name': 'github'
    },
    {
        'type': 'box',
        'name': 'privacy'
    },
    {
        'type': 'box',
        'name': 'contact'
    }
]
