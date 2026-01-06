let isScanning = false;
let collectedSet = new Set();
let uiContainer = null;
let countElement = null;
let statusElement = null;
let iconElement = null;

// Detection settings (Defaults)
let detectPhone = true;
let detectEmail = true;
let minPhoneDigits = 10;
let maxPhoneDigits = 12;

// Regex patterns
const emailRegex = /[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}/g;

let observer = null;

// Check state on load
chrome.storage.local.get(['extractionActive'], (result) => {
    if (result.extractionActive) {
        startScanning();
    }
});

chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
    if (request.action === 'start_extraction') {
        if (!isScanning) {
            startScanning();
        }
        sendResponse({ status: 'started' });
    } else if (request.action === 'stop_extraction') {
        stopScanning();
        sendResponse({ status: 'stopped' });
    }
});

// LISTEN FOR SETTINGS CHANGES REAL-TIME
chrome.storage.onChanged.addListener((changes, namespace) => {
    if (namespace === 'local') {
        if (changes.collectedData) {
            const newData = changes.collectedData.newValue || [];
            if (newData.length === 0) {
                collectedSet.clear();
            } else {
                newData.forEach(item => collectedSet.add(item.value));
            }
            updateCount(newData.length);
        }

        // Update settings immediately without reload
        if (changes.detectPhone) detectPhone = changes.detectPhone.newValue;
        if (changes.detectEmail) detectEmail = changes.detectEmail.newValue;
        if (changes.minPhoneDigits) minPhoneDigits = parseInt(changes.minPhoneDigits.newValue);
        if (changes.maxPhoneDigits) maxPhoneDigits = parseInt(changes.maxPhoneDigits.newValue);
    }
});

// Helper: Check if an element is TRULY visible to the user
function isVisible(elem) {
    if (!elem) return false;

    // 1. Check dimensions
    const style = window.getComputedStyle(elem);
    if (style.display === 'none' || style.visibility === 'hidden') return false;
    if (style.opacity === '0') return false;

    // 2. Check if it actually takes up screen space
    if (!(elem.offsetWidth || elem.offsetHeight || elem.getClientRects().length)) return false;

    // 3. Check for off-screen positioning (common for hidden data fields)
    const rect = elem.getBoundingClientRect();
    if (rect.left < -1000 || rect.top < -1000) return false;

    // 4. Check aria-hidden
    if (elem.getAttribute('aria-hidden') === 'true') return false;

    return true;
}

// Helper: Detect if text is actually code/JSON (The "Code Guard")
function isCodeOrJson(text) {
    if (!text) return false;
    // Check for common code symbols typically found in JSON or Scripts
    // Matches: {"key":, ["value", function(), var x, return, etc.
    if (text.includes('{"') || text.includes('["') || text.includes('"}') || text.includes('"]')) return true;
    if (text.includes('function(') || text.includes('=>') || text.includes('return ')) return true;
    if (text.includes('var ') || text.includes('const ') || text.includes('let ')) return true;

    // High density of code characters
    const codeChars = (text.match(/[\{\}\[\]\;\<\>]/g) || []).length;
    if (codeChars > 4 && text.length > 20) return true;

    return false;
}

function createUI() {
    if (uiContainer) return;
    uiContainer = document.createElement('div');
    uiContainer.id = 'data-extractor-ui';
    uiContainer.style.cssText = 'position:fixed;bottom:20px;right:20px;z-index:999999;font-family:sans-serif;';

    uiContainer.innerHTML = `
    <style>
      #data-extractor-ui .de-content { background: #fff; box-shadow: 0 4px 12px rgba(0,0,0,0.15); border-radius: 8px; padding: 12px; display: flex; align-items: center; gap: 10px; border: 1px solid #e5e7eb; }
      #data-extractor-ui .de-icon-wrapper { display: flex; align-items: center; justify-content: center; }
      #data-extractor-ui .de-info { font-size: 13px; color: #374151; }
      #data-extractor-ui .de-status { font-weight: 500; color: #6b7280; font-size: 11px; text-transform: uppercase; letter-spacing: 0.5px; }
      #data-extractor-ui .de-count { font-weight: 700; color: #111827; }
      #data-extractor-ui .hidden { display: none; }
    </style>
    <div class="de-content">
      <div class="de-icon-wrapper">
        <svg id="de-search-icon" xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#3b82f6" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
        <svg id="de-check-icon" class="hidden" xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#10b981" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>
      </div>
      <div class="de-info">
        <div class="de-status">Scanning Active</div>
        <div class="de-count">Found: <span id="de-count-val">0</span></div>
      </div>
    </div>
  `;
    document.body.appendChild(uiContainer);
    countElement = document.getElementById('de-count-val');
    statusElement = document.querySelector('.de-status');
    iconElement = document.getElementById('de-search-icon');
}

