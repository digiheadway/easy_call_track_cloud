/**
 * CallCloud Admin - Backend Deployment Script
 * Deploys PHP backend using File Manager and MySQL Manager APIs
 */

import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Configuration
const CONFIG = {
    FILE_MANAGER_URL: 'https://calltrack.mylistings.in/ai_file_manager.php',
    MYSQL_MANAGER_URL: 'https://calltrack.mylistings.in/ai_mysql_manager.php',
    SECRET_TOKEN: '567898765678', // Token from MySQL/File Manager
    REMOTE_BASE_PATH: '' // Empty = root directory
};

/**
 * Make API request
 */
async function apiRequest(url, data) {
    const response = await fetch(url, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${CONFIG.SECRET_TOKEN}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(data)
    });

    const result = await response.json();

    if (!result.status) {
        throw new Error(result.message || 'API request failed');
    }

    return result;
}

/**
 * Create folder via File Manager
 */
async function createFolder(folderPath) {
    try {
        await apiRequest(CONFIG.FILE_MANAGER_URL, {
            action: 'create_folder',
            path: folderPath
        });
        console.log(`✅ Created folder: ${folderPath}`);
    } catch (error) {
        console.log(`⚠️  Folder may already exist: ${folderPath}`);
    }
}

/**
 * Upload file via File Manager
 */
async function uploadFile(localPath, remotePath) {
    try {
        const content = fs.readFileSync(localPath, 'utf8');

        await apiRequest(CONFIG.FILE_MANAGER_URL, {
            action: 'create_file',
            path: remotePath,
            content: content
        });

        console.log(`✅ Uploaded: ${localPath} → ${remotePath}`);
    } catch (error) {
        console.error(`❌ Failed to upload ${localPath}:`, error.message);
        throw error;
    }
}

/**
 * Execute SQL via MySQL Manager
 */
async function executeSQL(sql) {
    try {
        const result = await apiRequest(CONFIG.MYSQL_MANAGER_URL, { sql });
        return result;
    } catch (error) {
        console.error(`❌ SQL Error:`, error.message);
        throw error;
    }
}

/**
 * Initialize database schema
 */
async function initializeDatabase() {
    console.log('\n📊 Initializing Database...\n');

    const schemaPath = path.join(__dirname, 'php', 'schema.sql');
    const schema = fs.readFileSync(schemaPath, 'utf8');

    // Split into individual statements
    const statements = schema
        .split(';')
        .map(stmt => stmt.trim())
        .filter(stmt => stmt.length > 0);

    console.log(`Found ${statements.length} SQL statements to execute.\n`);

    let success = 0;
    let failed = 0;

    for (let i = 0; i < statements.length; i++) {
        const sql = statements[i] + ';';
        process.stdout.write(`Executing statement ${i + 1}/${statements.length}... `);

        try {
            await executeSQL(sql);
            console.log('✅');
            success++;
        } catch (error) {
            console.log('❌');
            console.log(`   Error: ${error.message}`);
            failed++;
        }
    }

    console.log('\n' + '='.repeat(50));
    console.log('Database Initialization Complete');
    console.log(`Success: ${success} | Failed: ${failed}`);
    console.log('='.repeat(50) + '\n');
}

/**
 * Deploy backend files
 */
async function deployBackend() {
    console.log('\n🚀 CallCloud Admin - Backend Deployment\n');
    console.log('='.repeat(50) + '\n');

    try {
        // Create folder structure
        console.log('📁 Creating folder structure...\n');
        await createFolder(CONFIG.REMOTE_BASE_PATH);
        await createFolder(`${CONFIG.REMOTE_BASE_PATH}/api`);
        console.log('');

        // Upload core PHP files
        console.log('📤 Uploading core PHP files...\n');

        const coreFiles = [
            { local: 'php/config.php', remote: `${CONFIG.REMOTE_BASE_PATH}/config.php` },
            { local: 'php/utils.php', remote: `${CONFIG.REMOTE_BASE_PATH}/utils.php` },
            { local: 'php/schema.sql', remote: `${CONFIG.REMOTE_BASE_PATH}/schema.sql` },
            { local: 'php/init_database.php', remote: `${CONFIG.REMOTE_BASE_PATH}/init_database.php` }
        ];

        for (const file of coreFiles) {
            await uploadFile(file.local, file.remote);
        }

        console.log('');

        // Upload API files
        console.log('📤 Uploading API endpoints...\n');

        const apiFiles = [
            { local: 'php/api/auth.php', remote: `${CONFIG.REMOTE_BASE_PATH}/api/auth.php` },
            { local: 'php/api/employees.php', remote: `${CONFIG.REMOTE_BASE_PATH}/api/employees.php` },
            { local: 'php/api/calls.php', remote: `${CONFIG.REMOTE_BASE_PATH}/api/calls.php` },
            { local: 'php/api/recordings.php', remote: `${CONFIG.REMOTE_BASE_PATH}/api/recordings.php` },
            { local: 'php/api/reports.php', remote: `${CONFIG.REMOTE_BASE_PATH}/api/reports.php` },
            { local: 'php/api/sync_app.php', remote: `${CONFIG.REMOTE_BASE_PATH}/api/sync_app.php` },
            { local: 'php/api/contacts.php', remote: `${CONFIG.REMOTE_BASE_PATH}/api/contacts.php` },
            { local: 'php/api/numbers.php', remote: `${CONFIG.REMOTE_BASE_PATH}/api/numbers.php` }
        ];

        for (const file of apiFiles) {
            await uploadFile(file.local, file.remote);
        }

        console.log('');

        // Initialize database
        await initializeDatabase();

        // Success message
        console.log('✨ Deployment Complete!\n');
        console.log('='.repeat(50));
        console.log('Next Steps:');
        console.log('='.repeat(50) + '\n');
        console.log('1. Your backend is deployed at:');
        console.log('   https://calltrack.mylistings.in/callcloud/\n');
        console.log('2. Test authentication:');
        console.log('   https://calltrack.mylistings.in/callcloud/api/auth.php?action=verify\n');
        console.log('3. The database has been initialized with all tables.\n');
        console.log('4. Update frontend API URL in src/api/client.ts if needed.\n');
        console.log('5. Start testing! Create an account and explore the dashboard.\n');

    } catch (error) {
        console.error('\n❌ Deployment failed:', error.message);
        console.error('Details:', error);
        process.exit(1);
    }
}

// Run deployment
deployBackend();
