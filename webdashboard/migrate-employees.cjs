const https = require('https');

const MYSQL_MANAGER_URL = 'https://calltrack.mylistings.in/ai_mysql_manager.php';
const SECRET_TOKEN = '567898765678';

async function executeSql(sql) {
    return new Promise((resolve, reject) => {
        const postData = JSON.stringify({ sql });

        const options = {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${SECRET_TOKEN}`,
                'Content-Length': Buffer.byteLength(postData)
            }
        };

        const req = https.request(MYSQL_MANAGER_URL, options, (res) => {
            let data = '';

            res.on('data', (chunk) => {
                data += chunk;
            });

            res.on('end', () => {
                try {
                    const result = JSON.parse(data);
                    resolve(result);
                } catch (e) {
                    reject(new Error(`Failed to parse response: ${data}`));
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

async function runMigration() {
    console.log('🔧 Starting Database Migration...\n');

    const migrations = [
        {
            name: 'Add device_id column (if not exists)',
            sql: `ALTER TABLE employees ADD COLUMN IF NOT EXISTS device_id VARCHAR(255) DEFAULT NULL`
        },
        {
            name: 'Add device_id index (if not exists)',
            sql: `ALTER TABLE employees ADD INDEX IF NOT EXISTS idx_device_id (device_id)`
        },
        {
            name: 'Drop email column',
            sql: `ALTER TABLE employees DROP COLUMN IF EXISTS email`
        },
        {
            name: 'Drop department column',
            sql: `ALTER TABLE employees DROP COLUMN IF EXISTS department`
        },
        {
            name: 'Drop role column',
            sql: `ALTER TABLE employees DROP COLUMN IF EXISTS role`
        },
        {
            name: 'Drop calls_today column',
            sql: `ALTER TABLE employees DROP COLUMN IF EXISTS calls_today`
        },
        {
            name: 'Drop department index',
            sql: `ALTER TABLE employees DROP INDEX IF EXISTS idx_department`
        }
    ];

    let successCount = 0;
    let failCount = 0;

    for (const migration of migrations) {
        process.stdout.write(`📝 ${migration.name}... `);

        try {
            const result = await executeSql(migration.sql);

            if (result.status) {
                console.log('✅ Success');
                successCount++;
            } else {
                console.log(`❌ Failed: ${result.error || result.message}`);
                failCount++;
            }
        } catch (error) {
            console.log(`❌ Error: ${error.message}`);
            failCount++;
        }
    }

    console.log('\n' + '='.repeat(50));
    console.log(`✅ Successful: ${successCount}`);
    console.log(`❌ Failed: ${failCount}`);
    console.log('='.repeat(50));

    // Verify the final structure
    console.log('\n🔍 Verifying employees table structure...\n');
    try {
        const result = await executeSql('DESCRIBE employees');
        if (result.status && result.rows) {
            console.log('Current employees table columns:');
            result.rows.forEach(row => {
                console.log(`  - ${row.Field} (${row.Type})`);
            });
        }
    } catch (error) {
        console.log('Failed to verify table structure:', error.message);
    }
}

runMigration().catch(console.error);
