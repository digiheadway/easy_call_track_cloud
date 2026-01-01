#!/usr/bin/env node

/**
 * CallCloud Admin Backend Deployment Script
 * Uses File Manager API to upload all backend files
 * Uses MySQL Manager API to initialize database
 */

const https = require('https');
const fs = require('fs');
const path = require('path');

// Configuration
const CONFIG = {
    SECRET_TOKEN: 'callcloud_admin_secret_2024', // Secure token for backend
    FILE_MANAGER_URL: 'https://calltrack.mylistings.in/ai_file_manager.php',
    MYSQL_MANAGER_URL: 'https://calltrack.mylistings.in/ai_mysql_manager.php',
    REMOTE_BASE_PATH: '', // Empty = root directory
    LOCAL_PHP_DIR: path.join(__dirname, 'php')
};

// API Helper
async function apiRequest(url, data) {
    return new Promise((resolve, reject) => {
        const jsonData = JSON.stringify(data);
        const urlObj = new URL(url);

        const options = {
            hostname: urlObj.hostname,
            port: 443,
            path: urlObj.pathname,
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${CONFIG.SECRET_TOKEN}`,
                'Content-Type': 'application/json',
                'Content-Length': jsonData.length
            }
        };

        const req = https.request(options, (res) => {
            let body = '';
            res.on('data', (chunk) => body += chunk);
            res.on('end', () => {
                try {
                    resolve(JSON.parse(body));
                } catch (e) {
                    reject(new Error('Invalid JSON response'));
                }
            });
        });

        req.on('error', reject);
        req.write(jsonData);
        req.end();
    });
}

// File Manager Functions
async function createFolder(remotePath) {
    console.log(`📁 Creating folder: ${remotePath}`);
    const result = await apiRequest(CONFIG.FILE_MANAGER_URL, {
        action: 'create_folder',
        path: remotePath
    });

    if (result.status) {
        console.log(`   ✅ Success`);
    } else {
        console.log(`   ⚠️  ${result.message} (may already exist)`);
    }
    return result;
}

async function uploadFile(localPath, remotePath) {
    const fileName = path.basename(localPath);
    console.log(`📤 Uploading: ${fileName}`);

    const content = fs.readFileSync(localPath, 'utf8');

    const result = await apiRequest(CONFIG.FILE_MANAGER_URL, {
        action: 'create_file',
        path: remotePath,
        content: content
    });

    if (result.status) {
        console.log(`   ✅ Success`);
    } else {
        console.log(`   ❌ Failed: ${result.message}`);
    }
    return result;
}

// MySQL Functions
async function executeSQL(sql) {
    return await apiRequest(CONFIG.MYSQL_MANAGER_URL, { sql });
}

async function initializeDatabase() {
    console.log('\n🗄️  Initializing Database...\n');

    const schemaPath = path.join(CONFIG.LOCAL_PHP_DIR, 'schema.sql');
    const schema = fs.readFileSync(schemaPath, 'utf8');

    // Split SQL statements
    const statements = schema
        .split(';')
        .map(s => s.trim())
        .filter(s => s.length > 0 && !s.startsWith('--'));

    console.log(`Found ${statements.length} SQL statements to execute\n`);

    let success = 0;
    let failed = 0;

    for (let i = 0; i < statements.length; i++) {
        const stmt = statements[i];
        const tableName = stmt.match(/CREATE TABLE.*?`?(\w+)`?/i);
        const name = tableName ? tableName[1] : `Statement ${i + 1}`;

        process.stdout.write(`   Executing ${name}... `);

        try {
            const result = await executeSQL(stmt + ';');

            if (result.status) {
                console.log('✅');
                success++;
            } else {
                console.log('❌');
                console.log(`      Error: ${result.error || result.message}`);
                failed++;
            }
        } catch (error) {
            console.log('❌');
            console.log(`      Error: ${error.message}`);
            failed++;
        }
    }

    console.log(`\n📊 Database Initialization Complete`);
    console.log(`   Success: ${success} | Failed: ${failed}\n`);

    return { success, failed };
}

// Main Deployment Function
async function deploy() {
    console.log('🚀 CallCloud Admin Backend Deployment\n');
    console.log('='.repeat(50) + '\n');

    // Check secret token
    if (CONFIG.SECRET_TOKEN === 'CHANGE_THIS_SECRET_TOKEN') {
        console.log('❌ Error: Please set your SECRET_TOKEN in this script!');
        console.log('   Update the SECRET_TOKEN at the top of deploy.js\n');
        process.exit(1);
    }

    try {
        // Step 1: Create folder structure
        console.log('📂 Creating folder structure...\n');
        await createFolder(CONFIG.REMOTE_BASE_PATH);
        await createFolder(`${CONFIG.REMOTE_BASE_PATH}/api`);
        console.log('');

        // Step 2: Upload PHP files
        console.log('📤 Uploading PHP files...\n');

        const filesToUpload = [
            { local: 'config.php', remote: `${CONFIG.REMOTE_BASE_PATH}/config.php` },
            { local: 'utils.php', remote: `${CONFIG.REMOTE_BASE_PATH}/utils.php` },
            { local: 'schema.sql', remote: `${CONFIG.REMOTE_BASE_PATH}/schema.sql` },
            { local: 'init_database.php', remote: `${CONFIG.REMOTE_BASE_PATH}/init_database.php` },
            { local: 'api/auth.php', remote: `${CONFIG.REMOTE_BASE_PATH}/api/auth.php` },
            { local: 'api/employees.php', remote: `${CONFIG.REMOTE_BASE_PATH}/api/employees.php` },
            { local: 'api/calls.php', remote: `${CONFIG.REMOTE_BASE_PATH}/api/calls.php` },
            { local: 'api/recordings.php', remote: `${CONFIG.REMOTE_BASE_PATH}/api/recordings.php` },
            { local: 'api/reports.php', remote: `${CONFIG.REMOTE_BASE_PATH}/api/reports.php` }
        ];

        for (const file of filesToUpload) {
            const localPath = path.join(CONFIG.LOCAL_PHP_DIR, file.local);
            await uploadFile(localPath, file.remote);
        }

        // Step 3: Initialize database
        await initializeDatabase();

        // Step 4: Success message
        console.log('='.repeat(50));
        console.log('✅ Deployment Complete!\n');
        console.log('Next steps:');
        console.log('1. Update frontend API URL in src/api/client.ts:');
        console.log(`   const API_BASE_URL = 'https://calltrack.mylistings.in${CONFIG.REMOTE_BASE_PATH}/api';\n`);
        console.log('2. Test the API:');
        console.log(`   https://calltrack.mylistings.in${CONFIG.REMOTE_BASE_PATH}/api/auth.php?action=verify\n`);
        console.log('3. Open your dashboard and create an account!\n');

    } catch (error) {
        console.log('\n❌ Deployment failed!');
        console.log(`Error: ${error.message}\n`);
        process.exit(1);
    }
}

// Run deployment
deploy();
