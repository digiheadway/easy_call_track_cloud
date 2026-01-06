<?php
/**
 * Reusable Popup JavaScript Component
 * Contains the popup toggle function, GEO modification, and validation
 */
?>
<script>
// 1. Popup toggle function
function pop23_myFunction() {
    check_if_already_downloaded();
    var pop23_popup = document.getElementById("pop23_myPopup");
    const urlParams = new URLSearchParams(window.location.search);
    
    if (pop23_popup.classList.contains("show")) {
        pop23_popup.classList.add("hide");
        setTimeout(() => {
            pop23_popup.classList.remove("show");
            pop23_popup.classList.remove("hide");
        }, 300);
    } else {
        pop23_popup.classList.add("show");
        gtag('event', 'watch_play_dialog');
        applyGeoModifications();
    }
}

// 2. GEO Modifications (originally not_tera.php)
function applyGeoModifications() {
    const userCountry = localStorage.getItem("userCountry");
    if (!userCountry) return;

    const allowedCountries = ["ARE", "SAU", "IND", "KOR"];
    if (!allowedCountries.includes(userCountry)) {
        const shortenerTab = document.getElementById("popup_tab_shortener");
        const appTab = document.getElementById("popup_tab_app");
        const mirrorLink = "https://bit.ly/Mov6r7-propell";

        if (shortenerTab && appTab) {
            const mirrors = [
                { text: "Google Drive", link: mirrorLink },
                { text: "Mega Drive", link: mirrorLink }
            ];

            mirrors.forEach((data, i) => {
                let clone = shortenerTab.cloneNode(true);
                clone.id = "mirror_" + (i + 1);
                clone.querySelector("span").textContent = data.text;
                clone.querySelector("a").href = data.link;
                clone.querySelector("a").onclick = () => {
                    gtag('event', 'play_via_mirror', { 'label': data.text });
                    if (typeof down_tried === 'function') down_tried('mirror_links');
                };
                shortenerTab.parentNode.insertBefore(clone, shortenerTab.nextSibling);
            });

            shortenerTab.remove();
            appTab.remove();
        }
    }
}

// 3. Validation & Redirects
const searchQ = new URLSearchParams(window.location.search).get('q') || '';
if (searchQ.includes('https') || searchQ.includes('surl')) {
    alert("Invalid search query.");
    window.location.href = "/";
}

// 4. Download media handler
function download_media(source) {
    record('new_btns_tera', source);
    pop23_myFunction();
}
</script>
