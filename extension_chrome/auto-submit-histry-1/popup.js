document.addEventListener('DOMContentLoaded', () => {
  const statusElement = document.getElementById('status');

  chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
    const activeTab = tabs[0];

    if (!activeTab || !activeTab.id) {
      updateStatus("No active tab found.", false);
      return;
    }

    // Try to execute a script in the active tab
    chrome.scripting.executeScript({
      target: { tabId: activeTab.id },
      func: () => {
        // This function runs in the context of the web page
        return !!document.body;
      }
    }, (results) => {
      if (chrome.runtime.lastError) {
        // If there's an error (e.g., restricted page like chrome://), we can't access the DOM
        console.warn("Access denied:", chrome.runtime.lastError.message);
        updateStatus("Cannot read this website", false);
      } else if (results && results[0] && results[0].result) {
        updateStatus("I can read this website", true);
      } else {
        updateStatus("Cannot read this website", false);
      }
    });
  });

  function updateStatus(message, success) {
    statusElement.innerText = message;
    if (!success) {
      statusElement.style.background = "linear-gradient(135deg, #f87171 0%, #f472b6 100%)";
      statusElement.style.webkitBackgroundClip = "text";
      statusElement.style.webkitTextFillColor = "transparent";
    } else {
        // Reset to success gradient
        statusElement.style.background = "linear-gradient(135deg, #60a5fa 0%, #34d399 100%)";
        statusElement.style.webkitBackgroundClip = "text";
        statusElement.style.webkitTextFillColor = "transparent";
    }
  }
});
