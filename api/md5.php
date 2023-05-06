<?php
$url = $_GET['url'] ?? '';

if (!filter_var($url, FILTER_VALIDATE_URL)) {
    http_response_code(404);
    die('Invalid URL');
}

echo shell_exec('curl -q -H "CacheControl: no-cache;" '.$url.' | md5sum | grep -Po \'\\K\\w*(?=\\s)\'');
?>