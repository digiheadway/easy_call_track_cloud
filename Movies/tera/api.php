<?php

// Default values for link_id and surl
$default_surl = 'yHftaLAwlwPx4hD1KkFgQg';
$default_link_id = '1728765897089-62595217';
$default_uk = '4398511686747';
$default_shareid = '51860903277';
$share_create_time = '1728722916';
$default_randsk = 'brxzEUa2yGKtCgL3Xbs1TBFBagoKDL5sVB1x+viMXdU=';

$origin = 'https://www.1024tera.com';

$sec_ch_ua = 'Google Chrome";v="129", "Not=A?Brand";v="8", "Chromium";v="129"';
$sec_ch_ua_mobile = '?1';
$sec_ch_ua_platform = 'Android';


// Function to sanitize input
function sanitize_input($data)
{
    return htmlspecialchars(trim($data), ENT_QUOTES, 'UTF-8');
}

// Fetch from POST or GET parameters

$surl = isset($_POST['surl']) ? sanitize_input($_POST['surl']) : (isset($_GET['surl']) ? sanitize_input($_GET['surl']) : $default_surl);

$link_id = isset($_POST['link_id']) ? sanitize_input($_POST['link_id']) : (isset($_GET['link_id']) ? sanitize_input($_GET['link_id']) : $default_link_id);

$surl = isset($_POST['surl']) ? sanitize_input($_POST['surl']) :
    (isset($_GET['surl']) ? sanitize_input($_GET['surl']) : $default_surl);

$link_id = isset($_POST['link_id']) ? sanitize_input($_POST['link_id']) :
    (isset($_GET['link_id']) ? sanitize_input($_GET['link_id']) : $default_link_id);

$uk = isset($_POST['uk']) ? sanitize_input($_POST['uk']) :
    (isset($_GET['uk']) ? sanitize_input($_GET['uk']) : $default_uk);

$shareid = isset($_POST['shareid']) ? sanitize_input($_POST['shareid']) :
    (isset($_GET['shareid']) ? sanitize_input($_GET['shareid']) : $default_shareid);

$randsk = isset($_POST['randsk']) ? sanitize_input($_POST['randsk']) :
    (isset($_GET['randsk']) ? sanitize_input($_GET['randsk']) : $default_randsk);

// Dynamic timestamp for 't' parameter
$t = time() * 1000;

// Common values organized by type
// API settings
$clienttype = '5';
$version = 'v5';

// Shared parameters
$currentUrl = 'https://www.1024tera.com/wap/share/filelist?surl=' . $surl;
$referrer = 'https://www.1024tera.com/wap/share/filelist?surl=' . $surl;

// Fetching device information dynamically
$useragent = isset($_SERVER['HTTP_USER_AGENT']) ? $_SERVER['HTTP_USER_AGENT'] : 'unknown_device';
$device = ' sm-g981b';
$system = php_uname('s') . ' ' . php_uname('r'); // OS name and version
$cpu = 'unknown'; // CPU information might not be available directly
$lang = 'en-GB'; // Default language
$width = '412'; // You can adjust this based on your needs
$height = '915'; // You can adjust this based on your needs
$osVersion = 'android 13';

// Static values
$jsToken = '4BD63D494018A6DE956D2D68FD0AB5C6E1DFAAD4FEBD188038F1F2F959B20EFBF68D89E45F8D81DA057341A616D20059624';
$dp_logid = '94126100247706570024';

// Static af_dp parameters
$af_dp_params = [
    'shareid' => $shareid,
    'uk' => $uk,
    'share_create_time' => $share_create_time,
    'randsk' => $randsk,
    'share_expired_time' => '0',
    'source' => 'af_link',
    'share_file_count' => '1',
];

// Prepare af_dp parameter as a query string
$af_dp_query = http_build_query($af_dp_params);
$af_dp = 'dubox://share_detail?' . $af_dp_query . '&surl=' . $surl; // Combining with dynamic surl

