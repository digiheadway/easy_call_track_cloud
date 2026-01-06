<?php
/**
 * SEO & Schema Component
 * Handles dynamic generation of JSON-LD schemas
 */

function renderSchemas($data) {
    $subdomain = $data['subdomain'] ?? '';
    $subdomaintitle = $data['subdomaintitle'] ?? '';
    $fulldomain = $data['fulldomain'] ?? '';
    $url = "https://{$subdomain}.{$fulldomain}/";

    $schemas = [];

    // 1. WebSite Schema (Search Action)
    $schemas[] = [
        "@context" => "https://schema.org/",
        "@type" => "WebSite",
        "name" => $subdomaintitle,
        "url" => $url,
        "potentialAction" => [
            "@type" => "SearchAction",
            "target" => "{$url}msearch.php?q={search_term_string}",
            "query-input" => "required name=search_term_string"
        ]
    ];

    // 2. Organization Schema
    $schemas[] = [
        "@context" => "https://schema.org",
        "@type" => "NewsMediaOrganization",
        "name" => $subdomaintitle,
        "alternateName" => $subdomain,
        "url" => $url,
        "logo" => "https://{$subdomain}.{$fulldomain}/assets/img/favicon.png",
        "sameAs" => ["https://www.facebook.com/wikipedia/"]
    ];

    // 3. Article Schema
    $schemas[] = [
        "@context" => "https://schema.org",
        "@type" => "Article",
        "mainEntityOfPage" => [
            "@type" => "WebPage",
            "@id" => $url
        ],
        "headline" => "{$subdomaintitle} - Watch Free Movies",
        "description" => "Download or Watch latest movies and webseries on {$subdomaintitle}.",
        "image" => "https://{$subdomain}.{$fulldomain}/assets/img/favicon.png",
        "author" => [
            "@type" => "Organization",
            "name" => "Contributors to Wikimedia projects"
        ],
        "publisher" => [
            "@type" => "Organization",
            "name" => $subdomaintitle,
            "logo" => [
                "@type" => "ImageObject",
                "url" => "https://{$subdomain}.{$fulldomain}/assets/img/favicon.png"
            ]
        ],
        "datePublished" => "2023-05-03",
        "dateModified" => date('Y-m-d')
    ];

    foreach ($schemas as $schema) {
        echo '<script type="application/ld+json">' . json_encode($schema, JSON_UNESCAPED_SLASHES | JSON_PRETTY_PRINT) . '</script>' . PHP_EOL;
    }
}
?>
