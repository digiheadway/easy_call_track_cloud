const urlParams = new URLSearchParams(window.location.search);
let query = urlParams.get("q");

if (window.location.pathname.includes("msearch.php")) {
	if (!query) {
		const cookie = document.cookie.match(/(^|[^;]+)q=([^;]*)/);
		query = cookie ? cookie[2] : null;
	}
}

if (query) {
	const url = `/inc/php/search-debug.php?q=${query}`;
	fetch(url)
		.then((response) => response.json())
		.then((data) => {
			if (data && data.imageUrl) {
				const imageUrl = data.imageUrl;
				document.getElementById("loader_img").src = imageUrl;
				const img_loaded = new CustomEvent("img_loaded");
				document.dispatchEvent(img_loaded);
			} else if (data && data.corrected_query) {
				// Get the current URL
				const corrected_query = data.corrected_query;
				const currentUrl = new URL(window.location.href);
				currentUrl.searchParams.set("q", corrected_query);
				window.location.href = currentUrl.toString();
				// console.log(currentUrl.toString());
			} else {
				console.log("Invalid response data:", data);
			}
		})
		.catch((error) => console.log("Fetch error:", error));
} else {
	console.log('No "q" found');
}

async function not_this_save() {
	// Get the value of the 'q' parameter from the URL
	const urlParams = new URLSearchParams(window.location.search);
	const queryToCheck = urlParams.get("q") || ""; // Default to an empty string if 'q' is not present
	const apiUrl = `inc/php/not_this.php?q=${encodeURIComponent(queryToCheck)}`;
	const response = await fetch(apiUrl, {
		method: "GET",
		headers: {
			"Content-Type": "text/html; charset=UTF-8",
		},
	});

	const data = await response.text();
	console.log(data);
}

async function down_tried(btn_type) {
	// Check if the function has been triggered previously
	if (!localStorage.getItem("down_tried_triggered")) {
		// Get the value of the 'q' parameter from the URL
		const urlParams = new URLSearchParams(window.location.search);
		const queryToCheck = urlParams.get("q") || ""; // Default to an empty string if 'q' is not present

		const apiUrl = `inc/php/down_tried.php?q=${encodeURIComponent(
			queryToCheck
		)}`;
		const response = await fetch(apiUrl, {
			method: "GET",
			headers: {
				"Content-Type": "text/html; charset=UTF-8",
			},
		});

		const data = await response.text();
		console.log(data);
		gtag("event", "down_tried", {
			event_category: btn_type,
			event_label: "download_tried",
			// Additional event parameters if needed
		});
		gtag("event", "purchase", {
			value: 1.02,
			tax: 0.01,
			shipping: 0.01,
			currency: "INR",
			coupon: btn_type,
			items: [
				{
					item_id: "M_" + query,
					item_name: query,
					item_brand: urlParams.get("utm_source"),
					affiliation: urlParams.get("dm"),
					promotion_name: urlParams.get("utm_medium"),
					price: 1,
					quantity: 1,
				},
			],
		});

		// Set a flag in localStorage to indicate that the function has been triggered
		localStorage.setItem("down_tried_triggered", "true");
	} else {
		console.log("down_tried function has already been triggered.");
	}
}

function search_page_viewed() {
	// Check if the event has been triggered previously
	if (!localStorage.getItem("search_page_viewed")) {
		// Your GA4 event tracking code here
		gtag("event", "search_page_viewed", {
			event_category: "Search",
			event_label: "Page Viewed",
			// Additional event parameters if needed
		});

		// Set a flag in localStorage to indicate that the event has been triggered
		localStorage.setItem("search_page_viewed", "true");
	} else {
		console.log(
			"GA4 event search_page_viewed has already been triggered for this user."
		);
	}
}

// Call the triggerGA4Event function when the search page is viewed
search_page_viewed();

function unique_visitor() {
	// Check if the event has been triggered previously
	if (!localStorage.getItem("unique_visitor")) {
		// Your GA4 event tracking code here
		gtag("event", "unique_visitor", {
			event_category: "view",
			event_label: "Page Viewed",
			// Additional event parameters if needed
		});

		// Set a flag in localStorage to indicate that the event has been triggered
		localStorage.setItem("unique_visitor", "true");
	} else {
		console.log(
			"GA4 event unique_visitor has already been triggered for this user."
		);
	}
}

// Call the triggerGA4Event function when the search page is viewed
unique_visitor();

