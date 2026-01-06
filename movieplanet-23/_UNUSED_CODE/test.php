<?php

// Generate tera_link_id with UTC time up to milliseconds and 8 random integers
$utc_milliseconds = round(microtime(true) * 1000);
$tera_link_id = $utc_milliseconds . "-" . mt_rand(10000000, 99999999);


$surl = "dqFU8m1Ub5U_4_MUyz4VRQ";
// Construct copytext variable with tera_link_id
$copytext = "&surl={$surl}&tera_link_id={$tera_link_id}&tera_link_type=1";

$directurl = "market://details?id=com.dubox.drive&referrer=utm_source%3Dtera_sharelink%26utm_campaign%3Dheader_button%26surl%3D{$surl}%26tera_link_id%3D{$tera_link_id}%26tera_link_type%3D1%26c%3Dhttps%253A%252F%252Fwww.freeterabox.com%252Fwap%252Fshare%252Ffilelist%253Fsurl%253D{$surl}%26af_dp%3Ddubox%253A%252F%252Fshare_detail%253Fshareid%253D54558678269%2526uk%253D4399317291483%2526share_create_time%253D1718129409%2526randsk%253Dj%25252FHI2fJmxjLbTOfnr%25252F6ywyxCFwPfYi2c9oFlkQUGGM4%25253D%2526share_expired_time%253D0%2526source%253Daf_link%2526surl%253D{$surl}%26agency%3Da";

$teraurl = "dubox://share_detail?shareid=50087964265&uk=4399535455304&share_create_time=1718913516&randsk=ceR7y1%2F0QyNxGDohRrMISj0wiGbh1%2BSJaVh%2FmNsfQSE%3D&share_expired_time=0&source=deepLink&surl=dqFU8m1Ub5U_4_MUyz4VRQ";

$teramarket = "market://details?id=com.dubox.drive&referrer=utm_source%3Dtera_sharelink%26utm_campaign%3Dheader_button%26surl%3DdqFU8m1Ub5U_4_MUyz4VRQ%26tera_link_id%3D1718966258986-94293076%26tera_link_type%3D1%26c%3Dhttps%253A%252F%252Fwww.1024tera.com%252Fwap%252Fshare%252Ffilelist%253Fsurl%253DdqFU8m1Ub5U_4_MUyz4VRQ%26af_dp%3Ddubox%253A%252F%252Fshare_detail%253Fshareid%253D50087964265%2526uk%253D4399535455304%2526share_create_time%253D1718913516%2526randsk%253DceR7y1%25252F0QyNxGDohRrMISj0wiGbh1%25252BSJaVh%25252FmNsfQSE%25253D%2526share_expired_time%253D0%2526source%253Daf_link%2526surl%253DdqFU8m1Ub5U_4_MUyz4VRQ%26agency%3Da";

$teramarket2 = "market://details?id=com.dubox.drive&referrer=utm_source%3Dtera_sharelink%26utm_campaign%3Dheader_button%26surl%3DdqFU8m1Ub5U_4_MUyz4VRQ%26tera_link_id%3D1718967142449-80047583%26tera_link_type%3D1%26c%3Dhttps%253A%252F%252Fwww.1024tera.com%252Fwap%252Fshare%252Ffilelist%253Fsurl%253DdqFU8m1Ub5U_4_MUyz4VRQ%26af_dp%3Ddubox%253A%252F%252Fshare_detail%253Fshareid%253D50087964265%2526uk%253D4399535455304%2526share_create_time%253D1718913516%2526randsk%253DceR7y1%25252F0QyNxGDohRrMISj0wiGbh1%25252BSJaVh%25252FmNsfQSE%25253D%2526share_expired_time%253D0%2526source%253Daf_link%2526surl%253DdqFU8m1Ub5U_4_MUyz4VRQ%26agency%3Da";
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
</head>
<body>
    <a href="<?php echo $directurl;?>">directurl </a><br>
        <a href="<?php echo $teraurl; ?>">teraurl</a><br>
            <a href="<?php echo $teramarket; ?>">teramarket</a><br>


    
</body>
</html>