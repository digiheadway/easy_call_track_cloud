<?php
/**
 * Customer Management Dashboard
 * Single page - No Auth (User ID: 1)
 */

require_once __DIR__ . '/config/database.php';
require_once __DIR__ . '/config/firebase.php';

$USER_ID = 1; // Fixed user

// Handle AJAX requests
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action'])) {
    header('Content-Type: application/json');
    
    $action = $_POST['action'];
    $id = $_POST['id'] ?? null;
    
    switch ($action) {
        case 'list':
            $customers = fetchAll("SELECT * FROM customers WHERE user_id = ? AND status = 'active' ORDER BY created_at DESC", [$USER_ID]);
            die(json_encode(['success' => true, 'customers' => $customers]));
            
        case 'get':
            $c = fetchOne("SELECT * FROM customers WHERE id = ? AND user_id = ?", [$id, $USER_ID]);
            die(json_encode(['success' => true, 'customer' => $c]));
            
        case 'add':
            // Generate pairing code
            // 1. Insert with temp code
            $tempCode = 'TEMP_' . uniqid();
            
            $user = fetchOne("SELECT phone FROM users WHERE id = ?", [$USER_ID]);
            
            query("INSERT INTO customers (user_id, name, phone, email, address, loan_amount, pending_amount, 
                   device_name, is_freezed, is_protected, freeze_message, call_to, unlock_codes, pairing_code) 
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", [
                $USER_ID,
                $_POST['name'],
                $_POST['phone'],
                $_POST['email'] ?? null,
                $_POST['address'] ?? null,
                $_POST['loan_amount'] ?? 0,
                $_POST['pending_amount'] ?? 0,
                $_POST['device_name'] ?? null,
                $_POST['is_freezed'] ?? 0,
                $_POST['is_protected'] ?? 1,
                $_POST['freeze_message'] ?? 'Device Locked - Contact Manager',
                $_POST['call_to'] ?? $user['phone'],
                $_POST['unlock_codes'] ?? null,
                $tempCode
            ]);
            
            // 2. Update with final code U{uid}C{cid}
            $id = db()->lastInsertId();
            $finalCode = "U{$USER_ID}C{$id}";
            query("UPDATE customers SET pairing_code = ? WHERE id = ?", [$finalCode, $id]);
            
            die(json_encode(['success' => true, 'id' => $id, 'pairing_code' => $finalCode]));
            
        case 'update':
            $fields = ['name', 'phone', 'email', 'address', 'loan_amount', 'pending_amount', 
                       'device_name', 'is_freezed', 'is_protected', 'freeze_message', 'call_to', 'unlock_codes'];
            $updates = [];
            $params = [];
            foreach ($fields as $f) {
                if (isset($_POST[$f])) {
                    $updates[] = "$f = ?";
                    $params[] = $_POST[$f];
                }
            }
            if ($updates) {
                $params[] = $id;
                query("UPDATE customers SET " . implode(', ', $updates) . " WHERE id = ?", $params);
            }
            die(json_encode(['success' => true]));
            
        case 'delete':
            // Set status to 'used' - pairing code can't be reused unless same IMEI
            query("UPDATE customers SET status = 'used' WHERE id = ? AND user_id = ?", [$id, $USER_ID]);
            die(json_encode(['success' => true]));
            
        case 'push':
            $c = fetchOne("SELECT * FROM customers WHERE id = ? AND user_id = ?", [$id, $USER_ID]);
            if (!$c || !$c['fcm_token']) {
                die(json_encode(['success' => false, 'error' => 'No FCM token. Open app on device first.']));
            }
            
            $command = $_POST['command'] ?? 'SYNC';
            $data = [
                'command' => $command,
                'is_freezed' => $c['is_freezed'] ? '1' : '0',
                'is_protected' => $c['is_protected'] ? '1' : '0',
                'message' => $c['freeze_message'],
                'amount' => (string)$c['pending_amount'],
                'call_to' => $c['call_to'] ?: ''
            ];
            
            $result = sendFCM($c['fcm_token'], $data);
            die(json_encode(['success' => $result['success'], 'fcm' => $result]));
    }
    
    die(json_encode(['success' => false, 'error' => 'Invalid action']));
}
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Device Manager Dashboard</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <script src="https://cdnjs.cloudflare.com/ajax/libs/qrcodejs/1.0.0/qrcode.min.js"></script>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        :root {
            --primary: #6366f1;
            --primary-dark: #4f46e5;
            --success: #10b981;
            --danger: #ef4444;
            --warning: #f59e0b;
            --bg: #0f172a;
            --bg-card: #1e293b;
            --bg-input: #334155;
            --text: #f1f5f9;
            --text-muted: #94a3b8;
            --border: #475569;
        }
        
        body {
            font-family: 'Inter', sans-serif;
            background: var(--bg);
            color: var(--text);
            min-height: 100vh;
        }
        
        .container {
            max-width: 1400px;
            margin: 0 auto;
            padding: 20px;
        }
        
        header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 30px;
            flex-wrap: wrap;
            gap: 15px;
        }
        
        h1 {
            font-size: 1.8rem;
            background: linear-gradient(135deg, #6366f1, #a855f7);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
        }
        
        .btn {
            padding: 10px 20px;
            border: none;
            border-radius: 8px;
            font-size: 14px;
            font-weight: 500;
            cursor: pointer;
            transition: all 0.2s;
            display: inline-flex;
            align-items: center;
            gap: 8px;
        }
        
        .btn-primary {
            background: var(--primary);
            color: white;
        }
        
        .btn-primary:hover {
            background: var(--primary-dark);
            transform: translateY(-1px);
        }
        
        .btn-success {
            background: var(--success);
            color: white;
        }
        
        .btn-danger {
            background: var(--danger);
            color: white;
        }
        
        .btn-warning {
            background: var(--warning);
            color: black;
        }
        
        .btn-sm {
            padding: 6px 12px;
            font-size: 12px;
        }
        
        .stats {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 15px;
            margin-bottom: 30px;
        }
        
        .stat-card {
            background: var(--bg-card);
            padding: 20px;
            border-radius: 12px;
            border: 1px solid var(--border);
        }
        
        .stat-card h3 {
            font-size: 2rem;
            margin-bottom: 5px;
        }
        
        .stat-card p {
            color: var(--text-muted);
            font-size: 14px;
        }
        
        .customer-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
            gap: 20px;
        }
        
        .customer-card {
            background: var(--bg-card);
            border-radius: 16px;
            border: 1px solid var(--border);
            overflow: hidden;
            transition: transform 0.2s, box-shadow 0.2s;
        }
        
        .customer-card:hover {
            transform: translateY(-2px);
            box-shadow: 0 10px 40px rgba(0,0,0,0.3);
        }
        
        .card-header {
            padding: 20px;
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
            border-bottom: 1px solid var(--border);
        }
        
        .card-header h3 {
            font-size: 1.1rem;
            margin-bottom: 5px;
        }
        
        .card-header .phone {
            color: var(--text-muted);
            font-size: 14px;
        }
        
        .pairing-code {
            background: linear-gradient(135deg, #6366f1, #8b5cf6);
            padding: 8px 16px;
            border-radius: 8px;
            font-size: 1.2rem;
            font-weight: 700;
            letter-spacing: 2px;
        }
        
        .card-body {
            padding: 20px;
        }
        
        .device-info {
            display: flex;
            flex-wrap: wrap;
            gap: 10px;
            margin-bottom: 15px;
        }
        
        .badge {
            padding: 4px 10px;
            border-radius: 20px;
            font-size: 12px;
            font-weight: 500;
        }
        
        .badge-frozen {
            background: rgba(239, 68, 68, 0.2);
            color: #f87171;
            border: 1px solid #dc2626;
        }
        
        .badge-active {
            background: rgba(16, 185, 129, 0.2);
            color: #34d399;
            border: 1px solid #059669;
        }
        
        .badge-protected {
            background: rgba(99, 102, 241, 0.2);
            color: #818cf8;
            border: 1px solid #6366f1;
        }
        
        .badge-offline {
            background: rgba(148, 163, 184, 0.2);
            color: #94a3b8;
        }
        
        .badge-online {
            background: rgba(16, 185, 129, 0.2);
            color: #34d399;
        }
        
        .info-row {
            display: flex;
            justify-content: space-between;
            padding: 8px 0;
            border-bottom: 1px solid rgba(71, 85, 105, 0.5);
            font-size: 14px;
        }
        
        .info-row:last-child {
            border-bottom: none;
        }
        
        .info-row .label {
            color: var(--text-muted);
        }
        
        .info-row .value {
            font-weight: 500;
        }
        
        .card-actions {
            padding: 15px 20px;
            background: rgba(0,0,0,0.2);
            display: flex;
            gap: 8px;
            flex-wrap: wrap;
        }
        
        /* Modal */
        .modal-overlay {
            position: fixed;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background: rgba(0,0,0,0.7);
            display: none;
            justify-content: center;
            align-items: center;
            z-index: 1000;
            padding: 20px;
        }
        
        .modal-overlay.active {
            display: flex;
        }
        
        .modal {
            background: var(--bg-card);
            border-radius: 16px;
            width: 100%;
            max-width: 500px;
            max-height: 90vh;
            overflow-y: auto;
            border: 1px solid var(--border);
        }
        
        .modal-header {
            padding: 20px;
            border-bottom: 1px solid var(--border);
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        
        .modal-header h2 {
            font-size: 1.3rem;
        }
        
        .modal-close {
            background: none;
            border: none;
            color: var(--text-muted);
            font-size: 24px;
            cursor: pointer;
        }
        
        .modal-body {
            padding: 20px;
        }
        
        .form-group {
            margin-bottom: 15px;
        }
        
        .form-group label {
            display: block;
            margin-bottom: 5px;
            font-size: 14px;
            color: var(--text-muted);
        }
        
        .form-group input,
        .form-group textarea,
        .form-group select {
            width: 100%;
            padding: 12px;
            border: 1px solid var(--border);
            border-radius: 8px;
            background: var(--bg-input);
            color: var(--text);
            font-size: 14px;
        }
        
        .form-group input:focus,
        .form-group textarea:focus {
            outline: none;
            border-color: var(--primary);
        }
        
        .form-row {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 15px;
        }
        
            gap: 20px;
        }

        .tabs {
            display: flex;
            gap: 10px;
            margin-bottom: 20px;
            border-bottom: 1px solid var(--border);
            padding-bottom: 10px;
        }

        .tab {
            padding: 8px 16px;
            border-radius: 6px;
            cursor: pointer;
            color: var(--text-muted);
            font-weight: 500;
        }

        .tab.active {
            background: var(--primary);
            color: white;
        }

        #qrContainer {
            background: white;
            padding: 20px;
            border-radius: 12px;
            display: inline-block;
            margin: 20px auto;
        }
        
        .setup-instructions {
            font-size: 14px;
            color: var(--text-muted);
            line-height: 1.5;
            background: rgba(0,0,0,0.2);
            padding: 15px;
            border-radius: 8px;
            margin-bottom: 20px;
        }

        .toggle-group {
            display: flex;
            gap: 20px;
        }
        
        .toggle {
            display: flex;
            align-items: center;
            gap: 10px;
            cursor: pointer;
        }
        
        .toggle input {
            width: 20px;
            height: 20px;
            accent-color: var(--primary);
        }
        
        .modal-footer {
            padding: 15px 20px;
            border-top: 1px solid var(--border);
            display: flex;
            justify-content: flex-end;
            gap: 10px;
        }
        
        /* Toast */
        .toast {
            position: fixed;
            bottom: 20px;
            right: 20px;
            padding: 15px 25px;
            border-radius: 8px;
            color: white;
            font-weight: 500;
            z-index: 2000;
            animation: slideIn 0.3s ease;
        }
        
        .toast.success {
            background: var(--success);
        }
        
        .toast.error {
            background: var(--danger);
        }
        
        @keyframes slideIn {
            from {
                transform: translateX(100%);
                opacity: 0;
            }
            to {
                transform: translateX(0);
                opacity: 1;
            }
        }
        
        .empty-state {
            text-align: center;
            padding: 60px 20px;
            color: var(--text-muted);
        }
        
        .empty-state svg {
            width: 80px;
            height: 80px;
            margin-bottom: 20px;
            opacity: 0.5;
        }
        
        @media (max-width: 768px) {
            .customer-grid {
                grid-template-columns: 1fr;
            }
            
            .form-row {
                grid-template-columns: 1fr;
            }
            
            header {
                flex-direction: column;
                align-items: flex-start;
            }
        }
    </style>
