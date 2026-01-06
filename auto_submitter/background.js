const TARGET_URL_PART = "digiheadway.in/autosubmit/2.php";
const API_URL = "https://digiheadway.in/autosubmit/2.php";
const FORM_URL = "https://forms.gle/EzcfReSWR7VEpcRRA";

let pollingInterval = null;

// Listen for messages from popup
chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
    if (request.action === "directFormAccess") {
        handleDirectFormAccess(request.profile);
    }
});

// Listen for tab updates to start polling
chrome.tabs.onUpdated.addListener((tabId, changeInfo, tab) => {
    if (changeInfo.status === 'complete' && tab.url && tab.url.includes(TARGET_URL_PART)) {
        startPolling(tabId);
    } else if (changeInfo.status === 'complete' && tab.url && !tab.url.includes(TARGET_URL_PART)) {
        // Stop polling if navigated away from target
        if (pollingInterval) {
            // We check if the active tab is the one we were polling, but for simplicity, 
            // if we are not on the target URL anymore, we stop.
            // A more robust solution would track tabId.
            // For now, we rely on the check inside setInterval.
        }
    }
});

async function startPolling(tabId) {
    if (pollingInterval) clearInterval(pollingInterval);
    console.log("Starting polling for tab", tabId);

    // Get profile from storage
    const stored = await chrome.storage.local.get(['profile']);
    const profileId = stored.profile || "1";

    pollingInterval = setInterval(() => {
        chrome.tabs.get(tabId, (tab) => {
            if (chrome.runtime.lastError || !tab) {
                clearInterval(pollingInterval);
                return;
            }

            if (!tab.url.includes(TARGET_URL_PART)) {
                clearInterval(pollingInterval);
                return;
            }

            // Poll with profile param
            const pollUrl = `${API_URL}?profile=${profileId}&t=${Date.now()}`;
            fetch(pollUrl)
                .then(response => response.json())
                .then(data => {
                    if (data.url && data.url.includes("https")) {
                        console.log("Auto Submitter: Target URL found:", data.url);
                        processAndSaveData(data, profileId, () => {
                            console.log("Auto Submitter: Data saved, redirecting...");
                            chrome.tabs.update(tabId, { url: data.url });
                        });
                    }
                })
                .catch(err => console.error("Polling error:", err));
        });
    }, 300); // 300ms interval
}

function handleDirectFormAccess(profileId) {
    console.log("Auto Submitter: Direct Form Access for profile", profileId);

    // Open form immediately
    chrome.tabs.create({ url: FORM_URL });

    // Fetch data in parallel
    const pollUrl = `${API_URL}?profile=${profileId}&t=${Date.now()}`;

    fetch(pollUrl)
        .then(response => response.json())
        .then(data => {
            console.log("Auto Submitter: Fetched data for direct access:", data);
            processAndSaveData(data, profileId, () => {
                console.log("Auto Submitter: Data saved to storage.");
            });
        })
        .catch(err => console.error("Direct access fetch error:", err));
}

// Listen for profile changes to re-filter existing data
chrome.storage.onChanged.addListener((changes, area) => {
    if (area === 'local' && changes.profile) {
        const newProfileId = changes.profile.newValue;
        console.log("Auto Submitter: Profile changed to", newProfileId, "- Re-filtering data...");

        chrome.storage.local.get(['rawAutoSubmitData'], (result) => {
            if (result.rawAutoSubmitData) {
                filterAndSave(result.rawAutoSubmitData, newProfileId);
            } else {
                console.warn("Auto Submitter: No raw data found to re-filter.");
            }
        });
    }
});

function processAndSaveData(rawData, profileId, callback) {
    console.log("Auto Submitter: Saving raw data and processing for profile:", profileId);

    // Save raw data for future re-filtering
    chrome.storage.local.set({ 'rawAutoSubmitData': rawData });

    filterAndSave(rawData, profileId, callback);
}

function filterAndSave(rawData, profileId, callback) {
    let filteredData = rawData;

    if (rawData.data && Array.isArray(rawData.data) && rawData.data.length > 0) {
        // If profileId is "0", we show ALL data (no filtering)
        if (String(profileId) === "0") {
            console.log("Auto Submitter: Profile is 0 (All). Skipping filtering.");
            filteredData = rawData;
        } else {
            // Find the profile key (case-insensitive)
            const firstRow = rawData.data[0];
            const profileKey = Object.keys(firstRow).find(k => k.toLowerCase() === 'profile');

            if (profileKey) {
                console.log(`Auto Submitter: Filtering by column '${profileKey}' for value '${profileId}'`);
                const rows = rawData.data.filter(row => {
                    const val = row[profileKey];
                    return String(val).trim() === String(profileId).trim();
                });

                console.log(`Auto Submitter: Filtered down to ${rows.length} rows.`);
                filteredData = { ...rawData, data: rows };
            } else {
                console.warn("Auto Submitter: 'profile' column not found. Keeping all data.");
            }
        }
    } else {
        console.warn("Auto Submitter: No data rows found.");
    }

    console.log("Auto Submitter: Saving filtered data to storage.");
    chrome.storage.local.set({ 'autoSubmitData': filteredData }, callback);
}
