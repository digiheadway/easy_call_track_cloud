// include_popup.js
(function () {
	// Function to check if the user is on a mobile device
	function isMobileDevice() {
		return (
			typeof window.orientation !== "undefined" ||
			navigator.userAgent.indexOf("IEMobile") !== -1
		);
	}

	// Check if the dialog has already been included and if the user is on a mobile device
	if (!document.getElementById("pop23_container") && isMobileDevice()) {
		// Create a <div> element to hold the popup dialog
		var popupContainer = document.createElement("div");

		// Fetch the HTML content for the popup dialog
		fetch("/open_in_app/popup.php")
			.then((response) => response.text())
			.then((htmlContent) => {
				popupContainer.innerHTML = htmlContent;

				// Append the popup dialog to the body
				document.body.appendChild(popupContainer);

				// Call the inline script to initialize the popup functionality
				var inlineScript = popupContainer.querySelector("script");
				if (inlineScript) {
					eval(inlineScript.innerText);
				}
				// Call pop23_myFunction after the content is fully loaded
				pop23_myFunction();
			})
			.catch((error) => {
				console.error("Error fetching the dialog content:", error);
			});
	}
})();

function pop23_myFunction() {
	var pop23_popup = document.getElementById("pop23_myPopup");
	if (pop23_popup.classList.contains("show")) {
		pop23_popup.classList.add("hide");

		// Wait for the animation to complete and then remove the 'show' and 'hide' classes
		setTimeout(() => {
			pop23_popup.classList.remove("show");
			pop23_popup.classList.remove("hide");
			document.body.classList.remove("noscroll");
		}, 300); // 300ms is the duration of the fade-out animation
	} else {
		pop23_popup.classList.add("show");
		// document.body.classList.add("noscroll");
	}
}
