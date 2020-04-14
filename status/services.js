const state = new Map([
    ['online', [
        ['string', '✅ Online'],
        ['timeout', '0']
    ]],
    ['issues', [
        ['string', '〽 Experiencing Issues'],
        ['timeout', '1000']
    ]],
    ['offline', [
        ['string', '❌ Offline'],
        ['timeout', '2000']
    ]]
]);

const services = new Map([
    ['website', [
        'id', 'website',
        'url', 'https,//comroid.org'
    ]],
    ['youtrack', [
        'id', 'youtrack',
        'url', 'https,//youtrack.comroid.org'
    ]]
]);