// Common headers
$common_headers = [
    'Accept: application/json, text/plain, */*',
    'Accept-Language: en-GB,en-US;q=0.9,en;q=0.8',
    'Connection: keep-alive',
    'Content-Type: application/x-www-form-urlencoded',
    'User-Agent: ' . $useragent,
    'X-Requested-With: XMLHttpRequest'
];



// Function to send a POST request using cURL
function sendPostRequest($url, $data, $headers)
{
    $ch = curl_init($url);

    // Set cURL options
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, $data);
    curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);

    $response = curl_exec($ch);

    if (curl_errno($ch)) {
        echo 'Error: ' . curl_error($ch);
    } else {
        echo 'Response: ' . $response;
    }

    curl_close($ch);
}

// Request 1 - Posting shareExpType, shareProfitExpType, link_id, surl
function request1($link_id, $surl)
{
    global $clienttype, $version, $currentUrl, $t;

    $url = 'https://www.1024tera.com/api/analytics';
    $data = http_build_query([
        'shareExpType' => 'typeA',
        'shareProfitExpType' => 'e',
        'surl' => $surl,
        'uk' => $uk,
        'type' => 'wap_share_host_open_na_btn_click',
        'clienttype' => $clienttype,
        'version' => $version,
        'currentUrl' => $currentUrl,
        'client' => 'wap',
        't' => $t,
        'link_id' => $link_id // Pass link_id
    ]);

    sendPostRequest($url, $data, $GLOBALS['common_headers']);
}

// Request 2 - Posting device info and link_id, surl
function request2($link_id, $surl)
{
    global $clienttype, $version, $t, $currentUrl;

    $url = 'https://www.1024tera.com/api/analytics';
    $data = http_build_query([
        'cpu' => $GLOBALS['cpu'],
        'lang' => $GLOBALS['lang'],
        'tera_link_id' => $link_id, // Pass link_id
        'tera_link_type' => '1',
        'device' => $GLOBALS['device'],
        'system' => $GLOBALS['system'],
        'width' => $GLOBALS['width'],
        'height' => $GLOBALS['height'],
        'type' => 'wap_share_teralink_device_upload',
        'clienttype' => $clienttype,
        'version' => $version,
        'currentUrl' => $currentUrl,
        'client' => 'wap',
        't' => $t,
        'surl' => $surl // Pass surl
    ]);

    sendPostRequest($url, $data, $GLOBALS['common_headers']);
}

// Request 3 - Posting tera_link_id, surl
function request3($link_id, $surl)
{
    global $clienttype, $version, $currentUrl, $t;

    $url = 'https://www.1024tera.com/api/analytics';
    $data = http_build_query([
        'from' => 'view-in-na',
        'tera_link_id' => $link_id, // Pass link_id
        'type' => 'wap_share_onlink_key_way',
        'clienttype' => $clienttype,
        'version' => $version,
        'currentUrl' => $currentUrl,
        'client' => 'wap',
        't' => $t,
        'surl' => $surl // Pass surl
    ]);

    sendPostRequest($url, $data, $GLOBALS['common_headers']);
}

// Execute requests
request1($link_id, $surl);
request2($link_id, $surl);
request3($link_id, $surl);

// Initialize a cURL session
$ch = curl_init();

// Define URL and required parameters
$web = '1';
$channel = 'dubox';
$dpLogid = '96415800168719760023'; // varies
$version = '0';
$devUid = '0';
$cuid = '0';
$lang = 'en';
$app_id = '250528';

// Set POST fields
$pid = 'tera_sharelink';
$c = $currentUrl;
$pdetail = 'header_button';
$teraLinkId = $link_id;
$teraLinkType = '1';
$agency = 'j1__0';
$opSource = '';
// $afDp = 'dubox%3A%2F%2Fshare_detail%3Fshareid%3D51860903277%26uk%3D4398511686747%26share_create_time%3D1728722916%26randsk%3DbrxzEUa2yGKtCgL3Xbs1TBFBagoKDL5sVB1x%252BviMXdU%253D%26share_expired_time%3D0%26source%3Daf_link%26share_file_count%3D1%26surl%3DyHftaLAwlwPx4hD1KkFgQg';
$af_dp_query = http_build_query($af_dp_params);
$af_dp = 'dubox://share_detail?' . $af_dp_query . '&surl=' . $surl; // Combining with dynamic surl
// $referrer = '';


