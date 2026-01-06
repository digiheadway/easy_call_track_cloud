<?php
/**
 * Ads Component
 * Centralizes ad placements and domain-specific logic
 */

function renderAd($type) {
    global $fulldomain;
    
    if ($fulldomain !== 'harleywives.com') return;

    switch ($type) {
        case 'header':
            ?>
            <br><br><br>
            <center>
                <script async src="https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=ca-pub-3167708670200886" crossorigin="anonymous"></script>
                <ins class="adsbygoogle"
                     style="display:block"
                     data-ad-client="ca-pub-3167708670200886"
                     data-ad-slot="7390242806"
                     data-ad-format="auto"
                     data-full-width-responsive="true"></ins>
                <script>(adsbygoogle = window.adsbygoogle || []).push({});</script>
            </center>
            <?php
            break;

        case 'mid_page':
        case 'content':
            ?>
            <script async src="https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=ca-pub-3167708670200886" crossorigin="anonymous"></script>
            <ins class="adsbygoogle"
                 style="display:block"
                 data-ad-format="autorelaxed"
                 data-ad-client="ca-pub-3167708670200886"
                 data-ad-slot="6921721636"></ins>
            <script>(adsbygoogle = window.adsbygoogle || []).push({});</script>
            <?php
            break;
    }
}
?>
