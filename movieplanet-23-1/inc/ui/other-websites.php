<?php
/**
 * Other Websites Component
 * Dynamically generates a list of other websites based on the subdomains list
 */

$current_subdomain = $subdomainexactly ?? '';

// Some websites might have special URLs that aren't just subdomain.domain.ext
$special_cases = [
    'ibomma' => 'https://ibomma.helpsarkari.com/',
    'soap2day' => 'https://soap2day.helpsarkari.com/',
    '1moviesda' => 'https://1moviesda.netlify.app/',
    'skymovies' => 'https://skymovieshd1.com/',
    'olamovies' => 'https://olamovies.vercel.app/',
];
?>

<h2 class="content_sec_heading">Other Websites</h2>
<div class="h1_underline"></div>
<ul class="other_web_links r-flex ali-c jut-fs flex-w">
    <?php foreach ($allowed_subdomains as $sub): ?>
        <?php 
        // Skip current subdomain
        if ($sub === $current_subdomain) continue;
        
        // Determine URL
        if (isset($special_cases[$sub])) {
            $url = $special_cases[$sub];
        } else {
            $url = "https://{$sub}.{$domainwithext}";
        }
        
        // Beautify name
        $name = ucwords(str_replace('-', ' ', $sub));
        ?>
        <li class="web_link_list">
            <a target="_blank" href="<?php echo htmlspecialchars($url); ?>">
                <?php echo htmlspecialchars($name); ?>
            </a>
        </li>
    <?php endforeach; ?>
    
    <!-- Hardcoded extra links from original file -->
    <li class="web_link_list"><a target="_blank" href="https://m3mcodpanipat.com/">M3M Panipat</a></li>
    <li class="web_link_list"><a target="_blank" href="https://9kmovies1.com/">9kMovies</a></li>
    <li class="web_link_list"><a target="_blank" href="http://tridentpanipat.in/">Trident Panipat Plots</a></li>
    <li class="web_link_list"><a target="_blank" href="https://godrejpropertiesinfo.com/godrej-panipat/">Godrej Panipat Plots</a></li>
    <li class="web_link_list"><a target="_blank" href="https://medium.com/@devmaster443/sites-that-must-check-fb94afa0deb3">Other Websites 1</a></li>
</ul>