// Construct the URL
$url = "https://www.1024tera.com/api/srn/ck?app_id=$app_id&web=$web&channel=$channel&clienttype=$clienttype&jsToken=$jsToken&dp-logid=$dpLogid&version=$version&devuid=$devUid&cuid=$cuid&lang=$lang";

// Set the URL and required options
curl_setopt($ch, CURLOPT_URL, $url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);  // To store response in a variable

// Set headers
$headers = [
    'Accept: application/json, text/plain, */*',
    'Accept-Language: en-GB,en-US;q=0.9,en;q=0.8',
    'Connection: keep-alive',
    'Content-Type: application/x-www-form-urlencoded',
    'Cookie: csrfToken=xecs0gsT4YpPgWJDreMhuJYP; browserid=r39n22pZaH92VSeUZWF8NumoEIgt8f8W4i-SZ1pUr-SpuCGF-Q6jTEXmVyk=; lang=en; TSID=f6P9Brtvf3DDc6gwGIdbsChVaNyPfCOc; _gcl_au=1.1.1089347491.1728765897; __bid_n=1913f5bd20218c6a094207; _tt_enable_cookie=1; _ttp=MRgaJkoLNc-70CIzsB3b7rz7Ukr; _ga=GA1.1.1138989302.1728765897; ndut_fmt=264218271CB63AFA71708C6B6D86E72DDC1D507B0BDB1ED6BFA81BD4821D9E40; ab_sr=1.0.1_MmQxZDc2YmU4ODNiYTJhYWI4NGVhYzk5ZTVjOWQwMmFlMTUwMjI3NTVmY2FhZDRkNDlmNzRhZjU0ZTQzMDU3MzIzZDFkNDc1NjJjMzNjNDhhNDkyZTM1MTRmMzYxOWIwYTJlNTAwYTRhYTk1NTA5N2MxMTM0ZGQzYzNjNmU3NmNkNzkxNGVhMjIyYTVkNGNjNGI1OWU3ZTIzMWU0NTViMw==; _ga_RSNVN63CM3=GS1.1.1728773198.3.1.1728773202.56.0.0',
    'Origin:' . $origin,
    'Referer: ' . $referrer,
    'Sec-Fetch-Dest: empty',
    'Sec-Fetch-Mode: cors',
    'Sec-Fetch-Site: same-origin',
    'User-Agent: ' . $useragent,
    'X-Requested-With: XMLHttpRequest',
    'sec-ch-ua:' . $sec_ch_ua,
    'sec-ch-ua-mobile:' . $sec_ch_ua_mobile,
    'sec-ch-ua-platform:' . $sec_ch_ua_platform,
];

curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);



$postData = http_build_query([
    'pid' => $pid,
    'c' => $c,
    'pdetail' => $pdetail,
    'tera_link_id' => $teraLinkId,
    'tera_link_type' => $teraLinkType,
    'agency' => $agency,
    'op_source' => $opSource,
    'lang' => $lang,
    'os_version' => $osVersion,
    'device' => $device,
    'af_dp' => $af_dp,
    'referrer' => $referrer,
]);

curl_setopt($ch, CURLOPT_POST, true);
curl_setopt($ch, CURLOPT_POSTFIELDS, $postData);

// Execute the request and capture the response
$response = curl_exec($ch);

// Check for errors
if (curl_errno($ch)) {
    echo 'Error:' . curl_error($ch);
} else {
    echo 'Link ID:' . $link_id . ' || SURL' . $surl . ' || Response:' . $response;
}

// Close the cURL session
curl_close($ch);
?>