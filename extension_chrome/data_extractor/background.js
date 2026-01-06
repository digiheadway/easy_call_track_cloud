let savePromise = Promise.resolve();

chrome.runtime.onInstalled.addListener(() => {
    console.log('Data Extractor Extension Installed');
    chrome.storage.local.set({ collectedData: [], extractionActive: false });
});

chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
    if (request.action === 'save_data') {
        handleSaveData(request.data).then((isNew) => {
            sendResponse({ success: true, isNew });
        });
        return true; // Keep channel open for async response
    }
});

function handleSaveData(newItem) {
    savePromise = savePromise.then(async () => {
        const result = await chrome.storage.local.get(['collectedData']);
        const currentData = result.collectedData || [];

        // Check uniqueness again (global check)
        if (currentData.some(item => item.value === newItem.value)) {
            return false;
        }

        const updatedData = [...currentData, newItem];
        await chrome.storage.local.set({ collectedData: updatedData });
        return true;
    });
    return savePromise;
}