async function startScanning() {
    isScanning = true;

    // Load settings
    const settings = await chrome.storage.local.get([
        'detectPhone',
        'detectEmail',
        'minPhoneDigits',
        'maxPhoneDigits',
        'collectedData'
    ]);

    detectPhone = settings.detectPhone !== false;
    detectEmail = settings.detectEmail !== false;
    minPhoneDigits = parseInt(settings.minPhoneDigits) || 10;
    maxPhoneDigits = parseInt(settings.maxPhoneDigits) || 12;

    const existingData = settings.collectedData || [];
    existingData.forEach(item => collectedSet.add(item.value));

    createUI();
    updateCount(existingData.length);

    scanNode(document.body);

    observer = new MutationObserver((mutations) => {
        mutations.forEach((mutation) => {
            mutation.addedNodes.forEach((node) => {
                if (node.nodeType === Node.ELEMENT_NODE && isVisible(node)) {
                    scanNode(node);
                }
            });
        });
    });

    observer.observe(document.body, {
        childList: true,
        subtree: true
    });
}

function stopScanning() {
    isScanning = false;
    if (observer) {
        observer.disconnect();
        observer = null;
    }
    if (uiContainer) {
        uiContainer.remove();
        uiContainer = null;
        countElement = null;
        statusElement = null;
        iconElement = null;
    }
}

// Helper: Centralized validation logic for ALL phone sources
function validateAndCleanPhone(rawString) {
    if (!rawString) return null;
    const clean = rawString.trim();

    // Safety check: if the raw string looks like code (e.g. "123456...789"), reject it early
    if (isCodeOrJson(clean)) return null;

    const digits = clean.replace(/\D/g, '');

    // 1. Strict Length Check
    // We use the GLOBAL variables here which are updated by the listener
    if (digits.length < minPhoneDigits || digits.length > maxPhoneDigits) return null;

    // 2. Irrelevant Data Filter
    // Date: YYYY-MM-DD or DD/MM/YYYY
    if (/^\d{4}[-./]\d{2}[-./]\d{2}/.test(clean) || /^\d{2}[-./]\d{2}[-./]\d{4}/.test(clean)) return null;
    // Price/Decimal
    if (/[0-9]+\.[0-9]{2}$/.test(clean)) return null;
    // IP Addresses
    if (/^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$/.test(clean)) return null;
    // Version numbers
    if (/^\d+\.\d+\.\d+$/.test(clean)) return null;
    // Years (strict 4 digits)
    if (digits.length === 4 && (digits.startsWith('19') || digits.startsWith('20'))) return null;
    // Repeated digits
    if (/^(\d)\1+$/.test(digits)) return null;
    // Sequential
    if ('01234567890123456789'.includes(digits)) return null;

    return clean;
}