</head>
<body>
    <div class="container">
        <header>
            <h1>📱 Device Manager</h1>
            <button class="btn btn-primary" onclick="openAddModal()">
                <span>➕</span> Add Customer
            </button>
        </header>
        
        <div class="stats" id="stats">
            <div class="stat-card">
                <h3 id="totalCount">0</h3>
                <p>Total Customers</p>
            </div>
            <div class="stat-card">
                <h3 id="frozenCount">0</h3>
                <p>🔒 Frozen Devices</p>
            </div>
            <div class="stat-card">
                <h3 id="onlineCount">0</h3>
                <p>🟢 Online (24h)</p>
            </div>
            <div class="stat-card">
                <h3 id="totalPending">₹0</h3>
                <p>💰 Total Pending</p>
            </div>
        </div>
        
        <div class="customer-grid" id="customerGrid">
            <!-- Cards will be inserted here -->
        </div>
    </div>
    
    <!-- Add/Edit Modal -->
    <div class="modal-overlay" id="modal">
        <div class="modal">
            <div class="modal-header">
                <h2 id="modalTitle">Add Customer</h2>
                <button class="modal-close" onclick="closeModal()">&times;</button>
            </div>
            <div class="modal-body">
                <form id="customerForm">
                    <input type="hidden" id="customerId">
                    
                    <div class="form-row">
                        <div class="form-group">
                            <label>Name *</label>
                            <input type="text" id="name" required placeholder="Customer name">
                        </div>
                        <div class="form-group">
                            <label>Phone *</label>
                            <input type="tel" id="phone" required placeholder="9876543210">
                        </div>
                    </div>
                    
                    <div class="form-group">
                        <label>Email</label>
                        <input type="email" id="email" placeholder="email@example.com">
                    </div>
                    
                    <div class="form-group">
                        <label>Address</label>
                        <textarea id="address" rows="2" placeholder="Full address"></textarea>
                    </div>
                    
                    <div class="form-row">
                        <div class="form-group">
                            <label>Loan Amount</label>
                            <input type="number" id="loan_amount" placeholder="50000">
                            </div>

                        <div class="form-group">
                            <label>Pending Amount</label>
                            <input type="number" id="pending_amount" placeholder="25000">
                        </div>
                    </div>
                    
                    <div class="form-row">
                        <div class="form-group">
                            <label>Device Name</label>
                            <input type="text" id="device_name" placeholder="Samsung Galaxy A53">
                        </div>
                        <div class="form-group">
                            <label>Call To (Manager)</label>
                            <input type="tel" id="call_to" placeholder="9068062563">
                        </div>
                    </div>
                    
                    <div class="form-group">
                        <label>Offline Unlock Codes (Comma separated 6-digit codes)</label>
                        <input type="text" id="unlock_codes" placeholder="123456, 654321">
                    </div>
                    
                    <div class="form-group">
                        <label>Freeze Message</label>
                        <input type="text" id="freeze_message" placeholder="Device Locked - Contact Manager">
                    </div>
                    
                    <div class="form-group">
                        <label>Settings</label>
                        <div class="toggle-group">
                            <label class="toggle">
                                <input type="checkbox" id="is_freezed">
                                <span>🔒 Frozen</span>
                            </label>
                            <label class="toggle">
                                <input type="checkbox" id="is_protected" checked>
                                <span>🛡️ Protected</span>
                            </label>
                        </div>
                    </div>
                </form>
            </div>
            <div class="modal-footer">
                <button class="btn" onclick="closeModal()" style="background: var(--bg-input)">Cancel</button>
                <button class="btn btn-primary" onclick="saveCustomer()">Save</button>
            </div>
        </div>
    </div>

    <!-- Setup Modal (QR Code) -->
    <div class="modal-overlay" id="setupModal">
        <div class="modal" style="max-width: 600px;">
            <div class="modal-header">
                <h2>Device Setup</h2>
                <button class="modal-close" onclick="closeSetupModal()">&times;</button>
            </div>
            <div class="modal-body" style="text-align: center;">
                <input type="hidden" id="setupCustomerCode">
                <input type="hidden" id="setupCustomerName">
                
                <div class="tabs">
                    <div class="tab active" id="tab-owner" onclick="switchTab('owner')">Device Owner (QR)</div>
                    <div class="tab" id="tab-admin" onclick="switchTab('admin')">Direct Download</div>
                </div>

                <div id="setup-owner" class="setup-content">
                    <div class="setup-instructions">
                        1. Factory reset the target device.<br>
                        2. Tap "Welcome" screen 7 times to start setup.<br>
                        3. Connect to Wi-Fi and scan this QR code.
                    </div>
                </div>

                <div id="setup-admin" class="setup-content" style="display: none;">
                    <p style="margin-bottom: 10px;">For devices already set up, download the APK directly:</p>
                    <code style="background: rgba(0,0,0,0.3); padding: 5px 10px; border-radius: 4px; display: block; margin-bottom: 10px;">https://api.miniclickcrm.com/admin/app.apk</code>
                </div>

                <div id="qrContainer"></div>
                
                <div style="margin-top: 15px; font-weight: 600; color: var(--primary);">
                    Pairing Code: <span id="displayPairingCode" style="font-size: 1.2rem;">-</span>
                </div>
            </div>
            <div class="modal-footer">
                <button class="btn btn-primary" onclick="closeSetupModal()">Done</button>
            </div>
        </div>
    </div>


    
    <script>
        // Update these with your actual details
        const APP_CONFIG = {
            DOWNLOAD_URL: 'https://api.miniclickcrm.com/admin/app.apk', 
            CHECKSUM: 'mQYHuxWP1EsrUENnlIyB2v22U64BAh_vV1NMO3aWurg',
            COMPONENT: 'com.miniclickcrm.deviceadmin3/com.miniclickcrm.deviceadmin3.receiver.MyAdminReceiver'
        };

        let customers = [];
        
        // Load customers on page load
        document.addEventListener('DOMContentLoaded', loadCustomers);
        
        async function loadCustomers() {
            const res = await fetch('dashboard.php', {
                method: 'POST',
                headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                body: 'action=list'
            });
            const data = await res.json();
            customers = data.customers || [];
            renderCustomers();
            updateStats();
        }
        
        function updateStats() {
            document.getElementById('totalCount').textContent = customers.length;
            document.getElementById('frozenCount').textContent = customers.filter(c => c.is_freezed == 1).length;
            
            const now = Date.now();
            const onlineCount = customers.filter(c => {
                if (!c.last_seen_at) return false;
                const seen = new Date(c.last_seen_at).getTime();
                return (now - seen) < 24 * 60 * 60 * 1000;
            }).length;
            document.getElementById('onlineCount').textContent = onlineCount;
            
            const totalPending = customers.reduce((sum, c) => sum + parseFloat(c.pending_amount || 0), 0);
            document.getElementById('totalPending').textContent = '₹' + totalPending.toLocaleString('en-IN');
        }
        
        function renderCustomers() {
            const grid = document.getElementById('customerGrid');
            
            if (customers.length === 0) {
                grid.innerHTML = `
                    <div class="empty-state" style="grid-column: 1/-1">
                        <svg viewBox="0 0 24 24" fill="currentColor"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>
                        <h3>No customers yet</h3>
                        <p>Click "Add Customer" to get started</p>
                    </div>
                `;
                return;
            }
            
            grid.innerHTML = customers.map(c => {
                const isOnline = c.last_seen_at && (Date.now() - new Date(c.last_seen_at).getTime()) < 24 * 60 * 60 * 1000;
                const lastSeen = c.last_seen_at ? new Date(c.last_seen_at).toLocaleString() : 'Never';
                
                return `
                    <div class="customer-card">
                        <div class="card-header">
                            <div>
                                <h3>${escapeHtml(c.name)}</h3>
                                <div class="phone">📞 ${escapeHtml(c.phone)}</div>
                            </div>
                            <div class="pairing-code">${c.pairing_code}</div>
                        </div>
                        <div class="card-body">
                            <div class="device-info">
                                ${c.is_freezed == 1 ? '<span class="badge badge-frozen">🔒 Frozen</span>' : '<span class="badge badge-active">✅ Active</span>'}
                                ${c.is_protected == 1 ? '<span class="badge badge-protected">🛡️ Protected</span>' : ''}
                                <span class="badge ${isOnline ? 'badge-online' : 'badge-offline'}">${isOnline ? '🟢 Online' : '⚫ Offline'}</span>
                            </div>
                            <div class="info-row">
                                <span class="label">Device</span>
                                <span class="value">${escapeHtml(c.device_name || 'Unknown')}</span>
                            </div>
                            ${c.imei ? `
                            <div class="info-row">
                                <span class="label">IMEI</span>
                                <span class="value" style="font-size: 11px">${c.imei}${c.imei2 ? '<br>' + c.imei2 : ''}</span>
                            </div>` : ''}
                            <div class="info-row">
                                <span class="label">Loan Amount</span>
                                <span class="value">₹${parseFloat(c.loan_amount || 0).toLocaleString('en-IN')}</span>
                            </div>
                            <div class="info-row">
                                <span class="label">Pending</span>
                                <span class="value" style="color: ${c.pending_amount > 0 ? '#f87171' : '#34d399'}">₹${parseFloat(c.pending_amount || 0).toLocaleString('en-IN')}</span>
                            </div>
                            <div class="info-row">
                                <span class="label">Last Seen</span>
                                <span class="value">${lastSeen}</span>
                            </div>
                            <div class="info-row">
                                <span class="label">Message</span>
                                <span class="value" style="font-size: 12px; color: var(--text-muted)">${escapeHtml(c.freeze_message || '-')}</span>
                            </div>
                        </div>
                        <div class="card-actions">
                            <button class="btn btn-sm btn-primary" onclick="editCustomer(${c.id})">✏️ Edit</button>
                            ${c.is_freezed == 1 
                                ? `<button class="btn btn-sm btn-success" onclick="toggleFreeze(${c.id}, 0)">🔓 Unfreeze</button>`
                                : `<button class="btn btn-sm btn-warning" onclick="toggleFreeze(${c.id}, 1)">🔒 Freeze</button>`
                            }
                            <button class="btn btn-sm" style="background: var(--bg-input)" onclick="pushToDevice(${c.id})">📤 Push</button>
                            <button class="btn btn-sm" style="background: #4f46e5; color: white" onclick="openSetupModal('${c.pairing_code}', '${escapeHtml(c.name)}')">📱 Setup</button>
                            <button class="btn btn-sm btn-danger" onclick="deleteCustomer(${c.id})">🗑️</button>
                        </div>
                    </div>
                `;
            }).join('');
        }
        
        function escapeHtml(text) {
            if (!text) return '';
            const div = document.createElement('div');
            div.textContent = text;
            return div.innerHTML;
        }
        
        function openAddModal() {
            document.getElementById('modalTitle').textContent = 'Add Customer';
            document.getElementById('customerId').value = '';
            document.getElementById('customerForm').reset();
            document.getElementById('is_protected').checked = true;
            document.getElementById('modal').classList.add('active');
        }
        
        function closeModal() {
            document.getElementById('modal').classList.remove('active');
        }
        
        async function editCustomer(id) {
            const res = await fetch('dashboard.php', {
                method: 'POST',
                headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                body: `action=get&id=${id}`
            });
            const data = await res.json();
            const c = data.customer;
            
            document.getElementById('modalTitle').textContent = 'Edit Customer';
            document.getElementById('customerId').value = c.id;
            document.getElementById('name').value = c.name || '';
            document.getElementById('phone').value = c.phone || '';
            document.getElementById('email').value = c.email || '';
            document.getElementById('address').value = c.address || '';
            document.getElementById('loan_amount').value = c.loan_amount || '';
            document.getElementById('pending_amount').value = c.pending_amount || '';
            document.getElementById('device_name').value = c.device_name || '';
            document.getElementById('call_to').value = c.call_to || '';
            document.getElementById('freeze_message').value = c.freeze_message || '';
            document.getElementById('unlock_codes').value = c.unlock_codes || '';
            document.getElementById('is_freezed').checked = c.is_freezed == 1;
            document.getElementById('is_protected').checked = c.is_protected == 1;
            
            document.getElementById('modal').classList.add('active');
        }
        
        async function saveCustomer() {
            const id = document.getElementById('customerId').value;
            const formData = new FormData();
            formData.append('action', id ? 'update' : 'add');
            if (id) formData.append('id', id);
            
            ['name', 'phone', 'email', 'address', 'loan_amount', 'pending_amount', 
             'device_name', 'call_to', 'freeze_message', 'unlock_codes'].forEach(f => {
                formData.append(f, document.getElementById(f).value);
            });
            
            formData.append('is_freezed', document.getElementById('is_freezed').checked ? 1 : 0);
            formData.append('is_protected', document.getElementById('is_protected').checked ? 1 : 0);
            
            const res = await fetch('dashboard.php', {
                method: 'POST',
                body: formData
            });
            const data = await res.json();
            
            if (data.success) {
                closeModal();
                loadCustomers();
                
                 if (data.pairing_code && !id) {
                    const name = document.getElementById('name').value;
                     // Auto open setup
                    setTimeout(() => {
                        openSetupModal(data.pairing_code, name);
                    }, 500);
                     toast(`Customer added! Pairing code: ${data.pairing_code}`, 'success');
                } else {
                    toast('Customer updated!', 'success');
                }
            } else {
                toast(data.error || 'Error saving', 'error');
            }
        }
        
        async function toggleFreeze(id, freeze) {
            const formData = new FormData();
            formData.append('action', 'update');
            formData.append('id', id);
            formData.append('is_freezed', freeze);
            
            await fetch('dashboard.php', { method: 'POST', body: formData });
            await loadCustomers();
            
            // Push to device
            await pushToDevice(id, freeze ? 'LOCK_DEVICE' : 'UNLOCK_DEVICE');
        }
        
        async function pushToDevice(id, command = 'SYNC') {
            const formData = new FormData();
            formData.append('action', 'push');
            formData.append('id', id);
            formData.append('command', command);
            
            const res = await fetch('dashboard.php', { method: 'POST', body: formData });
            const data = await res.json();
            
            if (data.success) {
                toast('📤 Pushed to device!', 'success');
            } else {
                toast(data.error || 'Push failed', 'error');
            }
        }
        
        // Setup Modal Logic
        let currentQr = null;
        
        function openSetupModal(code, name) {
            document.getElementById('setupCustomerCode').value = code;
            document.getElementById('setupCustomerName').value = name;
            document.getElementById('displayPairingCode').textContent = code;
            
            document.getElementById('setupModal').classList.add('active');
            
            // Default to Owner mode
            switchTab('owner');
        }
        
        function closeSetupModal() {
            document.getElementById('setupModal').classList.remove('active');
        }
        
        function switchTab(mode) {
            document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
            document.querySelectorAll('.setup-content').forEach(c => c.style.display = 'none');
            
            document.getElementById('tab-' + mode).classList.add('active');
            document.getElementById('setup-' + mode).style.display = 'block';
            
            generateQr(mode);
        }
        
        function generateQr(mode) {
            const container = document.getElementById('qrContainer');
            container.innerHTML = '';
            const code = document.getElementById('setupCustomerCode').value;
            
            let data = '';
            
            if (mode === 'owner') {
                // Device Owner Provisioning JSON
                const json = {
                    "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME": APP_CONFIG.COMPONENT,
                    "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION": APP_CONFIG.DOWNLOAD_URL,
                    "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM": APP_CONFIG.CHECKSUM,
                    "android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED": true,
                    "android.app.extra.PROVISIONING_SKIP_ENCRYPTION": false,
                    "android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE": {
                        "pairing_code": code
                    }
                };
                data = JSON.stringify(json);
            } else {
                // Normal: Just the app url or maybe a deep link?
                // For now, just the download URL
                data = APP_CONFIG.DOWNLOAD_URL;
            }
            
            new QRCode(container, {
                text: data,
                width: 256,
                height: 256
            });
        }
        
        async function deleteCustomer(id) {
            if (!confirm('Delete this customer?')) return;
            
            const formData = new FormData();
            formData.append('action', 'delete');
            formData.append('id', id);
            
            await fetch('dashboard.php', { method: 'POST', body: formData });
            loadCustomers();
            toast('Customer deleted', 'success');
        }
        
        function toast(message, type = 'success') {
            const t = document.createElement('div');
            t.className = `toast ${type}`;
            t.textContent = message;
            document.body.appendChild(t);
            setTimeout(() => t.remove(), 3000);
        }
    </script>
</body>
</html>
