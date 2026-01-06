chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
    if (request.action === "openRunner") {
        chrome.tabs.create({ url: chrome.runtime.getURL("runner.html") });
    }
});

let pollingInterval = null;
const TARGET_URL_PART = "digiheadway.in/autosubmit";
const API_URL = "https://digiheadway.in/autosubmit/";

chrome.tabs.onUpdated.addListener((tabId, changeInfo, tab) => {
    if (changeInfo.status === 'complete' && tab.url && tab.url.includes(TARGET_URL_PART)) {
        startPolling(tabId);
    } else if (changeInfo.status === 'complete' && tab.url && !tab.url.includes(TARGET_URL_PART)) {
        // If the tab navigates away (e.g. to the Google Form), we might want to stop polling
        // strictly speaking, if we only have one interval, we should stop it if the *active* polling tab changes.
        // But for now, let's just let the startPolling handle the reset.
        // If we want to be stricter:
        // if (pollingInterval) clearInterval(pollingInterval);
    }
});

function startPolling(tabId) {
    if (pollingInterval) clearInterval(pollingInterval);
    console.log("Starting polling for tab", tabId);

    pollingInterval = setInterval(() => {
        // Check if tab still exists and is on the right URL (optional, but good for safety)
        chrome.tabs.get(tabId, (tab) => {
            if (chrome.runtime.lastError || !tab) {
                clearInterval(pollingInterval);
                return;
            }

            // If the user manually navigated away, stop polling
            if (!tab.url.includes(TARGET_URL_PART)) {
                clearInterval(pollingInterval);
                return;
            }

            // Add timestamp to prevent caching
            const pollUrl = `${API_URL}?t=${Date.now()}`;
            fetch(pollUrl)
                .then(response => response.json())
                .then(data => {
                    // console.log("Polled data:", data);
                    if (data.url && data.url.includes("https")) {
                        console.log("Auto Submitter: Target URL found:", data.url);
                        console.log("Auto Submitter: Saving data to storage:", data);

                        // Save data to storage so content script can use it
                        chrome.storage.local.set({ 'autoSubmitData': data }, () => {
                            console.log("Auto Submitter: Data saved, redirecting...");
                            chrome.tabs.update(tabId, { url: data.url });
                        });
                        // Polling will stop on next tick because URL check will fail
                    }
                })
                .catch(err => console.error("Polling error:", err));
        });
    }, 1000);
}
