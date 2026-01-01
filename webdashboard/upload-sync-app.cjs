const https = require('https');
const fs = require('fs');
const path = require('path');

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

async function uploadFile(localPath, remotePath) {
    const content = fs.readFileSync(localPath, 'utf8');
    return await executeAction('update_file', { path: remotePath, content });
}

async function upload() {
    console.log('📤 Uploading updated sync_app.php...\n');

    try {
        const localFile = path.join(__dirname, 'php/api/sync_app.php');
        const remotePath = 'api/sync_app.php';

        process.stdout.write(`Uploading ${remotePath}... `);

        const result = await uploadFile(localFile, remotePath);

        if (result.status) {
            console.log('✅');
            console.log('\n✨ Upload complete!');
            console.log('sync_app.php has been updated with verify_pairing_code action.');
        } else {
            console.log(`❌ ${result.message || 'Failed'}`);
        }

    } catch (error) {
        console.error('❌ Error:', error.message);
        process.exit(1);
    }
}

upload();
