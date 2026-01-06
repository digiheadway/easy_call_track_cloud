<?php

$host = "localhost";
$dbname = "u240376517_propdb";
$username = "u240376517_propdb";
$password = "Y*Q;5gIOp2";

// Allow AJAX from same origin (adjust if needed)
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Headers: Content-Type");
header("Content-Type: application/json; charset=utf-8");

// If request is POST then handle insert and return JSON
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_SERVER['HTTP_X_REQUESTED_WITH'])) {
    // Read JSON body
    $input = json_decode(file_get_contents('php://input'), true);

    // Basic server-side defaults & validation (we enforce required fields)
    $Email = trim($input['Email'] ?? 'myuptownproperties@gmail.com');
    $CUSTOMER_NAME = trim($input['CUSTOMER_NAME'] ?? '');
    $task_pending = trim($input['task_pending'] ?? '');
    $city = trim($input['city'] ?? '');
    $remark = trim($input['remark'] ?? '');
    $phone = trim($input['phone'] ?? '12345');
    $chances = trim($input['chances'] ?? '');
    $CHEQUE_NO = trim($input['CHEQUE_NO'] ?? '');
    // Enforce defaults regardless of client input:
    $CHEQUE_DATE = '2025-12-11';            // default as requested
    $CHEQUE_AMOUNT = 500000;                // always 500000
    $BANK_NAME = trim($input['BANK_NAME'] ?? '');
    $CP_NAME = 'Parmod Singh';              // always Parmod Singh
    $profile = 2;                           // default 2

    // Simple validation
    if ($CUSTOMER_NAME === '') {
        echo json_encode(['success' => false, 'error' => 'Customer name is required.']);
        exit;
    }

    // Connect and insert using prepared statement
    $conn = new mysqli($host, $username, $password, $dbname);
    if ($conn->connect_error) {
        echo json_encode(['success' => false, 'error' => 'DB connect error: '.$conn->connect_error]);
        exit;
    }

    // Note: columns with spaces/backticks used exactly as in your DB
    $sql = "INSERT INTO `godrej` (
                `Email`,
                `CUSTOMER NAME`,
                `task_pending`,
                `city`,
                `remark`,
                `phone`,
                `chances`,
                `CHEQUE NO`,
                `CHEQUE DATE`,
                `CHEQUE AMOUNT`,
                `BANK NAME`,
                `CP NAME`,
                `profile`
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    $stmt = $conn->prepare($sql);
    if (!$stmt) {
        echo json_encode(['success' => false, 'error' => 'Prepare failed: '.$conn->error]);
        $conn->close();
        exit;
    }

    // Bind params: s = string, i = integer
    $stmt->bind_param(
        'sssssssssssss',
        $Email,
        $CUSTOMER_NAME,
        $task_pending,
        $city,
        $remark,
        $phone,
        $chances,
        $CHEQUE_NO,
        $CHEQUE_DATE,
        $CHEQUE_AMOUNT,
        $BANK_NAME,
        $CP_NAME,
        $profile
    );

    if ($stmt->execute()) {
        echo json_encode(['success' => true, 'message' => 'Row added', 'insert_id' => $stmt->insert_id]);
    } else {
        echo json_encode(['success' => false, 'error' => $stmt->error]);
    }

    $stmt->close();
    $conn->close();
    exit;
}

