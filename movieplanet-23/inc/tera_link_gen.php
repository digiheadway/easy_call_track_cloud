
<?php
$userAgent = $_SERVER['HTTP_USER_AGENT'];

// Generate tera_link_id with UTC time up to milliseconds and 8 random integers
$utc_milliseconds = round(microtime(true) * 1000);
$tera_link_id = $utc_milliseconds . "-" . mt_rand(10000000, 99999999);


// Construct copytext variable with tera_link_id
// $copytext = "&surl=rV6WnfcX3DoDk5tVBId56Q&tera_link_id={$tera_link_id}&tera_link_type=1";
$copytext = "&surl=9HX7frJ3WjT08txahZv8EQ&tera_link_id={$tera_link_id}&tera_link_type=1";

// Prepare the URL with the new link id created
// $directurl = "market://details?id=com.dubox.drive&referrer=utm_source%3Dtera_sharelink%26utm_campaign%3Dheader_button%26surl%3DrV6WnfcX3DoDk5tVBId56Q%26tera_link_id%3D{$tera_link_id}%26tera_link_type%3D1%26c%3Dhttps%253A%252F%252Fwww.freeterabox.com%252Fwap%252Fshare%252Ffilelist%253Fsurl%253DrV6WnfcX3DoDk5tVBId56Q%26af_dp%3Ddubox%253A%252F%252Fshare_detail%253Fshareid%253D54558678269%2526uk%253D4399317291483%2526share_create_time%253D1718129409%2526randsk%253Dj%25252FHI2fJmxjLbTOfnr%25252F6ywyxCFwPfYi2c9oFlkQUGGM4%25253D%2526share_expired_time%253D0%2526source%253Daf_link%2526surl%253DrV6WnfcX3DoDk5tVBId56Q%26agency%3Da";
// $directurl = "market://details?id=com.dubox.drive&referrer=utm_source%3Dtera_sharelink%26utm_campaign%3Dheader_button%26surl%3DrV6WnfcX3DoDk5tVBId56Q%26tera_link_id%3D{$tera_link_id}%26tera_link_type%3D1%26c%3Dhttps%253A%252F%252Fwww.1024tera.com%252Fwap%252Fshare%252Ffilelist%253Fsurl%253DrV6WnfcX3DoDk5tVBId56Q%26af_dp%3Ddubox%253A%252F%252Fshare_detail%253Fshareid%253D43636840018%2526uk%253D4401833920836%2526share_create_time%253D1718964012%2526randsk%253DGYgxVsM2gOhLz25bB%25252Bij%25252BEQKHvsiMFoR9oFlkQUGGM4%25253D%2526share_expired_time%253D0%2526source%253Daf_link%2526surl%253DrV6WnfcX3DoDk5tVBId56Q%26agency%3Da";

$directurl = "market://details?id=com.dubox.drive&referrer=utm_source%253Dtera_sharelink%2526utm_campaign%253Dheader_button%2526surl%253DR5P3ey37nK8lXN8FeB8ysg%2526tera_link_id%253D{$tera_link_id}%2526tera_link_type%253D1%2526c%253Dhttps%25253A%25252F%25252Fwww.terabox.app%25252Fwap%25252Fshare%25252Ffilelist%25253Fsurl%25253DR5P3ey37nK8lXN8FeB8ysg%2526af_dp%253Ddubox%25253A%25252F%25252Fshare_detail%25253Fshareid%25253D59164277975%252526uk%25253D4401581855675%252526share_create_time%25253D1724409119%252526randsk%25253D%2525252FBFeRX88RVQI9iXE%2525252BBYqg%2525252FyO%2525252BOvlcz3haVh%2525252FmNsfQSE%2525253D%252526share_expired_time%25253D0%252526source%25253Daf_link%252526share_file_count%25253D1%252526surl%25253DR5P3ey37nK8lXN8FeB8ysg%2526agency%253Dj1__0";

$directurl = 'https://be6.in/Movie57676-1080p-tera-main';

$teraburl = 'https://be6.in/Movie57676-1080p-tera-main';


$url = $teraburl;

if (isset($_SESSION['known_user'])) {
    $url = $directurl;
    $isandroid = true;
} else {
    $isandroid = false;
}

if (strpos($userAgent, 'Android') !== false) {
    if (isset($_SESSION['known_user'])) {
        $url = $directurl;
        $isandroid = true;
    } else {
        $isandroid = false;
    }

} else {
    $isandroid = false;
}
$browserTeraLinks = [
    "https://bit.ly/MOV6573682",
    "https://bit.ly/4bWAAfz",
    "https://bit.ly/Movie95676",
    "https://bit.ly/Movie95676",
    "https://bit.ly/Movie95676",
    "https://bit.ly/Movie95676",
    "https://bit.ly/Movie95676"
];
// Assign one value randomly to the $browser_tera_link variable
//  $browserTeraLink = $browserTeraLinks[array_rand($browserTeraLinks)];


$browserTeraLink1 = "https://be6.in/Movie57676-1080p-tera-url";
$browserTeraLink = "https://be6.in/Movie65676-mirrors-adstr";


$secondaryLinks = "https://be6.in/mov5667y8_adst_17dec";
$terawatch = "https://be6.in/adstera_new-mov678";

if ($_GET['type'] == 'tv') {
    $browserTeraLink1 = "https://be6.in/Series767578_All_Seson_All";
    $url = "https://be6.in/Series767578_All_Seson_All";
}
?>


<script>
   function getzne() {
            return new Date().getTimezoneOffset();
        }

        const getznevar = getzne();
const isind = getznevar === -330;
var teraburl = "<?php echo $teraburl; ?>";

        
function open_tera_on_play() {
    if(isind){
var url = "<?php echo $url; ?>";
    }else {
        var url = "<?php echo $teraburl; ?>";
    }
    
     // Check if isandroid is true
    <?php if ($isandroid): ?>
                                                                                                                                                             var copytext = "<?php echo $copytext; ?>";    
                                                                                                                                                            navigator.clipboard.writeText(copytext)
                                                                                                                                                                .then(function() {
                                                                                                                                                                    window.open(url, '_blank');
                                                                                                                                                                })
                                                                                                                                                                .catch(function(err) {
                                                                                                                                                                    window.open(teraburl, '_blank');
                                                                                                                                                                });
    <?php else: ?>
                                                                                                                                                            window.open(url, '_blank');
    <?php endif; ?>
}


</script>