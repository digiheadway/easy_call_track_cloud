const https = require('https');

const FILE_MANAGER_URL = 'https://calltrack.mylistings.in/ai_file_manager.php';

// List of files to move from /callcloud/ to root
const filesToMove = [
    'config.php',
    'utils.php',
    'schema.sql',
    'init_database.php',
    'api/auth.php',
    'api/employees.php',
    'api/calls.php',
    'api/recordings.php',
    'api/reports.php',
    'api/sync_app.php',
    'api/contacts.php',
    'api/numbers.php'
];

async function executeAction(action, data) {
    return new Promise((resolve, reject) => {
        const postData = JSON.stringify({ action, ...data });

        const options = {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Content-Length': Buffer.byteLength(postData)
            }
        };

        const req = https.request(FILE_MANAGER_URL, options, (res) => {
            let responseData = '';

            res.on('data', (chunk) => {
                responseData += chunk;
            });

            res.on('end', () => {
                try {
                    const result = JSON.parse(responseData);
                    resolve(result);
                } catch (e) {
                    reject(new Error(`Failed to parse response: ${responseData}`));
                }
            });
        });

        req.on('error', (error) => {
            reject(error);
        });

        req.write(postData);
        req.end();
    });
}

async function readFile(path) {
    const result = await executeAction('read', { path });
    return result.content || '';
}

async function createFile(path, content) {
    return await executeAction('create_file', { path, content });
}

async function createFolder(path) {
    return await executeAction('create_folder', { path });
}

async function moveFiles() {
    console.log('🔧 Moving files from /callcloud/ to root...\n');

    try {
        // Create /api folder at root if it doesn't exist
        console.log('📁 Creating /api folder...');
        await createFolder('api');
        console.log('✅ Created /api folder\n');

        let successCount = 0;
        let failCount = 0;

        for (const file of filesToMove) {
            const oldPath = `callcloud/${file}`;
            const newPath = file;

            process.stdout.write(`📄 Moving ${oldPath} → ${newPath}... `);

            try {
                // Read content from old location
                const content = await readFile(oldPath);

                if (!content) {
                    console.log('⚠️  File not found or empty');
                    failCount++;
                    continue;
                }

                // Write to new location
                const result = await createFile(newPath, content);

                if (result.status) {
                    console.log('✅');
                    successCount++;
                } else {
                    console.log(`❌ ${result.message || 'Failed'}`);
                    failCount++;
                }
            } catch (error) {
                console.log(`❌ ${error.message}`);
                failCount++;
            }
        }

        console.log('\n' + '='.repeat(50));
        console.log(`✅ Successfully moved: ${successCount}`);
        console.log(`❌ Failed: ${failCount}`);
        console.log('='.repeat(50));
        console.log('\n✨ Files moved to root directory!');
        console.log('\nYou can now test the API at:');
        console.log('https://calltrack.mylistings.in/api/sync_app.php');

    } catch (error) {
        console.error('❌ Error:', error.message);
        process.exit(1);
    }
}

moveFiles();
