document.addEventListener('DOMContentLoaded', () => {
  const startBtn = document.getElementById('startBtn');
  const stopBtn = document.getElementById('stopBtn');
  const detectPhoneCheckbox = document.getElementById('detectPhone');
  const detectEmailCheckbox = document.getElementById('detectEmail');

  const phoneTabCount = document.getElementById('phoneTabCount');
  const emailTabCount = document.getElementById('emailTabCount');

  const phonesTable = document.getElementById('phonesTable');
  const emailsTable = document.getElementById('emailsTable');

  const downloadPhones = document.getElementById('downloadPhones');
  const downloadEmails = document.getElementById('downloadEmails');
  const clearPhones = document.getElementById('clearPhones');
  const clearEmails = document.getElementById('clearEmails');

  // Phone input controls
  const phoneSettings = document.getElementById('phoneSettings');
  const minDigitsInput = document.getElementById('minDigits');
  const maxDigitsInput = document.getElementById('maxDigits');

  let currentTab = 'phones';

  // 1. Initialize UI visibility immediately based on HTML default state
  togglePhoneSettings();

  // 2. Load actual settings from storage (async)
  loadSettings();
  updateUI();

  // Tab switching
  document.querySelectorAll('.tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      const tab = btn.dataset.tab;
      switchTab(tab);
    });
  });

  function switchTab(tab) {
    currentTab = tab;
    // Update tab buttons
    document.querySelectorAll('.tab-btn').forEach(btn => {
      btn.classList.toggle('active', btn.dataset.tab === tab);
    });
    // Update tab content
    document.querySelectorAll('.tab-content').forEach(content => {
      content.classList.toggle('active', content.id === `${tab}-tab`);
    });
  }

  // Listen for storage changes
  chrome.storage.onChanged.addListener((changes, namespace) => {
    if (namespace === 'local') {
      if (changes.collectedData) {
        updateUI();
      }
      if (changes.extractionActive) {
        updateButtonState(changes.extractionActive.newValue);
      }
    }
  });

  // Phone settings visibility
  detectPhoneCheckbox.addEventListener('change', () => {
    togglePhoneSettings();
    saveSettings();
  });

  detectEmailCheckbox.addEventListener('change', saveSettings);

  function togglePhoneSettings() {
    if (detectPhoneCheckbox && phoneSettings) {
      if (detectPhoneCheckbox.checked) {
        phoneSettings.classList.add('visible');
      } else {
        phoneSettings.classList.remove('visible');
      }
    }
  }

  // --- Input Logic ---

  // Validation to ensure positive numbers and min <= max
  minDigitsInput.addEventListener('change', () => {
    let minVal = parseInt(minDigitsInput.value);
    let maxVal = parseInt(maxDigitsInput.value);

    if (minVal < 1) minVal = 1;
    if (minVal > 20) minVal = 20;

    if (minVal > maxVal) {
      minVal = maxVal;
    }

    minDigitsInput.value = minVal;
    saveSettings();
  });

  maxDigitsInput.addEventListener('change', () => {
    let minVal = parseInt(minDigitsInput.value);
    let maxVal = parseInt(maxDigitsInput.value);

    if (maxVal < 1) maxVal = 1;
    if (maxVal > 20) maxVal = 20;

    if (maxVal < minVal) {
      maxVal = minVal;
    }

    maxDigitsInput.value = maxVal;
    saveSettings();
  });

  function saveSettings() {
    chrome.storage.local.set({
      detectPhone: detectPhoneCheckbox.checked,
      detectEmail: detectEmailCheckbox.checked,
      minPhoneDigits: parseInt(minDigitsInput.value),
      maxPhoneDigits: parseInt(maxDigitsInput.value)
    });
  }

  function loadSettings() {
    chrome.storage.local.get(['detectPhone', 'detectEmail', 'minPhoneDigits', 'maxPhoneDigits'], (result) => {
      detectPhoneCheckbox.checked = result.detectPhone !== false;
      detectEmailCheckbox.checked = result.detectEmail !== false;

      const minDigits = result.minPhoneDigits || 10;
      const maxDigits = result.maxPhoneDigits || 12;

      minDigitsInput.value = minDigits;
      maxDigitsInput.value = maxDigits;

      togglePhoneSettings();
    });
  }

  // --- End Input Logic ---

  startBtn.addEventListener('click', () => {
    chrome.storage.local.set({ extractionActive: true });
    sendMessageToActiveTab('start_extraction');
    window.close();
  });

  stopBtn.addEventListener('click', () => {
    chrome.storage.local.set({ extractionActive: false });
    sendMessageToActiveTab('stop_extraction');
  });

  async function sendMessageToActiveTab(action) {
    const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
    if (tab) {
      chrome.tabs.sendMessage(tab.id, { action }, (response) => {
        if (chrome.runtime.lastError) {
          // Content script might not be ready
        }
      });
    }
  }

  async function updateButtonState(isActive) {
    if (isActive) {
      startBtn.classList.add('hidden');
      stopBtn.classList.remove('hidden');
    } else {
      startBtn.classList.remove('hidden');
      stopBtn.classList.add('hidden');
    }
  }

  // Download handlers
  downloadPhones.addEventListener('click', async () => {
    const data = await getStoredData();
    const phones = data.filter(item => item.type === 'phone');
    if (phones.length === 0) return;
    downloadCSV(phones, 'phones');
  });

  downloadEmails.addEventListener('click', async () => {
    const data = await getStoredData();
    const emails = data.filter(item => item.type === 'email');
    if (emails.length === 0) return;
    downloadCSV(emails, 'emails');
  });

  function downloadCSV(data, type) {
    const csvContent = "ID,Value,Context,Timestamp,URL\n"
      + data.map((e, i) => `${i + 1},"${escapeCSV(e.value)}","${escapeCSV(e.context || 'N/A')}","${e.timestamp || e.date}","${escapeCSV(e.url || e.source)}"`).join("\n");

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);

    const link = document.createElement("a");
    link.setAttribute("href", url);
    link.setAttribute("download", `${type}_${Date.now()}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  function escapeCSV(str) {
    if (!str) return '';
    return String(str).replace(/"/g, '""');
  }

  // Clear handlers
  clearPhones.addEventListener('click', async () => {
    const data = await getStoredData();
    const emails = data.filter(item => item.type === 'email');
    chrome.storage.local.set({ collectedData: emails }, () => {
      updateUI();
    });
  });

  clearEmails.addEventListener('click', async () => {
    const data = await getStoredData();
    const phones = data.filter(item => item.type === 'phone');
    chrome.storage.local.set({ collectedData: phones }, () => {
      updateUI();
    });
  });

  // Delete single row handler
  async function deleteRow(timestamp) {
    const data = await getStoredData();
    const newData = data.filter(item => {
      const itemTime = item.timestamp || item.date;
      return itemTime !== timestamp;
    });

    chrome.storage.local.set({ collectedData: newData }, () => {
      updateUI();
    });
  }

  async function getStoredData() {
    return new Promise((resolve) => {
      chrome.storage.local.get(['collectedData'], (result) => {
        resolve(result.collectedData || []);
      });
    });
  }

  async function updateUI() {
    const data = await getStoredData();

    chrome.storage.local.get(['extractionActive'], (result) => {
      updateButtonState(result.extractionActive);
    });

    const phones = data.filter(item => item.type === 'phone');
    const emails = data.filter(item => item.type === 'email');

    phoneTabCount.textContent = phones.length;
    emailTabCount.textContent = emails.length;

    renderTable(phonesTable, phones, 'phone');
    renderTable(emailsTable, emails, 'email');
  }

  function renderTable(container, data, type) {
    if (data.length === 0) {
      container.innerHTML = `<div class="empty-state">No ${type}s collected yet</div>`;
      return;
    }

    const reversedData = [...data].reverse();

    // Updated Table Header and Row to include URL
    const table = `
      <table class="data-table">
        <thead>
          <tr>
            <th class="col-id">ID</th>
            <th class="col-value">${type === 'phone' ? 'Phone' : 'Email'}</th>
            <th class="col-timestamp">Time</th>
            <th class="col-context">Context</th>
            <th class="col-url">URL</th>
            <th class="col-action"></th> 
          </tr>
        </thead>
        <tbody>
          ${reversedData.map((item, index) => {
      const timeId = item.timestamp || item.date;
      return `
            <tr>
              <td class="col-id">${data.length - index}</td>
              <td class="col-value">
                <div class="value-with-copy">
                  <span class="value-text">${escapeHTML(item.value)}</span>
                  <button class="btn copy" data-value="${escapeHTML(item.value)}">Copy</button>
                </div>
              </td>
              <td class="col-timestamp">${formatTime(timeId)}</td>
              <td class="col-context" title="${escapeHTML(item.context || '')}">${truncate(item.context || 'N/A', 25)}</td>
              <td class="col-url" title="${escapeHTML(item.url || item.source)}">${truncate(item.url || item.source, 20)}</td>
              <td class="col-action">
                <button class="btn-delete" data-id="${timeId}" title="Delete row">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18"></path><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"></path><path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"></path></svg>
                </button>
              </td>
            </tr>
          `}).join('')}
        </tbody>
      </table>
    `;

    container.innerHTML = table;

    container.querySelectorAll('.btn.copy').forEach(btn => {
      btn.addEventListener('click', () => {
        const value = btn.dataset.value;
        copyToClipboard(value, btn);
      });
    });

    container.querySelectorAll('.btn-delete').forEach(btn => {
      btn.addEventListener('click', () => {
        const id = btn.dataset.id;
        deleteRow(id);
      });
    });
  }

  function copyToClipboard(text, button) {
    navigator.clipboard.writeText(text).then(() => {
      const originalText = button.textContent;
      button.textContent = '✓';
      button.classList.add('copied');

      setTimeout(() => {
        button.textContent = originalText;
        button.classList.remove('copied');
      }, 1500);
    }).catch(err => {
      console.error('Failed to copy:', err);
    });
  }

  function formatTime(isoString) {
    if (!isoString) return 'N/A';
    const date = new Date(isoString);
    const now = new Date();
    const diff = now - date;

    if (diff < 60000) return 'Just now';
    if (diff < 3600000) {
      const mins = Math.floor(diff / 60000);
      return `${mins}m ago`;
    }
    if (diff < 86400000) {
      const hours = Math.floor(diff / 3600000);
      return `${hours}h ago`;
    }
    return date.toLocaleDateString();
  }

  function truncate(str, maxLen) {
    if (!str || str.length <= maxLen) return str;
    return str.substring(0, maxLen) + '...';
  }

  function escapeHTML(str) {
    if (!str) return '';
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
  }
});