function scanNode(node, parentContext = '') {
    if (node.nodeType === Node.ELEMENT_NODE) {
        // Expanded Blocklist
        const blockList = ['SCRIPT', 'STYLE', 'NOSCRIPT', 'SVG', 'IMG', 'CODE', 'PRE', 'META', 'LINK', 'TEMPLATE', 'IFRAME', 'HEAD', 'AUDIO', 'VIDEO', 'CANVAS', 'OBJECT'];

        if (blockList.includes(node.tagName) || node.id === 'data-extractor-ui') return;
        if (!isVisible(node)) return;
    }

    if (node.nodeType === Node.TEXT_NODE) {
        if (isVisible(node.parentElement)) {
            processText(node.textContent, node.parentElement);
        }
    }
    else if (node.nodeType === Node.ELEMENT_NODE) {
        // Scan Links
        if (node.tagName === 'A' && node.hasAttribute('href')) {
            const href = node.getAttribute('href');

            if (detectEmail && href.startsWith('mailto:')) {
                const email = href.replace('mailto:', '').split('?')[0];
                addItem(email, 'email', node, 'Link: mailto');
            }
            else if (detectPhone) {
                if (href.startsWith('tel:')) {
                    const rawPhone = href.replace('tel:', '');
                    const validPhone = validateAndCleanPhone(rawPhone);
                    if (validPhone) addItem(validPhone, 'phone', node, 'Link: tel');
                }
                else if (href.includes('wa.me/') || href.includes('whatsapp.com/send')) {
                    const match = href.match(/(?:wa\.me\/|phone=)(\d+)/);
                    if (match && match[1]) {
                        const validPhone = validateAndCleanPhone(match[1]);
                        if (validPhone) addItem(validPhone, 'phone', node, 'Link: WhatsApp');
                    }
                }
            }
        }

        // Scan Inputs
        if (node.tagName === 'INPUT' || node.tagName === 'TEXTAREA') {
            if (node.type !== 'hidden' && node.type !== 'password' && node.type !== 'submit') {
                processText(node.value, node);
                processText(node.placeholder, node);
            }
        }

        // Recursive scan
        node.childNodes.forEach(child => scanNode(child, parentContext));
    }
}

function processText(text, element) {
    if (!text || text.length < 5) return;

    // CODE GUARD: If text looks like JSON or Code, skip entire block
    if (isCodeOrJson(text)) return;

    // Process Email
    if (detectEmail) {
        const emails = text.match(emailRegex);
        if (emails) {
            emails.forEach(email => addItem(email, 'email', element, 'Text content'));
        }
    }

    // Process Phone
    if (detectPhone) {
        // Regex slightly stricter to avoid matching massive numbers in middle of text
        const broadRegex = /(?:\b|\+|\()[\d\-\(\) \.]{6,}(?:\b|$)/g;
        let match;

        while ((match = broadRegex.exec(text)) !== null) {
            const rawMatch = match[0];

            // Validate
            const validPhone = validateAndCleanPhone(rawMatch);
            if (!validPhone) continue;

            // Extract Context
            const start = Math.max(0, match.index - 15);
            const end = Math.min(text.length, match.index + rawMatch.length + 15);
            let contextSnippet = text.substring(start, end).replace(/\s+/g, ' ').trim();

            // Final check on context: if context looks like code, reject
            if (isCodeOrJson(contextSnippet)) continue;

            addItem(validPhone, 'phone', element, contextSnippet);
        }
    }
}

function getContextText(element) {
    if (!element) return '';
    let context = '';

    if (element.tagName === 'INPUT' || element.tagName === 'TEXTAREA') {
        const label = element.labels?.[0] || document.querySelector(`label[for="${element.id}"]`);
        if (label) context = label.textContent.trim();
    }

    if (!context && element.parentElement) {
        context = element.parentElement.textContent.trim().substring(0, 100);
    }

    // Sanitize context
    if (isCodeOrJson(context)) return 'Page Text';

    return context.replace(/\s+/g, ' ').trim();
}

function addItem(value, type, element, specificContext = null) {
    value = value.trim();
    if (!value || collectedSet.has(value)) return;

    collectedSet.add(value);

    const context = specificContext || getContextText(element);
    const timestamp = new Date().toISOString();

    const newItem = {
        type,
        value,
        context: context || 'N/A',
        url: window.location.href,
        timestamp: timestamp,
        source: window.location.href,
        date: timestamp
    };

    chrome.runtime.sendMessage({ action: 'save_data', data: newItem }, (response) => {
        if (response && response.isNew) {
            flashCheckIcon();
        }
    });
}

function updateCount(count) {
    if (countElement) countElement.textContent = count;
}

function flashCheckIcon() {
    if (iconElement && document.getElementById('de-check-icon')) {
        iconElement.classList.add('hidden');
        document.getElementById('de-check-icon').classList.remove('hidden');
        setTimeout(() => {
            document.getElementById('de-check-icon').classList.add('hidden');
            iconElement.classList.remove('hidden');
        }, 1500);
    }
}