function shareCurrentUrl() {
	if (navigator.share) {
		// Use the Web Share API if it's supported by the browser
		navigator
			.share({
				title: "Share Title",
				text: "Check out this amazing content!",
				url: window.location.href,
			})
			.then(() => {
				console.log("Successfully shared.");
			})
			.catch((error) => {
				console.log("Sharing failed:", error);
			});
	} else {
		// Fallback behavior if the Web Share API is not supported
		console.log("Web Share API not supported.");
		// Perform any custom sharing action or display a message to the user
	}
}
// Add a listener for the custom event
document.addEventListener("img_loaded", function () {
	const play_icon = document.querySelector(".v3_movies_play_icon");
	if (play_icon) {
		play_icon.style.display = "block";
	}
});

function showLoadingOverlay() {
	var overlay = document.getElementById("overlay455_overlay");
	overlay.style.display = "block"; // Show the overlay

	// Simulate a loading process (you can replace this with your actual loading logic)
	setTimeout(function () {
		overlay.style.display = "none"; // Hide the overlay after some time
	}, 1000); // 3000 milliseconds (3 seconds) in this example
}
// Function for Not This text
document.getElementById("not_this_text").addEventListener("click", function () {
	not_this_save();
	this.textContent = "Search Again More Precisely";
	this.style.color = "#3eb893";
	this.style.marginTop = "20px";

	//  document.getElementById("hambergar").checked = true;
	// Check if the device is a PC (desktop)
	if (window.innerWidth < 568) {
		document.getElementById("not_this_searchbox").style.display = "block";
		document.getElementById("hero_search").focus();
	}
});

gtag("event", "view_item", {
	currency: "INR",
	items: [
		{
			item_id: "M_" + query,
			item_name: query,
			item_brand: urlParams.get("utm_source"),
			affiliation: urlParams.get("dm"),
			promotion_name: urlParams.get("utm_medium"),
			price: 1,
			quantity: 1,
		},
	],
});

var isAndroid = /(android)/i.test(navigator.userAgent);

function upage_accesible() {
	if (isind && isAndroid) {
		var now = new Date();
		var expires = new Date(now.getTime() + 24 * 60 * 60 * 1000);
		var expiresUTC = expires.toUTCString();
		document.cookie = "ir=1; expires=" + expiresUTC + "; path=/";
	}
}

var link_if_installed;

if (typeof teraburl !== "undefined") {
	if (!isind || !isAndroid) {
		// If 'isind' is not defined or not an Android device, use teraburl with query
		link_if_installed = teraburl + "?q=" + query;
	} else {
		// If 'isind' is defined and the device is an Android device, use uninstall_tera.php with query
		link_if_installed = "uninstall_tera.php?q=" + query;
	}
}

function check_if_already_downloaded() {
	const lastRunTime = localStorage.getItem("last_installed_check");
	const oneHour = 60 * 5 * 1000; // 6 minute in milliseconds

	if (lastRunTime && Date.now() - lastRunTime < oneHour) {
		return;
	}
	if (!isAndroid) {
		return;
	}

	const timeout = setTimeout(() => {
		console.log("Not Installed");
		localStorage.setItem("last_installed_check", Date.now());
		localStorage.setItem("install_result", "Not Installed");
	}, 1000);

	const blurEvent = () => {
		clearTimeout(timeout);
		console.log("Installed");
		localStorage.setItem("last_installed_check", Date.now());
		localStorage.setItem("install_result", "Inst");
		upage_accesible();
		already_installed_terabox();
		gtag("event", "already_installed_terabox");
	};
	window.addEventListener("blur", blurEvent);
	const timeoutRemove = setTimeout(() => {
		window.removeEventListener("blur", blurEvent);
	}, 1000);

	console.log("Checking Installation..");
	window.location = "dubox://check";
}

function already_installed_terabox() {
	// Code to run on page load if the application is installed
	not_tera_links();
	// document.getElementById("popup_tab_app").style.display = "none";
	// document.getElementById("url_shorten_link").href = link_if_installed;
}

if (localStorage.getItem("install_result") === "Inst") {
	// If the condition is true, run the already_installed_terabox function
	already_installed_terabox();
}

async function calling_country_based_settings(retries = 25) {
	let userCountry = await getUserCountry();
	if (userCountry) {
		var countrygroup1 = ["AE", "SA", "IN", "KR"];
		if (!countrygroup1.includes(userCountry)) {
			not_tera_links();
			gtag("event", "country_not_tera");
		} else {
			// localStorage.setItem("tj2", "true");
			localStorage.setItem("tera_countries", "true");
			// not_tera_links();
		}
	} else {
		if (retries > 0) {
			setTimeout(() => calling_country_based_settings(retries - 1), 200); // Retry after 200 ms
		} else {
			console.error("Failed to get user country after multiple attempts.");
			gtag("event", "country_get_failed");
		}
	}
}

// Example usage
calling_country_based_settings();

