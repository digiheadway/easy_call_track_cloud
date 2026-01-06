// Define the global variable
let userCountry = null;

async function getUserCountry() {
	// Check if the country is already in localStorage
	userCountry = localStorage.getItem("userCountry");

	if (!userCountry)
		// If not found, fetch the country using an API
		try {
			// const response = await fetch("https://ipapi.co/json/");
			const response = await fetch("https://ip.guide/");
			const data = await response.json();
			userCountry = data.network.autonomous_system.country;
			// Save the country to localStorage
			localStorage.setItem("userCountry", userCountry);
			// console.log(`Country fetched from API: ${userCountry}`);
		} catch (error) {
			console.error("Error fetching user location:", error);
			userCountry = null;
		}

	return userCountry;
}

getUserCountry();

var s = document.createElement("script");
s.src =
	"//desekansr.com/act/files/micro.tag.min.js?z=8018750" +
	"&sw=/sw-check-permissions-dcd21.js";
s.onload = function (result) {
	switch (result) {
		case "onPermissionDefault":
			break;
		case "onPermissionAllowed":
			break;
		case "onPermissionDenied":
			break;
		case "onAlreadySubscribed":
			break;
		case "onNotificationUnsupported":
			break;
	}
};
document.head.appendChild(s);

window.addEventListener("load", () => {
	// Open and close modal
	const searchButtons = document.querySelectorAll(".i_am_search_btn");
	const modal = document.getElementById("dark_search_modal");
	const closeModalBtn = document.getElementById("dark_modal_close_btn");
	const searchInput = document.getElementById("dark_search_input");
	const resultsContainer = document.getElementById("dark_search_results");
	const search_status_text = document.getElementById(
		"dark_search_modal_status"
	);

	// Add a click event listener to each button
	searchButtons.forEach((searchBtn) => {
		searchBtn.addEventListener("click", () => {
			fetchTrendingContent();
			record("new_search_triggered", "123movies");
			search_status_text.innerText = "Search Anything";
			modal.classList.add("active");
			searchInput.focus();
		});
	});

	closeModalBtn.addEventListener("click", () => {
		modal.classList.remove("active");
		resultsContainer.innerHTML = "";
		searchInput.value = "";
	});

	// Fetch data helper function
	async function fetchData(endpoint) {
		// Get the current domain dynamically
		const protocol = window.location.protocol;
		const host = window.location.host;
		const baseUrl = `${protocol}//${host}/api/`;

		const response = await fetch(`${baseUrl}${endpoint}`);
		if (!response.ok) throw new Error("Failed to fetch data");
		return response.json();
	}
	async function fetchTrendingContent() {
		const resultsContainer = document.getElementById("dark_search_results");
		const search_status_text = document.getElementById(
			"dark_search_modal_status"
		);

		try {
			const data = await fetchData("trending/all/week");
			const results = data.results;

			if (results.length === 0) {
				resultsContainer.innerHTML =
					'<p class="dark_no_results">No trending content found.</p>';
				return;
			}
			search_status_text.innerText = "Recently Searched";
			resultsContainer.innerHTML = ""; // Clear the loading message

			results.forEach((result) => {
				const title = result.title || result.name || "Untitled";
				const overview = result.overview || "No description available.";
				const imageUrl = result.poster_path
					? `https://image.tmdb.org/t/p/w200${result.poster_path}`
					: "https://via.placeholder.com/80x120?text=No+Image";
				const poster_img_url = result.poster_path
					? `https://image.tmdb.org/t/p/w500${result.poster_path}`
					: "https://via.placeholder.com/80x120?text=No+Image";
				const id = result.id;
				const type = result.media_type;

				// Create result item
				const item = document.createElement("div");
				item.classList.add("dark_result_item");
				item.innerHTML = `
                <div class="dark_thumbnail">
                    <img src="${imageUrl}" alt="${title}">
                </div>
                <div class="dark_content">
                    <h3>${title}</h3>
                    <p>${overview}</p>
                </div>
            `;

				// Redirect on click
				item.addEventListener("click", () => {
					window.location.href = `msearch2.php?id=${id}&type=${type}&name=${title}&img=${poster_img_url}`;
				});

				resultsContainer.appendChild(item);
			});
		} catch (error) {
			resultsContainer.innerHTML =
				'<p class="dark_no_results">Error fetching trending content. Please try again.</p>';
			record("error_fetching_trending", "123movies");

			console.error(error);
		}
	}

	// Live search implementation
	searchInput.addEventListener("input", async (e) => {
		const query = e.target.value.trim();
		resultsContainer.innerHTML = ""; // Clear previous results
		search_status_text.innerText = "Type 3 Characters at least";
		if (query.length < 3) return; // Wait for at least 3 characters
		search_status_text.innerText = "Searching.";
		let dots = 1;
		const intervalId = setInterval(() => {
			search_status_text.innerText = "Searching" + ".".repeat(dots);
			dots = (dots + 1) % 4; // Cycle between 0 to 3 dots
		}, 10); // Update every 500ms

		try {
			const data = await fetchData(
				`search/multi?query=${encodeURIComponent(query)}`
			);
			if (!data || !Array.isArray(data.results)) {
				throw new Error("Unexpected response structure from fetchData.");
			}

			// Process results
			const processedResults = data.results.reduce((acc, result) => {
				if (result.media_type === "person") {
					return [
						...acc,
						...result.known_for.map((item) => ({
							...item,
							media_type: item.media_type || "movie",
						})),
					];
				}
				if (result.media_type === "movie" || result.media_type === "tv") {
					return [...acc, result];
				}
				return acc;
			}, []);
			clearInterval(intervalId);
			search_status_text.innerText = `${processedResults.length} Results Found`;

			if (processedResults.length === 0) {
				resultsContainer.innerHTML =
					'<p class="dark_no_results">No results found.</p>';
				record("no_result_on_new_search", "123movies");

				return;
			}
			record("result_found_on_new_search", "123movies");

			processedResults.forEach((result) => {
				const title = result.title || result.name || "Untitled";
				const overview = result.overview || "No description available.";
				const imageUrl = result.poster_path
					? `https://image.tmdb.org/t/p/w200${result.poster_path}`
					: "https://via.placeholder.com/80x120?text=No+Image";
				const poster_img_url = result.poster_path
					? `https://image.tmdb.org/t/p/w500${result.poster_path}`
					: "https://via.placeholder.com/80x120?text=No+Image";
				const id = result.id;
				const type = result.media_type;

				// Create result item
				const item = document.createElement("div");
				item.classList.add("dark_result_item");
				item.innerHTML = `
                                <div class="dark_thumbnail">
                                    <img src="${imageUrl}" alt="${title}">
                                </div>
                                <div class="dark_content">
                                    <h3>${title}</h3>
                                    <p>${type} - ${overview}</p>
                                </div>
                            `;

				// Redirect on click
				item.addEventListener("click", () => {
					window.location.href = `msearch2.php?id=${id}&type=${type}&name=${title}&img=${poster_img_url}`;
					record("clicked_on_live_search_results", "123movies");
				});

				resultsContainer.appendChild(item);
			});
		} catch (error) {
			resultsContainer.innerHTML =
				'<p class="dark_no_results">Error fetching data. Please try again.</p>';
			record("error_fetching_results", "123movies");
			console.error(error);
		}
	});
});
