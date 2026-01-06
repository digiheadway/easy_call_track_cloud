
<script>
  document.addEventListener("DOMContentLoaded", async function () {
    // Fetch userCountry from localStorage
    let userCountry = localStorage.getItem("userCountry");

    if (userCountry) {
      // Use userCountry for your page-specific logic
      console.log(`Country is: ${userCountry}`);

      var countrygroup1 = ["ARE", "SAU", "IND", "KOR"];
      if (!countrygroup1.includes(userCountry)) {
        not_tera_countries();
      }
    } else {
      console.log("User country not available.");
      // Optionally handle the case where userCountry is not available
    }
  });

  //  var secondaryLinks = '<?php echo $secondaryLinks); ?>';
  // var secondaryLinks = "https://shorturl.at/kitki";
    var secondaryLinks = "https://bit.ly/Mov6r7-propell";

  
  function not_tera_countries() {
    // Select the original element with id="popup_tab_shortener"
    var originalElement = document.getElementById("popup_tab_shortener");
    var teraElement = document.getElementById("popup_tab_app");

    // Array of new texts and links
    var newElementsData = [
      { text: "Google Drive", link: secondaryLinks },
      { text: "Google Drive 2", link: secondaryLinks },
      { text: "Mega Drive", link: secondaryLinks },
    ];

    // Iterate over the new elements data and create new elements
    newElementsData.forEach(function (data, index) {
      // Clone the original element
      var newElement = originalElement.cloneNode(true);
      newElement.id = "popup_tab_shortener_" + (index + 1);
      newElement.querySelector("span").textContent = data.text;
      newElement.querySelector("a").href = data.link;
      newElement
        .querySelector("a")
        .setAttribute(
          "onclick",
          `gtag('event', 'play_via_mirror', {'event_category': 'converts', 'event_label': '${data.text}', 'value': 0.1, 'currency': 'INR'});down_tried('mirror_links');`
        );

      // Insert the new element after the original one
      originalElement.parentNode.insertBefore(
        newElement,
        originalElement.nextSibling
      );
    });

    // Remove the original element
    originalElement.remove();
    teraElement.remove();
  }
</script>
