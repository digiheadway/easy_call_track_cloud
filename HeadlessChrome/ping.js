const puppeteer = require('puppeteer');

(async () => {
  const browser = await puppeteer.launch();
  const page = await browser.newPage();
  
  // Webhook URL provided
  const url = 'https://webhook.site/87ac0d40-b509-4437-8cee-0cce5afc281b';

  try {
    
    await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 5000 });
    console.log(`Successfully reached the webhook site: ${url}`);
  } catch (error) {
    console.error(`Failed to reach the webhook site: ${error.message}`);
  }

  await browser.close();
})();
