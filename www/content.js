const policy = {
    'skip_nav': 1,
    'instant_redir': 2
}

const pages = {
    'not_found': {
        'id': 1,
        'display_name': "404 Not Found",
        'path': 'part/not-found.html',
        'policy': policy['skip_nav']
    },

    'homepage': {
        'id': 2,
        'display_name': "Homepage",
        'path': 'part/homepage.html'
    },
    'statuspage': {
        'id': 4,
        'display_name': "Status Page",
        'path': 'https://status.comroid.org',
        'policy': policy['instant_redir']
    },
    'about': {
        'id': 8,
        'display_name': "About Us",
        'path': 'part/about.html'
    },

    'github': {
        'id': 16,
        'display_name': "GitHub",
        'path': 'https://github.com/comroid-git',
        'policy': policy['instant_redir']
    }
}