//  var secondaryLinks = '<?php echo $secondaryLinks; ?>';
var secondaryLinks = "https://be6.in/mov68_monetag";
var secondaryLinks2 = "https://be6.in/Movie57676-2-bfre-share_us";
var adsteralink = "https://be6.in/mov5667y8_adst_17dec";

function not_tera_links() {
	//  var secondaryLinks = '<?php echo $secondaryLinks; ?>';
	var secondaryLinks = "https://be6.in/mov68_monetag";
	gtag("event", "not_tera_function_triggered");
	// var adsteralink = "https://shorturl.at/IfoOG"; // mohit new adstra via short
	var adsteralink = "https://be6.in/mov5667y8_adst_17dec";
	var share_us_link = "https://be6.in/Movie57676-2-bfre-share_us";
	var adsteralink14 = "https://bit.ly/mov-fgdg-adst-mayank";
	var harleywives = "https://bit.ly/harleywives";
	var awin = "https://be6.in/awin-25";

	// Select the original element with id="popup_tab_shortener"
	var originalElement = document.getElementById("popup_tab_shortener");
	var teraElement = document.getElementById("popup_tab_app");

	// Array of new texts and links
	var newElementsData = [
		{ text: "Google Drive", link: adsteralink },
		{ text: "Dropbox", link: awin },
		{ text: "Mega Drive", link: secondaryLinks },
	];
	document.getElementById("ctr_booster_1").innerHTML =
		"<b>Wait 40 Sec</b> on the <b>destination page</b> for <b>automatic redirection</b> to the resource";
	document.getElementById("ctr_booster_1").style.display = "block";
	// Iterate over the new elements data and create new elements
	newElementsData.forEach(function (data, index) {
		// Clone the original element
		var newElement = originalElement.cloneNode(true);
		newElement.id = "popup_tab_shortener_" + (index + 1);
		newElement.querySelector("span").textContent = data.text;
		newElement.querySelector("a").href = data.link;
		console.log("hey this is" + data.link);
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

document.addEventListener("DOMContentLoaded", async function () {
	// Check if the 'tj2' key exists and is set to true in localStorage
	//  localStorage.setItem("tj2", "true");
	if (localStorage.getItem("tj2") !== "true") {
		return; // If 'tj2' is not true, stop execution
	}
	if (!isAndroid) {
		return;
	}

	// Check if link_id is already in localStorage
	let link_id = localStorage.getItem("assigned_link_id");

	if (!link_id) {
		// Fetch the latest unused link ID from the server if not found in localStorage
		const response = await fetch("/tj2/fetch_link.php");
		const data = await response.json();

		if (data.success && data.link_id) {
			link_id = data.link_id;
			localStorage.setItem("assigned_link_id", link_id); // Store the fetched link_id in localStorage
		} else {
			console.log("No unused link found in the database.");
			return;
		}
	}

	// Construct the market URL
	const marketurl = `market://details?id=com.dubox.drive&referrer=utm_source=tera_sharelink&utm_campaign=header_button&surl=L2GEqKHfs9pzwGD9OQ3i0g&tera_link_id=${link_id}&tera_link_type=1&c=https://www.1024tera.com/wap/share/filelist?surl=L2GEqKHfs9pzwGD9OQ3i0g&af_dp=dubox://share_detail?shareid=58712493527&uk=4398511686747&share_create_time=1734217962&randsk=brxzEUa2yGI6EcWGDP7XmhzSzTmSpCxEVB1x%20viMXdU=&share_expired_time=0&source=af_link&share_file_count=1&surl=L2GEqKHfs9pzwGD9OQ3i0g&agency=j1__0`;

	// Get the action button
	const actionButton = document.getElementById("tera_drive_link");

	// Remove any existing onclick handlers by setting the inline onclick to null (if any)
	actionButton.onclick = null; // Remove inline onclick attribute

	// Attach the new click event listener
	actionButton.addEventListener("click", async function () {
		try {
			// Step 1: Mark the link as used in the database
			const updateResponse = await fetch("/tj2/update_link.php", {
				method: "POST",
				headers: { "Content-Type": "application/json" },
				body: JSON.stringify({ link_id }),
			});

			const updateData = await updateResponse.json();
			if (!updateData.success) {
				throw new Error("Failed to update the link status in the database");
			}

			// Step 2: Copy "{link_id}&type=1" to clipboard
			const textToCopy = `&surl=L2GEqKHfs9pzwGD9OQ3i0g&tera_link_id=${link_id}&tera_link_type=1`;
			await navigator.clipboard.writeText(textToCopy);
			console.log("Copied to clipboard:", textToCopy);

			// Step 3: Open the market URL
			window.open(marketurl, "_blank");

			// Step 4: Push gtag event to Google Analytics
			if (typeof gtag === "function") {
				gtag("event", "button_click", {
					event_category: "Action",
					event_label: marketUrl,
				});
			}
		} catch (err) {
			console.error("Error in processing:", err);
		}
	});
});

const tera_main_link = "https://be6.in/Movie57676-1080p-tera-main";
function open_tera_link() {
	const link_id = localStorage.getItem("link_id");
	var tera_countries = localStorage.getItem("tera_countries");
	var isandroid = /android/i.test(navigator.userAgent);
	// localStorage.setItem("is_direct", "true");

	if (!tera_countries && !isandroid) {
		window.open(tera_main_link, "_blank");
		return;
	}

	if (link_id) {
		open_tera_app(link_id);
		return;
	}
	const is_direct = localStorage.getItem("is_direct");
	if (!is_direct) {
		window.open(tera_main_link, "_blank");
		return;
	}

	fetch("/tj2/get_link_id.php", {
		method: "GET",
		headers: {
			"Content-Type": "application/json",
		},
	})
		.then((response) => response.json())
		.then((data) => {
			// Check if link_id exists in the response
			if (data.link_id) {
				localStorage.setItem("link_id", data.link_id);
				open_tera_app(data.link_id);
			} else {
				window.open(tera_main_link, "_blank");
			}
		})
		.catch((error) => {
			// If there's an error with the request, redirect to g.com
			console.error("Error fetching link_id:", error);
			window.open(tera_main_link, "_blank");
		});
}

// async function open_tera_app(link_id) {
// 	var tera_countries = localStorage.getItem("tera_countries");
// 	var isandroid = /android/i.test(navigator.userAgent);

// 	if (tera_countries && isandroid) {
// 		var marketurl = `market://details?id=com.dubox.drive&referrer=utm_source=tera_sharelink&utm_campaign=header_button&surl=LKnUAElleaenD4yM1XmsaA&tera_link_id=${link_id}&tera_link_type=1&c=https://www.1024tera.com/wap/share/filelist?surl=LKnUAElleaenD4yM1XmsaA&af_dp=dubox://share_detail?shareid=57417742093&uk=4401581855675&share_create_time=1736792771&randsk=/BFeRX88RVQcBA75Pwb/agKB5Bc5bKSJV7sK5Xl4uSE=&share_expired_time=0&source=af_link&share_file_count=1&surl=LKnUAElleaenD4yM1XmsaA&agency=j1__0`;
// 		var textToCopy = `&surl=L2GEqKHfs9pzwGD9OQ3i0g&tera_link_id=${link_id}&tera_link_type=1`;
// 		navigator.clipboard.writeText(textToCopy);
// 		window.location.href = marketurl;
// 	} else {
// 		window.open(tera_main_link, "_blank");
// 	}
// }

async function open_tera_app(link_id) {
	// var marketurl = `market://details?id=com.dubox.drive&referrer=utm_source=tera_sharelink&utm_campaign=header_button&surl=LKnUAElleaenD4yM1XmsaA&tera_link_id=${link_id}&tera_link_type=1&c=https://www.terabox.club/wap/share/filelist?surl=LKnUAElleaenD4yM1XmsaA&af_dp=dubox://share_detail?shareid=57417742093&uk=4401581855675&share_create_time=1736792771&randsk=/BFeRX88RVQcBA75Pwb/agKB5Bc5bKSJV7sK5Xl4uSE=&share_expired_time=0&source=af_link&share_file_count=1&surl=LKnUAElleaenD4yM1XmsaA&agency=j1__0`;
	var marketurl = `market://details?id=com.dubox.drive&referrer=utm_source%253Dtera_sharelink%2526utm_campaign%253Dheader_button%2526surl%253DLKnUAElleaenD4yM1XmsaA%2526tera_link_id%253D${link_id}%2526tera_link_type%253D1%2526c%253Dhttps%25253A%25252F%25252Fwww.terabox.app%25252Fwap%25252Fshare%25252Ffilelist%25253Fsurl%25253DLKnUAElleaenD4yM1XmsaA%2526af_dp%253Ddubox%25253A%25252F%25252Fshare_detail%25253Fshareid%25253D57417742093%252526uk%25253D4401581855675%252526share_create_time%25253D1736792771%252526randsk%25253D%2525252FBFeRX88RVQcBA75Pwb%2525252FagKB5Bc5bKSJV7sK5Xl4uSE%2525253D%252526share_expired_time%25253D0%252526source%25253Daf_link%252526share_file_count%25253D1%252526surl%25253DLKnUAElleaenD4yM1XmsaA%2526agency%253Dj1__0`;
	var textToCopy = `&surl=LKnUAElleaenD4yM1XmsaA&tera_link_id=${link_id}&tera_link_type=1`;
	navigator.clipboard.writeText(textToCopy);
	window.location.href = marketurl;
}
