chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
    if (request.action === "openRunner") {
        chrome.tabs.create({ url: chrome.runtime.getURL("runner.html") });
    }
});
