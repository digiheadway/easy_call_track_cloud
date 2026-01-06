<?php

// Getting Subdomain Starts
$arr = explode('.', $_SERVER["SERVER_NAME"]);
$canonical = (empty($_SERVER['HTTPS']) ? 'http://' : 'https://') . $_SERVER['HTTP_HOST'] . strtok($_SERVER['REQUEST_URI'], '?');
$subdomainexactly = $arr[0];
$subdomainexactly_for_domain_checking = $arr[0];
$domain = $arr[1];
$ext = $arr[2];
$fulldomain = $domain . "." . $ext;
$domainwithext = $fulldomain . "?ref=" . $subdomainexactly;

// Supporting Other Websites with header website simulation

// for other Pages
if (isset($_COOKIE['dm'])) {
  $subdomainexactly = $_COOKIE['dm'];
}

// Debug Menu on off trigger
if (isset($_GET['debug'])) {
  ini_set('display_errors', 1);
  ini_set('display_startup_errors', 1);
  error_reporting(E_ALL);
}

// for landing page
if (isset($_GET['dm'])) {
  $subdomainexactly = $_GET['dm'];
  setcookie('dm', $_GET['dm'], time() + 3600);
}

// Making first word capital
$subdomain = ucwords($subdomainexactly);
// Getting Subdomain Ends

// subdomain for title
$subdomaintitle = ucwords(strtolower(str_replace("-", "_", $subdomain)));


$this_month = date("M Y");


$query_params = $_SERVER['QUERY_STRING'];

$func_search_url = "/msearch.php?" . $query_params;
$func_search_url_prefix = "/msearch.php?q=";

if (isset($_GET['q'])) {
  $searchquery = trim(ucwords($_GET['q']));
}
if (!isset($searchquery)) {
  $searchquery = isset($_COOKIE['q']) ? $_COOKIE['q'] : null;
  $lang = isset($_COOKIE['lang']) ? $_COOKIE['lang'] : null;
  $quality = isset($_COOKIE['quality']) ? $_COOKIE['quality'] : null;
  $filename = isset($searchquery) ? (str_replace(' ', '-', $searchquery) . '-1080p.mp4') : null;
}
// Extreme function to capatlise the subdomain
// $int_part = (int)$subdomain; // extract integer part
// $str_part = substr($subdomain, strlen((string)$int_part)); // extract string part
// $capitalized_str = ucfirst(strtolower($str_part)); // capitalize string part
// $subdomain = $int_part . $capitalized_str; // join integer and capitalized string parts


// exit if not match subdomain 

$allowed_subdomains = array(
  'ibomma',
  'movie-rulez-com',
  'movierulz',
  'soap2day',
  'filmyzilla',
  'mp4moviez',
  'tamilyogi',
  'bolly4u',
  'katmoviehd',
  'telegram-web',
  '123movies',
  'extramovies',
  'khatrimaza',
  '9xmovies',
  'tamilrockers',
  'fmovies',
  'worldfree4u',
  'pluto-tv',
  'pirate-bay',
  'u-watching-free',
  'vudu',
  'yify-movies',
  'gomovies',
  'peacock-tv',
  'putlockers',
  'plex',
  'kissanime',
  'yesmovies',
  'flixtor',
  'tubi-tv',
  'primewire',
  'moviesjoy',
  'tamilgun',
  '1337x',
  'hoopla',
  'solarmovie',
  'watch-movies-free',
  'fmovies-to',
  'roku-channel',
  'vumoo',
  '0gomovie-s',
  'f2movies',
  'watchseries',
  'crackle',
  'pirate-proxy',
  'kanopy',
  'popcornflix',
  'himovies',
  'cmovies',
  'bounce-tv',
  'yomovies',
  'lookmovie',
  'imdb-tv',
  'movies123',
  'azmovies',
  'fz-movies',
  'einthusan',
  'vegamovies',
  'ytsmovies',
  'watchcartoononline',
  'tamilrockerscom',
  'tamilrockersco',
  'tamilrockers',
  'bmovies',
  'hdhub4u',
  'a-to-z-movies',
  'ato-z-movies',
  'isaimini',
  '7starhd',
  'moviesda',
  'dvdplay',
  'hdhub4u',
  'hdmoviearea',
  'tamilblasters',
  'moviesflix',
  'ipagal'
);

$titlesuffix = "Download or Watch Online";


if (!in_array($subdomainexactly_for_domain_checking, $allowed_subdomains)) {
  //  if (!$_GET['dm']) {
  header('HTTP/1.0 404 Not Found');
  header("Location: https://filmyzilla.moviesda10.com/");

  exit;
  //  }
}
?>

