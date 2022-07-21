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
    'blog': {
        'display_name': "Blog",
        'path': 'https://blog.comroid.org',
        'policy': policy['instant_redir']
    },
    'auth': {
        'display_name': "Authentication Server",
        'path': 'https://auth.comroid.org',
        'policy': policy['instant_redir']
    },
    'projects': {
        'display_name': "Projects",
        'path': 'part/projects.html'
    },
    'guardian': {
        'display_name': "Guardian Framework",
        'path': 'part/guardian.html'
    },
    'crystalshard': {
        'display_name': "CrystalShard",
        'path': 'https://github.com/comroid-git/CrystalShard'
    },
    'kscr': {
        'display_name': "KScr Intermediate Language",
        'path': 'part/kscr.html'
    },
    'dirlinker': {
        'display_name': "DirLinker",
        'path': 'https://gitub.com/comroid-git/DirLinker'
    },
    'status': {
        'display_name': "Status Page",
        'path': 'https://status.comroid.org/slim/'
    },
    'github': {
        'display_name': "GitHub",
        'path': 'https://github.com/comroid-git',
        'policy': policy['instant_redir']
    },
    'contact': {
        'display_name': "Contact",
        'path': 'part/contact.html'
    },
    'privacy': {
        'display_name': "Privacy",
        'path': 'part/privacy.html'
    },
    'tos': {
        'display_name': "Terms of Use",
        'path': 'part/terms-of-use.html'
    }
}

const navigation = [
    {
        'type': 'box',
        'name': 'home'
    },
    {
        'type': 'box',
        'name': 'blog'
    },
    {
        'type': 'drop',
        'name': 'sitemap',
        'display': 'Sitemap',
        'content': [
            'auth',
            'status'
        ]
    },
    {
        'type': 'drop',
        'name': 'projects',
        'display': 'Projects',
        'content': [
            'kscr',
            'dirlinker',
            'guardian',
            'crystalshard'
        ]
    },
    {
        'type': 'box',
        'name': 'github'
    },
    {
        'type': 'drop',
        'name': 'about',
        'display': 'About',
        'content': [
            'privacy',
            'tos',
            'contact'
        ]
    }
]
