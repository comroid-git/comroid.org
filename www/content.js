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
    'about': {
        'display_name': "About Us",
        'path': 'part/about.html'
    },

    'github': {
        'display_name': "GitHub",
        'path': 'https://github.com/comroid-git',
        'policy': policy['instant_redir']
    },
    'discord': {
        'display_name': "Discord",
        'path': 'https://discord.gg/comroid',
        'policy': policy['instant_redir']
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
        'type': 'drop',
        'name': 'refs',
        'display': 'References',
        'content': [ 'github', 'discord' ]
    }
]
