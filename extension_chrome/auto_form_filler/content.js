(function () {
    // Check if the URL contains "forms"
    const isFormPage = window.location.href.toLowerCase().includes("forms");

    if (!isFormPage) return;

    // Global state
    let formFields = []; // { id, label, element, badge }
    let csvHeaders = [];
    let csvData = [];
    let savedMappings = {};

    // Load saved mappings for this form
    const formId = window.location.pathname; // Use path as ID
    chrome.storage.local.get([`mapping_${formId}`], (result) => {
        if (result[`mapping_${formId}`]) {
            savedMappings = result[`mapping_${formId}`];
        }
    });

    // Initialize Panel
    createPanel();

    async function createPanel() {
        const panel = document.createElement('div');
        panel.id = 'form-blaster-panel';
        Object.assign(panel.style, {
            position: 'fixed',
            top: '20px',
            right: '20px',
            width: '350px',
            backgroundColor: '#1e293b',
            color: '#f8fafc',
            padding: '16px',
            borderRadius: '12px',
            boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)',
            zIndex: '999999',
            fontFamily: 'sans-serif',
            border: '1px solid #334155',
            maxHeight: '90vh',
            overflowY: 'auto'
        });

        panel.innerHTML = `
            <h3 style="margin-top:0; color: #38bdf8; font-size: 1.1rem; border-bottom: 1px solid #334155; padding-bottom: 8px;">🚀 Form Data Blaster</h3>
            
            <div id="step-1">
                <div style="margin-bottom: 12px;">
                    <label style="display:block; font-size: 0.85rem; margin-bottom: 4px; color: #94a3b8;">1. Input CSV Data (Header required)</label>
                    <div style="display: flex; gap: 8px; margin-bottom: 8px;">
                        <input type="file" id="csv-file-input" accept=".csv" style="font-size: 12px; color: #94a3b8;">
                    </div>
                    <textarea id="csv-input" rows="6" style="width: 100%; box-sizing: border-box; background: #0f172a; border: 1px solid #334155; color: #e2e8f0; padding: 8px; border-radius: 6px; font-family: monospace; font-size: 12px;" placeholder="name,email,phone\nJohn,john@doe.com,123"></textarea>
                </div>
                <button id="parse-btn" style="width: 100%; background: #3b82f6; color: white; border: none; padding: 10px; border-radius: 6px; cursor: pointer; font-weight: 600;">Next: Map Fields</button>
            </div>

            <div id="step-2" style="display:none;">
                <p style="font-size: 0.9rem; color: #94a3b8; margin-bottom: 12px;">2. Map the CSV columns to the form fields on the page. <br><span style="color: #34d399">Green check</span> = Auto-matched.</p>
                <button id="launch-btn" style="width: 100%; background: #10b981; color: white; border: none; padding: 10px; border-radius: 6px; cursor: pointer; font-weight: 600;">🚀 Launch Runner</button>
                <button id="curl-btn" style="width: 100%; background: #0ea5e9; color: white; border: none; padding: 10px; border-radius: 6px; cursor: pointer; font-weight: 600; margin-top: 8px;">📋 Copy cURL (Row 1)</button>
                <button id="reset-btn" style="width: 100%; background: transparent; color: #94a3b8; border: 1px solid #334155; padding: 8px; border-radius: 6px; cursor: pointer; margin-top: 8px;">Reset</button>
            </div>
            
            <div id="blast-log" style="margin-top: 12px; font-size: 12px; color: #ef4444;"></div>
        `;

        document.body.appendChild(panel);

        const parseBtn = panel.querySelector('#parse-btn');
        const launchBtn = panel.querySelector('#launch-btn');
        const curlBtn = panel.querySelector('#curl-btn');
        const resetBtn = panel.querySelector('#reset-btn');
        const textarea = panel.querySelector('#csv-input');
        const fileInput = panel.querySelector('#csv-file-input');
        const log = panel.querySelector('#blast-log');
        const step1 = panel.querySelector('#step-1');
        const step2 = panel.querySelector('#step-2');

        // Load saved CSV
        try {
            const saved = await chrome.storage.local.get(['savedCsvData']);
            console.log('Form Blaster: Loaded CSV data length:', saved.savedCsvData ? saved.savedCsvData.length : 0);
            if (saved.savedCsvData) {
                textarea.value = saved.savedCsvData;
            }
        } catch (e) {
            console.error('Form Blaster: Error loading CSV:', e);
        }

        // File Upload Handler
        fileInput.addEventListener('change', (e) => {
            const file = e.target.files[0];
            if (!file) return;

            const reader = new FileReader();
            reader.onload = (event) => {
                textarea.value = event.target.result;
                chrome.storage.local.set({ savedCsvData: textarea.value });
                console.log('Form Blaster: Saved CSV from file');
            };
            reader.readAsText(file);
        });

        // Textarea Change Handler
        textarea.addEventListener('input', () => {
            const val = textarea.value;
            chrome.storage.local.set({ savedCsvData: val }, () => {
                if (chrome.runtime.lastError) {
                    console.error('Form Blaster: Save error:', chrome.runtime.lastError);
                }
            });
        });

        parseBtn.addEventListener('click', () => {
            const csv = textarea.value.trim();
            if (!csv) {
                log.textContent = "Please paste CSV data first.";
                return;
            }

            const rows = parseCSV(csv);
            if (rows.length < 2) {
                log.textContent = "Need at least a header row and one data row.";
                return;
            }

            csvHeaders = rows[0].map(h => h.trim());
            csvData = rows.slice(1);

            // Switch UI
            step1.style.display = 'none';
            step2.style.display = 'block';
            log.textContent = "";

            // Initialize Mapping Mode
            scanAndMapFields();
        });

        resetBtn.addEventListener('click', () => {
            step1.style.display = 'block';
            step2.style.display = 'none';
            // Clear badges
            document.querySelectorAll('.ext-mapping-badge').forEach(el => el.remove());
            formFields = [];
        });

        launchBtn.addEventListener('click', async () => {
            // Gather Mapping
            const mapping = {};
            formFields.forEach(field => {
                const select = field.badge.querySelector('select');
                if (select.value) {
                    mapping[select.value] = field.id;
                }
            });

            // Save Mapping
            const saveObj = {};
            saveObj[`mapping_${formId}`] = mapping;
            await chrome.storage.local.set(saveObj);

            // Gather Form Info
            const form = document.querySelector('form');
            let actionUrl = form.action;

            // Fix URL: Ensure it targets the submission endpoint
            // Sometimes form.action might be .../viewform or have params
            if (actionUrl.includes('/viewform')) {
                actionUrl = actionUrl.replace('/viewform', '/formResponse');
            }

            const hiddenInputs = form.querySelectorAll('input[type="hidden"]');
            const hiddenFields = {};
            // We only exclude draftResponse now, as the working curl uses most other hidden fields or we set them manually if needed.
            // But to be safe and match the working curl which is minimal, let's just grab what we need or let the user mapping handle entries.
            // The working curl has: entry.IDs, fvv=1, fbzx=..., pageHistory=0.

            hiddenInputs.forEach(input => {
                if (input.name === 'draftResponse') return;
                hiddenFields[input.name] = input.value;
            });

            // Ensure pageHistory is set
            if (!hiddenFields['pageHistory']) {
                hiddenFields['pageHistory'] = '0';
            }

            // Identify Date Fields from our mapping
            const dateFields = {};
            formFields.forEach(f => {
                if (f.element.type === 'date') {
                    dateFields[f.id] = true;
                }
            });

            // Get FBZX
            let fbzx = hiddenFields['fbzx'];
            if (!fbzx) {
                const fbzxInput = form.querySelector('input[name="fbzx"]');
                if (fbzxInput) fbzx = fbzxInput.value;
            }

            const config = {
                actionUrl: actionUrl,
                hiddenFields: hiddenFields,
                headers: csvHeaders,
                data: csvData,
                mapping: mapping,
                dateFields: dateFields,
                fbzx: fbzx,
                formTitle: document.title
            };

            // Save to storage
            await chrome.storage.local.set({ blasterConfig: config });

            // Open Runner via Background Script
            chrome.runtime.sendMessage({ action: "openRunner" });
        });

        curlBtn.addEventListener('click', async () => {
            if (csvData.length === 0) {
                log.textContent = "No data to generate cURL.";
                return;
            }

            // Gather Mapping
            const mapping = {};
            formFields.forEach(field => {
                const select = field.badge.querySelector('select');
                if (select.value) {
                    mapping[select.value] = field.id;
                }
            });

            // Gather Form Info
            const form = document.querySelector('form');
            let actionUrl = form.action;
            if (actionUrl.includes('/viewform')) {
                actionUrl = actionUrl.replace('/viewform', '/formResponse');
            }

            const hiddenInputs = form.querySelectorAll('input[type="hidden"]');
            const hiddenFields = {};

            hiddenInputs.forEach(input => {
                if (input.name === 'draftResponse') return;
                hiddenFields[input.name] = input.value;
            });

            // Construct Data for Row 1
            const rowData = csvData[0];
            const params = new URLSearchParams();

            // Mapped fields
            csvHeaders.forEach((header, index) => {
                const entryId = mapping[header];
                if (entryId) {
                    const val = rowData[index];
                    // Check if it's a date field
                    const fieldObj = formFields.find(f => f.id === entryId);
                    if (fieldObj && fieldObj.element.type === 'date' && val) {
                        const parts = val.split('-'); // Expecting YYYY-MM-DD
                        if (parts.length === 3) {
                            params.append(entryId + '_year', parts[0]);
                            params.append(entryId + '_month', parseInt(parts[1], 10).toString()); // Remove leading zero if any, though Google usually handles both
                            params.append(entryId + '_day', parseInt(parts[2], 10).toString());
                        } else {
                            params.append(entryId, val);
                        }
                    } else {
                        params.append(entryId, val);
                    }
                }
            });

            // Essential Google Forms fields based on working curl
            params.append('fvv', '1');

            let fbzx = hiddenFields['fbzx'];
            if (!fbzx) {
                const fbzxInput = form.querySelector('input[name="fbzx"]');
                if (fbzxInput) fbzx = fbzxInput.value;
            }
            if (fbzx) {
                params.append('fbzx', fbzx);
            }

            params.append('pageHistory', '0');

            // Construct cURL command
            const curlCommand = `curl '${actionUrl}' \\
  -X POST \\
  -H 'Content-Type: application/x-www-form-urlencoded' \\
  --data '${params.toString()}'`;

            try {
                await navigator.clipboard.writeText(curlCommand);
                log.textContent = "✅ cURL command for Row 1 copied to clipboard!";
            } catch (err) {
                console.error('Failed to copy: ', err);
                log.textContent = "❌ Failed to copy to clipboard.";
            }
        });
    }

    function scanAndMapFields() {
        // Find inputs (excluding file inputs and our own CSV input)
        const inputs = document.querySelectorAll('input:not([type="hidden"]):not([type="submit"]):not([type="button"]):not([type="file"]), textarea:not(#csv-input)');

        inputs.forEach(input => {
            // Skip file inputs for safety
            if (input.type === 'file') return;

            // Identify ID
            let fieldId = input.name || input.id;

            // Special handling for Google Forms "Collect email addresses" field
            if (!fieldId && input.type === 'email') {
                fieldId = 'emailAddress';
            }
            if (!fieldId || fieldId === 'No ID') {
                const container = input.closest('[jsmodel="CP1oW"]');
                if (container) {
                    const dataParams = container.getAttribute('data-params');
                    if (dataParams) {
                        const match = dataParams.match(/\[\[(\d+),/);
                        if (match && match[1]) fieldId = `entry.${match[1]}`;
                    }
                }
            }
            if (!fieldId) return;

            // Identify Label
            let labelText = '';
            if (input.getAttribute('aria-label')) labelText = input.getAttribute('aria-label');
            else if (input.getAttribute('aria-labelledby')) {
                const labelIds = input.getAttribute('aria-labelledby').split(/\s+/);
                for (const id of labelIds) {
                    const labelEl = document.getElementById(id);
                    if (labelEl && labelEl.innerText.trim()) {
                        labelText = labelEl.innerText;
                        break;
                    }
                }
            }
            if (!labelText) {
                const container = input.closest('[role="listitem"]') || input.closest('.freebirdFormviewerViewNumberedItemContainer');
                if (container) {
                    const heading = container.querySelector('[role="heading"], .freebirdFormviewerViewItemsItemItemTitle');
                    if (heading) labelText = heading.innerText;
                }
            }
            labelText = labelText.replace(/\s+/g, ' ').trim();

            // Create Badge UI
            const badge = createMappingBadge(input, labelText, fieldId);

            formFields.push({
                id: fieldId,
                label: labelText,
                element: input,
                badge: badge
            });
        });
    }

    function createMappingBadge(input, label, fieldId) {
        // Remove existing
        const existing = input.parentElement.querySelector('.ext-mapping-badge');
        if (existing) existing.remove();

        const badge = document.createElement('div');
        badge.className = 'ext-mapping-badge';
        Object.assign(badge.style, {
            position: 'absolute',
            top: '-35px',
            left: '0',
            background: '#0f172a',
            padding: '6px',
            borderRadius: '6px',
            zIndex: '1000',
            boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)',
            border: '1px solid #334155',
            display: 'flex',
            alignItems: 'center',
            gap: '8px'
        });

        // Dropdown
        const select = document.createElement('select');
        Object.assign(select.style, {
            background: '#1e293b',
            color: '#e2e8f0',
            border: '1px solid #475569',
            borderRadius: '4px',
            padding: '2px 4px',
            fontSize: '12px',
            maxWidth: '150px'
        });

        const defaultOption = document.createElement('option');
        defaultOption.value = "";
        defaultOption.text = "Select CSV Column...";
        select.appendChild(defaultOption);

        let bestMatch = null;

        // Check saved mapping first
        // savedMappings is { "csvHeader": "entry.123" }
        // We need to find which CSV header maps to THIS fieldId
        const savedHeader = Object.keys(savedMappings).find(key => savedMappings[key] === fieldId);

        // Only use saved header if it exists in current CSV
        if (savedHeader && csvHeaders.includes(savedHeader)) {
            bestMatch = savedHeader;
        } else {
            // Fallback to Fuzzy Match
            csvHeaders.forEach((header) => {
                const h = header.toLowerCase().replace(/[^a-z0-9]/g, '');
                const l = label.toLowerCase().replace(/[^a-z0-9]/g, '');
                if (l.includes(h) || h.includes(l)) {
                    bestMatch = header;
                }
            });
        }

        csvHeaders.forEach((header) => {
            const option = document.createElement('option');
            option.value = header;
            option.text = header;
            select.appendChild(option);
        });

        // Status Indicator
        const status = document.createElement('span');
        status.style.fontSize = '14px';

        if (bestMatch) {
            select.value = bestMatch;
            status.textContent = "✅";
            status.title = "Auto-matched!";
            // Preview Data (with safety check for file inputs)
            const colIndex = csvHeaders.indexOf(bestMatch);
            if (colIndex !== -1 && csvData.length > 0 && input.type !== 'file') {
                try {
                    input.dispatchEvent(new Event('focus', { bubbles: true }));
                    input.value = csvData[0][colIndex];
                    input.dispatchEvent(new Event('input', { bubbles: true }));
                    input.dispatchEvent(new Event('change', { bubbles: true }));
                    input.dispatchEvent(new Event('blur', { bubbles: true }));
                } catch (e) {
                    console.warn('Could not set value for input:', e);
                }
            }
        } else {
            status.textContent = "⚠️";
            status.title = "Please select a column";
        }

        // Change Handler
        select.addEventListener('change', () => {
            if (select.value) {
                status.textContent = "✅";
                const colIndex = csvHeaders.indexOf(select.value);
                if (colIndex !== -1 && csvData.length > 0 && input.type !== 'file') {
                    try {
                        input.dispatchEvent(new Event('focus', { bubbles: true }));
                        input.value = csvData[0][colIndex];
                        input.dispatchEvent(new Event('input', { bubbles: true }));
                        input.dispatchEvent(new Event('change', { bubbles: true }));
                        input.dispatchEvent(new Event('blur', { bubbles: true }));
                    } catch (e) {
                        console.warn('Could not set value for input:', e);
                    }
                }
            } else {
                status.textContent = "⚠️";
                if (input.type !== 'file') {
                    input.value = "";
                }
            }
        });

        badge.appendChild(status);
        badge.appendChild(select);

        // Insert
        const parent = input.parentElement;
        if (parent) {
            const parentStyle = window.getComputedStyle(parent);
            if (parentStyle.position === 'static') parent.style.position = 'relative';
            parent.appendChild(badge);
        } else {
            input.parentNode.insertBefore(badge, input);
        }

        return badge;
    }

    function parseCSV(text) {
        const rows = [];
        let currentRow = [];
        let currentCell = '';
        let inQuotes = false;

        for (let i = 0; i < text.length; i++) {
            const char = text[i];
            const nextChar = text[i + 1];

            if (char === '"') {
                if (inQuotes && nextChar === '"') {
                    currentCell += '"';
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (char === ',' && !inQuotes) {
                currentRow.push(currentCell);
                currentCell = '';
            } else if ((char === '\r' || char === '\n') && !inQuotes) {
                if (currentCell || currentRow.length > 0) {
                    currentRow.push(currentCell);
                    rows.push(currentRow);
                    currentRow = [];
                    currentCell = '';
                }
                if (char === '\r' && nextChar === '\n') i++;
            } else {
                currentCell += char;
            }
        }
        if (currentCell || currentRow.length > 0) {
            currentRow.push(currentCell);
            rows.push(currentRow);
        }
        return rows;
    }

})();
