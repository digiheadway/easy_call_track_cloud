document.addEventListener('DOMContentLoaded', async () => {
  const statusElement = document.getElementById('status');
  const profileSelect = document.getElementById('profile-select');
  const chkAutoFill = document.getElementById('chk-auto-fill');
  const chkAutoNext = document.getElementById('chk-auto-next');
  const chkAutoSubmit = document.getElementById('chk-auto-submit');
  const btnGetReady = document.getElementById('btn-get-ready');
  const btnDirectForm = document.getElementById('btn-direct-form');

  const btnToggle = document.getElementById('btn-toggle-automation');

  // Load saved settings
  const stored = await chrome.storage.local.get(['profile', 'autoFill', 'autoNext', 'autoSubmit']);
  if (stored.profile) profileSelect.value = stored.profile;
  if (stored.autoFill !== undefined) {
    chkAutoFill.checked = stored.autoFill;
    updateToggleButton(stored.autoFill);
  }
  if (stored.autoNext !== undefined) chkAutoNext.checked = stored.autoNext;
  if (stored.autoSubmit !== undefined) chkAutoSubmit.checked = stored.autoSubmit;

  function updateToggleButton(isActive) {
    if (isActive) {
      btnToggle.textContent = "WORKING";
      btnToggle.classList.add('working');
    } else {
      btnToggle.textContent = "STOPPED";
      btnToggle.classList.remove('working');
    }
  }

  // Save settings helper
  function saveSettings() {
    return new Promise((resolve) => {
      const settings = {
        profile: profileSelect.value,
        autoFill: chkAutoFill.checked,
        autoNext: chkAutoNext.checked,
        autoSubmit: chkAutoSubmit.checked
      };
      chrome.storage.local.set(settings, () => resolve(settings));
    });
  }

  // Toggle Button Listener
  btnToggle.addEventListener('click', async () => {
    chkAutoFill.checked = !chkAutoFill.checked;
    updateToggleButton(chkAutoFill.checked);
    await saveSettings();
  });

  // Clear previous state helper
  function clearPreviousState() {
    return new Promise((resolve) => {
      chrome.storage.local.remove(['as_state', 'autoSubmitData'], resolve);
    });
  }

  // Event Listeners for Settings
  [profileSelect, chkAutoFill, chkAutoNext, chkAutoSubmit].forEach(el => {
    el.addEventListener('change', () => {
      if (el === chkAutoFill) updateToggleButton(chkAutoFill.checked);
      saveSettings();
    });
  });

  // "Get Ready" Button
  btnGetReady.addEventListener('click', async () => {
    await saveSettings();
    await clearPreviousState();
    updateStatus("Opening target URL...");
    chrome.tabs.create({ url: "https://digiheadway.in/autosubmit/2.php" });
  });

  // "Direct Form" Button
  btnDirectForm.addEventListener('click', async () => {
    await saveSettings();
    await clearPreviousState();
    updateStatus("Fetching data & opening form...");

    // Send message to background to fetch data immediately and then open form
    chrome.runtime.sendMessage({ action: "directFormAccess", profile: profileSelect.value });
  });

  function updateStatus(message) {
    statusElement.innerText = message;
  }
});
