document.addEventListener('DOMContentLoaded', async () => {
    const startBtn = document.getElementById('start-btn');
    const stopBtn = document.getElementById('stop-btn');
    const tableHeader = document.getElementById('table-header');
    const tableBody = document.getElementById('table-body');
    const progressBar = document.getElementById('progress-bar');
    const consoleLog = document.getElementById('console-log');

    // Stats
    const totalEl = document.getElementById('total-count');
    const successEl = document.getElementById('success-count');
    const failEl = document.getElementById('fail-count');

    let isRunning = false;
    let shouldStop = false;
    let config = null;

    // Load data from storage
    const data = await chrome.storage.local.get(['blasterConfig']);
    if (!data.blasterConfig) {
        log("❌ No configuration found. Please start from the form page.");
        return;
    }

    config = data.blasterConfig;
    log(`✅ Loaded configuration for "${config.formTitle || 'Form'}"`);
    log(`📝 Target URL: ${config.actionUrl}`);

    // Initialize UI
    initTable();

    startBtn.addEventListener('click', () => {
        if (isRunning) return;
        startBlasting();
    });

    stopBtn.addEventListener('click', () => {
        if (!isRunning) return;
        shouldStop = true;
        log("⚠️ Stopping after current request...");
        stopBtn.textContent = "Stopping...";
    });

    function initTable() {
        // Headers
        config.headers.forEach(h => {
            const th = document.createElement('th');
            th.textContent = h;
            tableHeader.appendChild(th);
        });

        // Rows
        config.data.forEach((row, index) => {
            const tr = document.createElement('tr');
            tr.id = `row-${index}`;

            // Status Cell
            const statusTd = document.createElement('td');
            statusTd.className = 'status-pending';
            statusTd.textContent = 'Pending';
            tr.appendChild(statusTd);

            // Data Cells
            row.forEach(cell => {
                const td = document.createElement('td');
                td.textContent = cell;
                tr.appendChild(td);
            });

            tableBody.appendChild(tr);
        });

        totalEl.textContent = config.data.length;
    }

    async function startBlasting() {
        isRunning = true;
        shouldStop = false;
        startBtn.disabled = true;
        startBtn.classList.add('btn-disabled');
        stopBtn.disabled = false;
        stopBtn.classList.remove('btn-disabled');
        stopBtn.textContent = "Stop";

        let successCount = 0;
        let failCount = 0;

        for (let i = 0; i < config.data.length; i++) {
            if (shouldStop) break;

            const row = config.data[i];
            const tr = document.getElementById(`row-${i}`);
            const statusTd = tr.firstElementChild;

            // Skip if already done
            if (statusTd.classList.contains('status-success')) {
                successCount++;
                continue;
            }

            statusTd.className = 'status-sending';
            statusTd.textContent = 'Sending...';
            tr.scrollIntoView({ behavior: 'smooth', block: 'center' });

            try {
                await sendRequest(row);
                statusTd.className = 'status-success';
                statusTd.textContent = 'Success';
                successCount++;
                successEl.textContent = successCount;
                log(`✅ Row ${i + 1} submitted successfully.`);
            } catch (err) {
                statusTd.className = 'status-error';
                statusTd.textContent = 'Failed';
                failCount++;
                failEl.textContent = failCount;
                log(`❌ Row ${i + 1} failed: ${err.message}`);
            }

            // Update Progress
            const progress = ((i + 1) / config.data.length) * 100;
            progressBar.style.width = `${progress}%`;

            // Random delay to simulate human behavior (2-4 seconds)
            const delay = 2000 + Math.floor(Math.random() * 2000);
            await new Promise(r => setTimeout(r, delay));
        }

        isRunning = false;
        startBtn.disabled = false;
        startBtn.classList.remove('btn-disabled');
        stopBtn.disabled = true;
        stopBtn.classList.add('btn-disabled');

        if (shouldStop) {
            log("🛑 Stopped by user.");
        } else {
            log("🎉 All done!");
        }
    }

    async function sendRequest(rowData) {
        const formData = new URLSearchParams();

        // Add mapped fields
        config.headers.forEach((header, index) => {
            const entryId = config.mapping[header];
            if (entryId) {
                const val = rowData[index];
                // Check if it's a date field
                if (config.dateFields && config.dateFields[entryId] && val) {
                    const parts = val.split('-'); // Expecting YYYY-MM-DD
                    if (parts.length === 3) {
                        formData.append(entryId + '_year', parts[0]);
                        formData.append(entryId + '_month', parseInt(parts[1], 10).toString());
                        formData.append(entryId + '_day', parseInt(parts[2], 10).toString());
                    } else {
                        formData.append(entryId, val);
                    }
                } else {
                    formData.append(entryId, val);
                }
            }
        });

        // Essential Google Forms fields
        formData.append('fvv', '1');

        const fbzx = config.fbzx || config.hiddenFields['fbzx'] || '';
        if (fbzx) {
            formData.append('fbzx', fbzx);
        }

        formData.append('pageHistory', '0');

        // Debug: Log what we're sending
        console.log('Sending to:', config.actionUrl);
        console.log('Form data:', Object.fromEntries(formData.entries()));

        // Fetch with minimal headers
        const response = await fetch(config.actionUrl, {
            method: 'POST',
            mode: 'no-cors',
            credentials: 'include', // Include cookies
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: formData
        });

        return response;
    }

    function log(msg) {
        const div = document.createElement('div');
        div.textContent = `> ${msg}`;
        consoleLog.appendChild(div);
        consoleLog.scrollTop = consoleLog.scrollHeight;
    }
});
