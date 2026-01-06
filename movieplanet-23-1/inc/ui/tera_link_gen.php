<?php
/**
 * TeraBox Link Generator
 * Centralizes deep link construction and device detection logic
 */

$userAgent = $_SERVER['HTTP_USER_AGENT'];
$is_android = (strpos($userAgent, 'Android') !== false);

// Generate unique ID
$utc_ms = round(microtime(true) * 1000);
$tera_link_id = $utc_ms . "-" . mt_rand(10000000, 99999999);

// Configuration
$config = [
    'surl' => '9HX7frJ3WjT08txahZv8EQ',
    'base_url' => 'https://be6.in/Movie57676-1080p-tera-main',
    'tv_url' => 'https://be6.in/Series767578_All_Seson_All',
    'mirror_link' => 'https://bit.ly/Mov6r7-propell'
];

$copytext = "&surl={$config['surl']}&tera_link_id={$tera_link_id}&tera_link_type=1";

// Determine which URL to use
$url = (($_GET['type'] ?? '') === 'tv') ? $config['tv_url'] : $config['base_url'];

// Special logic for known users/Android
$isKnowUser = isset($_SESSION['known_user']);
$should_use_direct = ($is_android && $isKnowUser);
?>

<script>
const landerConfig = {
    url: "<?php echo $url; ?>",
    copyText: "<?php echo $copytext; ?>",
    isAndroid: <?php echo $is_android ? 'true' : 'false'; ?>,
    isIndia: (new Date().getTimezoneOffset() === -330)
};

function open_tera_on_play() {
    const finalUrl = landerConfig.url;
    
    if (landerConfig.isAndroid) {
        navigator.clipboard.writeText(landerConfig.copyText)
            .then(() => window.open(finalUrl, '_blank'))
            .catch(() => window.open(finalUrl, '_blank'));
    } else {
        window.open(finalUrl, '_blank');
    }
}
</script>