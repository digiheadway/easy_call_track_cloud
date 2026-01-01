const https = require('https');

const FILE_MANAGER_URL = 'https://calltrack.mylistings.in/ai_file_manager.php';

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

async function deleteFolder(path) {
    return await executeAction('delete_folder', { path });
}

async function cleanup() {
    console.log('🗑️  Cleaning up old /callcloud/ directory...\n');

    try {
        process.stdout.write('Deleting /callcloud/ folder... ');

        const result = await deleteFolder('callcloud');

        if (result.status) {
            console.log('✅');
            console.log('\n✨ Cleanup complete!');
            console.log('Old /callcloud/ directory has been removed.');
        } else {
            console.log(`❌ ${result.message || 'Failed'}`);
        }

    } catch (error) {
        console.error('❌ Error:', error.message);
        process.exit(1);
    }
}

cleanup();
