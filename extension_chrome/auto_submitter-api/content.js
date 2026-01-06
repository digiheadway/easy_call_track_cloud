(function () {
    // --- Constants & State ---
    const STORAGE_KEY = 'as_state';
    const PANEL_ID = 'as-panel';

    // Default State
    let state = {
        step: 1, // 1: Setup, 2: Running
        csvData: [],
        headers: [],
        mapping: {},
        currentIndex: 0,
        rowStatuses: {}, // { index: { entered: null, submitted: null, status: 'pending' } }
        settings: {
            isActive: false,     // "Working" vs "Stopped"
            autoSubmit: false,   // Try to click submit
            autoNext: false,     // Auto advance to next row after submit/reload
            autoRedirect: false  // Auto click "Submit another response"
        }
    };

    let formFields = []; // { id, label, element, badge }

    // --- Initialization ---
    init();

    async function init() {
        await loadState();

        // Check for auto-submit data from background script
        const autoData = await chrome.storage.local.get(['autoSubmitData']);
        console.log("Auto Submitter: Checking storage for autoSubmitData...", autoData);

        if (autoData.autoSubmitData && autoData.autoSubmitData.data) {
            const records = autoData.autoSubmitData.data;
            console.log("Auto Submitter: Found data records:", records);

            if (Array.isArray(records) && records.length > 0) {
                console.log("Auto Submitter: Importing data...");
                // Convert to CSV format (Headers + Rows)
                // We assume all records have same keys, or at least we take keys from first one
                const headers = Object.keys(records[0]);
                const rows = records.map(r => headers.map(h => r[h]));

                state.headers = headers;
                state.csvData = rows;
                // Reset statuses
                state.rowStatuses = {};
                rows.forEach((_, idx) => state.rowStatuses[idx] = { status: 'pending' });

                state.step = 1; // Ensure we are in setup mode to verify mapping

                // Clear the data so we don't reload it next time
                await chrome.storage.local.remove('autoSubmitData');
                await saveState();
                console.log("Auto Submitter: Data imported and storage cleared.");
            }
        } else {
            console.log("Auto Submitter: No autoSubmitData found in storage.");
        }

        // Check for "Submit another response" link (Thank You Page)
        if (checkThankYouPage()) return;

        createPanel();

        if (state.step === 2) {
            // We are in running mode
            renderTable();
            updateControls();

            if (state.settings.isActive) {
                // Auto Fill Logic: Find first pending, else first filled
                const nextIndex = findNextRowToFill();
                if (nextIndex !== -1) {
                    state.currentIndex = nextIndex;
                    if (state.csvData.length > state.currentIndex) {
                        // Slight delay to ensure form is ready
                        setTimeout(() => {
                            fillForm(state.currentIndex);

                            if (state.settings.autoSubmit) {
                                setTimeout(attemptSubmit, 200); // Faster submit
                            }
                        }, 100); // Faster initial fill
                    }
                }
            }
        }
    }

    function checkThankYouPage() {
        // Common text for Google Forms "Submit another response"
        const links = document.querySelectorAll('a');
        for (let link of links) {
            if (link.innerText.includes('Submit another response')) {
                if (state.settings.isActive && state.settings.autoNext) {
                    // We are done with previous, move to next
                    state.currentIndex++;
                    state.rowStatuses[state.currentIndex] = { status: 'pending' }; // Init next
                    saveState().then(() => {
                        link.click();
                    });
                    return true; // We are redirecting
                }
                // Even if not auto-redirecting, we might want to show a small status
                createSimpleStatus("✅ Response Recorded. Waiting for user...");
                return true;
            }
        }
        return false;
    }

    function createSimpleStatus(msg) {
        const div = document.createElement('div');
        Object.assign(div.style, {
            position: 'fixed', top: '10px', right: '10px', padding: '10px',
            background: '#10b981', color: 'white', borderRadius: '8px', zIndex: 999999
        });
        div.textContent = msg;
        document.body.appendChild(div);
    }

    // --- UI Construction ---
    function createPanel() {
        if (document.getElementById(PANEL_ID)) return;

        const panel = document.createElement('div');
        panel.id = PANEL_ID;
        Object.assign(panel.style, {
            position: 'fixed', top: '20px', right: '20px', width: '400px',
            backgroundColor: '#0f172a', color: '#f8fafc', padding: '0',
            borderRadius: '12px', boxShadow: '0 20px 25px -5px rgba(0,0,0,0.3)',
            zIndex: '999999', fontFamily: 'Segoe UI, sans-serif', border: '1px solid #334155',
            maxHeight: '90vh', display: 'flex', flexDirection: 'column'
        });

        // Header
        const header = document.createElement('div');
        Object.assign(header.style, {
            padding: '16px', borderBottom: '1px solid #334155', display: 'flex',
            justifyContent: 'space-between', alignItems: 'center', background: '#1e293b',
            borderTopLeftRadius: '12px', borderTopRightRadius: '12px', cursor: 'move', userSelect: 'none'
        });
        header.innerHTML = `
            <h3 style="margin:0; color: #38bdf8; font-size: 1.1rem;">🚀 Auto Submitter</h3>
            <button id="as-toggle-btn" style="padding: 6px 12px; border-radius: 6px; border: none; font-weight: bold; cursor: pointer;">
                ${state.settings.isActive ? 'WORKING' : 'STOPPED'}
            </button>
        `;
        panel.appendChild(header);

        // Content Area
        const content = document.createElement('div');
        content.id = 'as-content';
        Object.assign(content.style, { padding: '16px', overflowY: 'auto', flex: '1' });
        panel.appendChild(content);

        document.body.appendChild(panel);

        // Drag Logic
        let isDragging = false;
        let startX, startY, initialLeft, initialTop;

        header.addEventListener('mousedown', (e) => {
            // Prevent dragging if clicking the button
            if (e.target.tagName === 'BUTTON') return;

            isDragging = true;
            startX = e.clientX;
            startY = e.clientY;

            const rect = panel.getBoundingClientRect();
            initialLeft = rect.left;
            initialTop = rect.top;

            // Remove 'right' property if it exists so 'left' takes over
            panel.style.right = 'auto';
            panel.style.left = `${initialLeft}px`;
            panel.style.top = `${initialTop}px`;

            document.addEventListener('mousemove', onMouseMove);
            document.addEventListener('mouseup', onMouseUp);
        });

        function onMouseMove(e) {
            if (!isDragging) return;
            const dx = e.clientX - startX;
            const dy = e.clientY - startY;
            panel.style.left = `${initialLeft + dx}px`;
            panel.style.top = `${initialTop + dy}px`;
        }

        function onMouseUp() {
            isDragging = false;
            document.removeEventListener('mousemove', onMouseMove);
            document.removeEventListener('mouseup', onMouseUp);
        }

        // Event Listeners
        const toggleBtn = header.querySelector('#as-toggle-btn');
        updateToggleBtnStyle(toggleBtn);

        toggleBtn.addEventListener('click', () => {
            state.settings.isActive = !state.settings.isActive;
            updateToggleBtnStyle(toggleBtn);
            toggleBtn.textContent = state.settings.isActive ? 'WORKING' : 'STOPPED';
            saveState();

            if (state.settings.isActive) {
                const nextIndex = findNextRowToFill();
                if (nextIndex !== -1) {
                    state.currentIndex = nextIndex;
                    saveState();
                    renderTable();
                    fillForm(state.currentIndex);
                }
            }
        });

        renderContent();
    }

    function updateToggleBtnStyle(btn) {
        if (state.settings.isActive) {
            Object.assign(btn.style, { background: '#10b981', color: 'white' });
        } else {
            Object.assign(btn.style, { background: '#ef4444', color: 'white' });
        }
    }

    function renderContent() {
        const content = document.getElementById('as-content');
        content.innerHTML = '';

        if (state.step === 1) {
            renderSetup(content);
        } else {
            renderRunner(content);
        }
    }

    function renderSetup(container) {
        container.innerHTML = `
            <div style="margin-bottom: 12px;">
                <label style="display:block; font-size: 0.85rem; margin-bottom: 4px; color: #94a3b8;">1. Input CSV Data</label>
                <textarea id="as-csv-input" rows="6" style="width: 100%; box-sizing: border-box; background: #1e293b; border: 1px solid #334155; color: #e2e8f0; padding: 8px; border-radius: 6px; font-family: monospace; font-size: 12px;" placeholder="name,email\nJohn,john@doe.com"></textarea>
            </div>
            <button id="as-parse-btn" style="width: 100%; background: #3b82f6; color: white; border: none; padding: 10px; border-radius: 6px; cursor: pointer; font-weight: 600;">Map Fields</button>
            <div id="as-mapping-area" style="margin-top: 12px; display: none;">
                <p style="font-size: 0.9rem; color: #94a3b8;">2. Map fields using the badges on the form.</p>
                <button id="as-link-done-btn" style="width: 100%; background: #10b981; color: white; border: none; padding: 10px; border-radius: 6px; cursor: pointer; font-weight: 600;">Linking Done ✅</button>
            </div>
        `;

        const textarea = container.querySelector('#as-csv-input');
        if (state.csvData && Array.isArray(state.csvData) && state.csvData.length > 0 &&
            state.headers && Array.isArray(state.headers) && state.headers.length > 0) {
            // Reconstruct CSV for display
            const headerRow = state.headers.join(',');
            const dataRows = state.csvData.map(row => {
                if (!Array.isArray(row)) return "";
                return row.map(cell => {
                    if (cell == null) return "";
                    const c = String(cell);
                    if (c.includes(',') || c.includes('"') || c.includes('\n')) {
                        return `"${c.replace(/"/g, '""')}"`;
                    }
                    return c;
                }).join(',');
            }).join('\n');
            textarea.value = headerRow + '\n' + dataRows;

            // Auto-trigger mapping view if we have data
            container.querySelector('#as-mapping-area').style.display = 'block';
            setTimeout(scanAndMapFields, 100);
        }

        container.querySelector('#as-parse-btn').addEventListener('click', () => {
            const csv = textarea.value.trim();
            if (!csv) return;
            const rows = parseCSV(csv);
            if (rows.length < 2) return;

            state.headers = rows[0].map(h => h.trim());
            state.csvData = rows.slice(1);

            // Preserve existing statuses where possible
            const newStatuses = {};
            if (!state.rowStatuses) state.rowStatuses = {};

            state.csvData.forEach((_, idx) => {
                if (state.rowStatuses[idx]) {
                    newStatuses[idx] = state.rowStatuses[idx];
                } else {
                    newStatuses[idx] = { status: 'pending' };
                }
            });
            state.rowStatuses = newStatuses;

            container.querySelector('#as-mapping-area').style.display = 'block';
            scanAndMapFields();
        });

        container.querySelector('#as-link-done-btn').addEventListener('click', () => {
            // Save Mapping (Field ID -> Header Name)
            const mapping = {};
            formFields.forEach(field => {
                const select = field.badge.querySelector('select');
                if (select.value) {
                    mapping[field.id] = select.value;
                }
            });
            state.mapping = mapping;
            state.step = 2;
            // Don't reset currentIndex if we are just editing
            if (state.currentIndex >= state.csvData.length) {
                state.currentIndex = 0;
            }
            saveState();

            // Cleanup badges
            document.querySelectorAll('.ext-mapping-badge').forEach(el => el.remove());

            renderContent();
        });
    }

    function renderRunner(container) {
        container.innerHTML = `
            <div style="display: flex; gap: 10px; margin-bottom: 12px; flex-wrap: wrap;">
                <label style="display: flex; align-items: center; gap: 6px; font-size: 0.9rem; cursor: pointer;" title="Automatically click the Submit button after filling the form.">
                    <input type="checkbox" id="cb-auto-submit" ${state.settings.autoSubmit ? 'checked' : ''}> Auto Submit
                </label>
                <label style="display: flex; align-items: center; gap: 6px; font-size: 0.9rem; cursor: pointer;" title="After submission, automatically click 'Submit another response' and proceed to the next row.">
                    <input type="checkbox" id="cb-auto-next" ${state.settings.autoNext ? 'checked' : ''}> Auto Next
                </label>
            </div>

            <div style="background: #1e293b; border-radius: 8px; overflow: hidden; border: 1px solid #334155; max-height: 400px; overflow-y: auto;">
                <table style="width: 100%; border-collapse: collapse; font-size: 0.85rem;">
                    <thead style="background: #0f172a; position: sticky; top: 0;">
                        <tr>
                            <th style="padding: 8px; text-align: left; color: #94a3b8;">#</th>
                            <th style="padding: 8px; text-align: left; color: #94a3b8;">Data</th>
                            <th style="padding: 8px; text-align: left; color: #94a3b8;">Status</th>
                        </tr>
                    </thead>
                    <tbody id="as-data-table-body">
                        <!-- Rows -->
                    </tbody>
                </table>
            </div>
            
            <div style="margin-top: 12px; display: flex; gap: 8px;">
                <button id="btn-fill-curr" style="flex: 1; background: #3b82f6; color: white; border: none; padding: 8px; border-radius: 6px; cursor: pointer;" title="Fill the form with data from the selected row.">Fill Current</button>
                <button id="btn-edit-data" style="background: #eab308; color: white; border: none; padding: 8px; border-radius: 6px; cursor: pointer;" title="Go back to edit CSV data and mapping.">Edit Data</button>
            </div>
        `;

        // Bind Checkboxes
        container.querySelector('#cb-auto-submit').addEventListener('change', (e) => {
            state.settings.autoSubmit = e.target.checked;
            saveState();
        });
        container.querySelector('#cb-auto-next').addEventListener('change', (e) => {
            state.settings.autoNext = e.target.checked;
            saveState();
        });

        container.querySelector('#btn-fill-curr').addEventListener('click', () => {
            fillForm(state.currentIndex);
        });

        container.querySelector('#btn-edit-data').addEventListener('click', () => {
            state.step = 1;
            saveState();
            renderContent();
        });

        renderTable();
    }

    function renderTable() {
        const tbody = document.getElementById('as-data-table-body');
        if (!tbody) return;
        tbody.innerHTML = '';

        state.csvData.forEach((row, index) => {
            const tr = document.createElement('tr');
            const isCurrent = index === state.currentIndex;
            const status = state.rowStatuses[index] || { status: 'pending' };

            tr.style.borderBottom = '1px solid #334155';
            tr.style.cursor = 'pointer';
            if (isCurrent) {
                tr.style.background = 'rgba(59, 130, 246, 0.2)'; // Blue tint
                tr.style.borderLeft = '4px solid #3b82f6';
            } else {
                tr.style.background = 'transparent';
            }

            // Data Preview (First 2 cols)
            const preview = row.slice(0, 2).join(', ');

            // Index Cell
            const tdIndex = document.createElement('td');
            tdIndex.style.padding = '8px';
            tdIndex.textContent = index + 1;
            tr.appendChild(tdIndex);

            // Data Cell
            const tdData = document.createElement('td');
            tdData.style.padding = '8px';
            tdData.style.whiteSpace = 'nowrap';
            tdData.style.overflow = 'hidden';
            tdData.style.textOverflow = 'ellipsis';
            tdData.style.maxWidth = '150px';
            tdData.textContent = preview;
            tr.appendChild(tdData);

            // Status Cell (Clickable)
            const tdStatus = document.createElement('td');
            tdStatus.style.padding = '8px';
            tdStatus.style.cursor = 'pointer';
            tdStatus.style.userSelect = 'none';
            tdStatus.innerHTML = getStatusBadge(status);
            tdStatus.title = "Click to toggle status (Pending -> Filled -> Submitted)";
            tdStatus.addEventListener('click', (e) => {
                e.stopPropagation(); // Prevent row selection
                console.log(`Auto Submitter: Toggling status for row ${index + 1}`);
                toggleRowStatus(index);
            });
            tr.appendChild(tdStatus);

            // Row Click (Selection)
            tr.addEventListener('click', () => {
                state.currentIndex = index;
                saveState();
                renderTable(); // Re-render to update selection
                fillForm(index);
            });

            tbody.appendChild(tr);
        });

        // Scroll current into view
        const currentTr = tbody.children[state.currentIndex];
        if (currentTr) {
            currentTr.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }
    }

    function toggleRowStatus(index) {
        const current = state.rowStatuses[index]?.status || 'pending';
        let next;
        if (current === 'pending') next = 'filled';
        else if (current === 'filled') next = 'submitted';
        else next = 'pending';

        console.log(`Auto Submitter: Changing status from ${current} to ${next}`);

        state.rowStatuses[index] = { ...state.rowStatuses[index], status: next };
        // Clear timestamps if resetting to pending
        if (next === 'pending') {
            delete state.rowStatuses[index].entered;
            delete state.rowStatuses[index].submitted;
        }
        saveState();
        renderTable();
    }

    function findNextRowToFill() {
        // 1. Find first 'pending'
        let index = state.csvData.findIndex((_, i) => {
            const s = state.rowStatuses[i]?.status;
            return !s || s === 'pending';
        });

        if (index !== -1) return index;

        // 2. If no pending, find first 'filled'
        index = state.csvData.findIndex((_, i) => {
            const s = state.rowStatuses[i]?.status;
            return s === 'filled';
        });

        if (index !== -1) return index;

        // 3. Default to 0 if all submitted
        return 0;
    }

    function getStatusBadge(statusObj) {
        const s = statusObj.status;
        if (s === 'submitted') return '<span style="color: #34d399; font-weight: bold;">Submitted</span>';
        if (s === 'filled') return '<span style="color: #38bdf8;">Filled</span>';
        return '<span style="color: #94a3b8;">Pending</span>';
    }

    // --- Logic ---

    function fillForm(index) {
        if (index < 0 || index >= state.csvData.length) return;

        const rowData = state.csvData[index];
        const mapping = state.mapping; // Now { fieldId: headerName }

        // Update Status to Filled
        state.rowStatuses[index] = { ...state.rowStatuses[index], status: 'filled', entered: Date.now() };
        saveState();
        renderTable();

        // We need to re-scan inputs because the page might have changed/reloaded
        const inputs = findInputs();

        let filledCount = 0;

        // Iterate over the MAPPED FIELDS
        Object.keys(mapping).forEach(fieldId => {
            // Skip reCAPTCHA fields even if they were somehow mapped
            if (fieldId === 'g-recaptcha-response') {
                console.log(`Auto Submitter: Skipping reCAPTCHA field: ${fieldId}`);
                return;
            }

            const headerName = mapping[fieldId];
            const colIndex = state.headers.indexOf(headerName);

            if (colIndex === -1) return; // Header not found in current CSV

            const val = rowData[colIndex];

            // Find all inputs with this ID (handles Radio/Checkbox groups)
            const matchingInputs = inputs.filter(inp => inp.id === fieldId);

            if (matchingInputs.length > 0) {
                // If multiple inputs (Radio/Checkbox), try to find the one matching the value
                if (matchingInputs.length > 1 || matchingInputs[0].element.type === 'radio' || matchingInputs[0].element.type === 'checkbox') {
                    const target = matchingInputs.find(inp => {
                        // Fuzzy match value
                        const elVal = (inp.element.value || '').toLowerCase().trim();
                        const csvVal = (val || '').toLowerCase().trim();
                        return elVal === csvVal;
                    });

                    if (target) {
                        setInputValue(target.element, val);
                        filledCount++;
                    } else {
                        // Fallback: If it's a text input that just happens to share ID (rare), set first
                        if (matchingInputs[0].element.type === 'text' || matchingInputs[0].element.tagName === 'TEXTAREA') {
                            setInputValue(matchingInputs[0].element, val);
                            filledCount++;
                        }
                    }
                } else {
                    // Single input (Text, Date, etc.)
                    setInputValue(matchingInputs[0].element, val);
                    filledCount++;
                }
            }
        });

        console.log(`Auto Submitter: Filled ${filledCount} fields for row ${index + 1}`);

        // Attach listener to Submit button to detect manual submission
        attachSubmitListener();
    }

    function attachSubmitListener() {
        const buttons = document.querySelectorAll('div[role="button"], button, input[type="submit"]');
        for (let btn of buttons) {
            const text = (btn.innerText || btn.value || '').toLowerCase();
            if (text === 'submit' || text === 'send') {
                // Remove old listener to avoid duplicates if possible, 
                // but anonymous functions are hard to remove. 
                // We'll just add a new one that checks the current index.
                btn.addEventListener('click', () => {
                    console.log('Auto Submitter: Manual submit detected.');
                    state.rowStatuses[state.currentIndex] = { ...state.rowStatuses[state.currentIndex], status: 'submitted', submitted: Date.now() };
                    saveState();
                    // We don't re-render table immediately because page might unload.
                    // But if it fails, at least we saved state.
                });
                break;
            }
        }
    }

    function setInputValue(input, value) {
        if (!input) return;

        // Ensure element is visible/interactive
        // For text-like inputs, clicking can ensure focus and trigger necessary UI updates.
        // For radio/checkbox, we handle checked state directly below.
        if (input.type !== 'radio' && input.type !== 'checkbox') {
            input.click();
        }
        input.focus();

        if (input.type === 'radio' || input.type === 'checkbox') {
            if (!input.checked) {
                input.checked = true;
            }
        } else if (input.type === 'date') {
            input.value = value;
        } else {
            input.value = value;
        }

        // Dispatch events with more robust options
        input.dispatchEvent(new Event('focus', { bubbles: true, composed: true }));

        // Use InputEvent for 'input' if available, fallback to Event
        try {
            input.dispatchEvent(new InputEvent('input', { bubbles: true, composed: true, inputType: 'insertText', data: value }));
        } catch (e) {
            input.dispatchEvent(new Event('input', { bubbles: true, composed: true }));
        }

        input.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
        input.dispatchEvent(new Event('blur', { bubbles: true, composed: true }));
    }

    function attemptSubmit() {
        // Look for submit button
        const buttons = document.querySelectorAll('div[role="button"], button, input[type="submit"]');
        let submitBtn = null;

        for (let btn of buttons) {
            const text = (btn.innerText || btn.value || '').toLowerCase();
            if (text === 'submit' || text === 'send') {
                submitBtn = btn;
                break;
            }
        }

        if (submitBtn) {
            console.log('Auto Submitter: Clicking submit...');

            // Mark as submitted BEFORE click (optimistic)
            state.rowStatuses[state.currentIndex] = { ...state.rowStatuses[state.currentIndex], status: 'submitted', submitted: Date.now() };
            saveState();
            renderTable();

            submitBtn.click();
        } else {
            console.warn('Auto Submitter: Could not find submit button.');
        }
    }

    // --- Helpers ---

    function findInputs() {
        // Similar to scanAndMapFields but returns list of objects
        const results = [];
        const inputs = document.querySelectorAll('input:not([type="hidden"]):not([type="submit"]):not([type="button"]):not([type="file"]), textarea');

        inputs.forEach(input => {
            let fieldId = input.name || input.id;

            if (!fieldId && input.type === 'email') fieldId = 'emailAddress';

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

            if (fieldId) {
                results.push({ id: fieldId, element: input });
            }
        });
        return results;
    }

    // Reuse existing mapping logic for Setup Phase
    function scanAndMapFields() {
        // Find inputs (excluding file inputs and our own CSV input)
        const inputs = document.querySelectorAll('input:not([type="hidden"]):not([type="submit"]):not([type="button"]):not([type="file"]), textarea:not(#as-csv-input)');

        formFields = [];

        inputs.forEach(input => {
            if (input.type === 'file') return;

            let fieldId = input.name || input.id;
            if (!fieldId && input.type === 'email') fieldId = 'emailAddress';
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

            // Label finding logic
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

            const badge = createMappingBadge(input, labelText, fieldId);
            formFields.push({ id: fieldId, label: labelText, element: input, badge: badge });
        });
    }

    function createMappingBadge(input, label, fieldId) {
        const existing = input.parentElement.querySelector('.ext-mapping-badge');
        if (existing) existing.remove();

        const badge = document.createElement('div');
        badge.className = 'ext-mapping-badge';
        Object.assign(badge.style, {
            position: 'absolute', top: '-35px', left: '0', background: '#0f172a',
            padding: '6px', borderRadius: '6px', zIndex: '1000',
            boxShadow: '0 4px 6px -1px rgba(0,0,0,0.1)', border: '1px solid #334155',
            display: 'flex', alignItems: 'center', gap: '8px'
        });

        const select = document.createElement('select');
        Object.assign(select.style, {
            background: '#1e293b', color: '#e2e8f0', border: '1px solid #475569',
            borderRadius: '4px', padding: '2px 4px', fontSize: '12px', maxWidth: '150px'
        });

        const defaultOption = document.createElement('option');
        defaultOption.value = "";
        defaultOption.text = "Select CSV Column...";
        select.appendChild(defaultOption);

        let bestMatch = null;

        // 1. Check existing state.mapping first
        // state.mapping is now { "field_id": "Header Name" }
        const savedHeader = state.mapping ? state.mapping[fieldId] : null;

        if (savedHeader && state.headers.includes(savedHeader)) {
            bestMatch = savedHeader;
        } else {
            // 2. Fallback to Fuzzy Match
            state.headers.forEach((header) => {
                const h = header.toLowerCase().replace(/[^a-z0-9]/g, '');
                const l = label.toLowerCase().replace(/[^a-z0-9]/g, '');
                if (l.includes(h) || h.includes(l)) bestMatch = header;
            });
        }

        state.headers.forEach((header) => {
            const option = document.createElement('option');
            option.value = header;
            option.text = header;
            select.appendChild(option);
        });

        if (bestMatch) select.value = bestMatch;

        // 3. Live Preview on Change & Initial Load
        const updatePreview = () => {
            const selectedHeader = select.value;
            if (selectedHeader) {
                const colIndex = state.headers.indexOf(selectedHeader);
                // Preview using the first row of data (index 0) or current index if valid
                // User requested "first row (not header) values"
                const rowIndex = (state.currentIndex >= 0 && state.currentIndex < state.csvData.length) ? state.currentIndex : 0;

                if (colIndex !== -1 && state.csvData.length > rowIndex) {
                    const val = state.csvData[rowIndex][colIndex];
                    setInputValue(input, val);
                    // console.log(`Auto Submitter: Previewing '${val}' for ${label}`);
                }
            }
        };

        select.addEventListener('change', updatePreview);

        // Trigger initial preview if we have a match
        if (bestMatch) {
            // Small delay to ensure UI is ready
            setTimeout(updatePreview, 100);
        }

        badge.appendChild(select);

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
                if (inQuotes && nextChar === '"') { currentCell += '"'; i++; }
                else { inQuotes = !inQuotes; }
            } else if (char === ',' && !inQuotes) {
                currentRow.push(currentCell); currentCell = '';
            } else if ((char === '\r' || char === '\n') && !inQuotes) {
                if (currentCell || currentRow.length > 0) {
                    currentRow.push(currentCell); rows.push(currentRow); currentRow = []; currentCell = '';
                }
                if (char === '\r' && nextChar === '\n') i++;
            } else { currentCell += char; }
        }
        if (currentCell || currentRow.length > 0) { currentRow.push(currentCell); rows.push(currentRow); }
        return rows;
    }

    async function loadState() {
        const result = await chrome.storage.local.get([STORAGE_KEY]);
        if (result[STORAGE_KEY]) {
            state = { ...state, ...result[STORAGE_KEY] };
            if (!Array.isArray(state.csvData)) state.csvData = [];
            if (!Array.isArray(state.headers)) state.headers = [];
            if (!state.rowStatuses) state.rowStatuses = {};
        }
    }

    async function saveState() {
        const obj = {};
        obj[STORAGE_KEY] = state;
        await chrome.storage.local.set(obj);
    }

    function updateControls() {
        // Helper to update UI elements if they exist
    }

})();
