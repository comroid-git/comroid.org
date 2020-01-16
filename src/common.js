function load() {
    let random = Math.random();
    if (random < 0.05)
        document.getElementById('error').textContent = '///Under Construxion///';
    else if (random < 0.001) {
        document.getElementById('error').textContent = 'Under your Bed';
        document.getElementById('info').textContent = 'I am under your Bed';
    }
}