// If not POST (normal GET) — return the HTML page (we print HTML and include JS).
?>
<!doctype html>
<html>
<head>
  <meta charset="utf-8" />
  <title>Add Godrej Record — Smooth One-Page</title>
  <meta name="viewport" content="width=device-width,initial-scale=1" />
  <style>
    /* simple clean form style */
    body { font-family: Arial, sans-serif; padding: 20px; background:#f7f7f7; }
    .card { background: #fff; max-width:700px; margin:0 auto; padding:20px; border-radius:8px; box-shadow: 0 6px 18px rgba(0,0,0,0.06); }
    label { display:block; margin-top:12px; font-weight:600; }
    input[type="text"], input[type="email"], input[type="date"], input[type="number"], textarea, select {
      width:100%; padding:10px; margin-top:6px; border:1px solid #ddd; border-radius:6px; box-sizing:border-box;
    }
    .row { display:flex; gap:12px; }
    .row .col { flex:1; }
    button { margin-top:16px; padding:10px 16px; border:0; background:#2b7cff; color:white; border-radius:6px; cursor:pointer; }
    .small { font-size:13px; color:#666; margin-top:6px; }
    .message { margin-top:12px; padding:10px; border-radius:6px; display:none; }
    .message.success { background:#e6ffed; color:#0a6d2f; display:block; }
    .message.error { background:#ffe6e6; color:#8a1a1a; display:block; }
  </style>
</head>
<body>
  <div class="card">
    <h2>Add New Godrej Row</h2>
    <p class="small">All values saved server-side with defaults for date, amount, CP name and profile. (Smooth add — no page reload.)</p>

    <form id="addForm" onsubmit="return false;">
      <label>Email (default)</label>
      <input type="email" id="Email" value="myuptownproperties@gmail.com" />

      <label>Customer Name *</label>
      <input type="text" id="CUSTOMER_NAME" placeholder="eg. Rohit Kumar" required />

      <label>Task Pending</label>
      <input type="text" id="task_pending" placeholder="eg. Follow up / Need Cheque" />

      <div class="row">
        <div class="col">
          <label>City</label>
          <input type="text" id="city" placeholder="eg. Panipat" />
        </div>
        <div class="col">
          <label>Phone (default 12345)</label>
          <input type="text" id="phone" value="12345" />
        </div>
      </div>

      <label>Remark</label>
      <textarea id="remark" rows="3" placeholder="any note"></textarea>

      <div class="row">
        <div class="col">
          <label>Chances</label>
          <input type="text" id="chances" placeholder="eg. 70% / 100%" />
        </div>
        <div class="col">
          <label>Bank Name</label>
          <input type="text" id="BANK_NAME" placeholder="eg. HDFC Bank" />
        </div>
      </div>

      <label>Cheque No.</label>
      <input type="text" id="CHEQUE_NO" placeholder="eg. 114113" />

      <!-- These are defaults but shown readonly so user sees them -->
      <div class="row">
        <div class="col">
          <label>Cheque Date (server default)</label>
          <input type="date" id="CHEQUE_DATE" value="2025-12-11"  />
        </div>
        <div class="col">
          <label>Cheque Amount (always)</label>
          <input type="number" id="CHEQUE_AMOUNT" value="500000" readonly />
        </div>
      </div>

      <label>CP Name (always)</label>
      <input type="text" id="CP_NAME" value="Parmod Singh"  />

      <label>Profile (default)</label>
      <input type="number" id="profile" value="2"  />

      <button id="sendBtn">Add Row</button>

      <div id="msg" class="message"></div>
    </form>
  </div>

<script>
(async function(){
  const form = document.getElementById('addForm');
  const btn = document.getElementById('sendBtn');
  const msg = document.getElementById('msg');

  function showMessage(text, type='success') {
    msg.className = 'message ' + (type === 'error' ? 'error' : 'success');
    msg.textContent = text;
    // hide after 4s
    setTimeout(()=> { msg.style.display = msg.textContent ? 'block' : 'none'; }, 10);
    setTimeout(()=> { msg.style.display = 'none'; }, 4000);
  }

  btn.addEventListener('click', async function(){
    btn.disabled = true;
    btn.textContent = 'Saving...';

    // collect values
    const payload = {
      Email: document.getElementById('Email').value.trim() || 'myuptownproperties@gmail.com',
      CUSTOMER_NAME: document.getElementById('CUSTOMER_NAME').value.trim(),
      task_pending: document.getElementById('task_pending').value.trim(),
      city: document.getElementById('city').value.trim(),
      remark: document.getElementById('remark').value.trim(),
      phone: document.getElementById('phone').value.trim() || '12345',
      chances: document.getElementById('chances').value.trim(),
      CHEQUE_NO: document.getElementById('CHEQUE_NO').value.trim(),
      // we still send date/amount but server enforces defaults
      CHEQUE_DATE: document.getElementById('CHEQUE_DATE').value,
      CHEQUE_AMOUNT: parseInt(document.getElementById('CHEQUE_AMOUNT').value, 10),
      BANK_NAME: document.getElementById('BANK_NAME').value.trim(),
      CP_NAME: document.getElementById('CP_NAME').value,
      profile: parseInt(document.getElementById('profile').value, 10)
    };

    // simple client validation
    if (!payload.CUSTOMER_NAME) {
      showMessage('Customer name is required.', 'error');
      btn.disabled = false;
      btn.textContent = 'Add Row';
      return;
    }

    try {
      const response = await fetch(window.location.href, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-Requested-With': 'XMLHttpRequest'
        },
        body: JSON.stringify(payload)
      });
      const data = await response.json();
      if (data.success) {
        showMessage('Saved ✓ (ID: ' + (data.insert_id || 'n/a') + ')');
        // clear form fields except defaults / readonly
        document.getElementById('CUSTOMER_NAME').value = '';
        document.getElementById('task_pending').value = '';
        document.getElementById('city').value = '';
        document.getElementById('remark').value = '';
        document.getElementById('phone').value = '12345';
        document.getElementById('chances').value = '';
        document.getElementById('CHEQUE_NO').value = '';
        document.getElementById('BANK_NAME').value = '';
      } else {
        showMessage('Error: ' + (data.error || 'unknown'), 'error');
      }
    } catch (err) {
      showMessage('Network error: ' + err.message, 'error');
    }

    btn.disabled = false;
    btn.textContent = 'Add Row';
  });
})();
</script>
</body>
</html>
