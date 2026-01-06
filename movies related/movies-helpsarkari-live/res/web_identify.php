<?php

// $subdomain = "Movierulz";
$subdomain = $_COOKIE['subdomain'];
$domain = ".helpsarkari.com";
$url = "/res/generate_link.php";
$link = "https://" . $subdomain . $domain . $url;
// echo $link;
header('Location: ' . $link);
