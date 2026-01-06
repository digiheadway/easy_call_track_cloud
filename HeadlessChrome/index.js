const { Builder, By, Key, until } = require('selenium-webdriver');
require('chromedriver');

// Pixel 7 emulation settings
const pixel7Emulation = {
  deviceMetrics: {
    width: 412,   // Pixel 7 width in pixels
    height: 915,  // Pixel 7 height in pixels
    pixelRatio: 3, // Pixel ratio (DPR)
  },
  userAgent:
    'Mozilla/5.0 (Linux; Android 12; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.45 Mobile Safari/537.36',
};

async function run() {
  // Create the ChromeOptions object
  const chromeOptions = new (require('selenium-webdriver/chrome').Options)();

  // Set mobile emulation and DevTools settings
  chromeOptions.setMobileEmulation(pixel7Emulation);
  chromeOptions.addArguments('--auto-open-devtools-for-tabs'); // Opens DevTools (inspect view)

  const driver = await new Builder()
    .forBrowser('chrome')
    .setChromeOptions(chromeOptions) // Use the chromeOptions with mobile emulation and DevTools
    .build();

  try {
    await driver.get('https://www.terabox.app/wap/share/filelist?surl=LKnUAElleaenD4yM1XmsaA');
    
    // Give some time for the page to load before we execute the function
    await driver.sleep(3000);

    // Open DevTools and toggle the device toolbar manually via JavaScript
    await driver.executeScript(() => {
      const devTools = document.querySelector('body');
      if (devTools) {
        document.body.style.transform = 'scale(1)'; // This helps in making the toolbar visible when toggled.
      }

      // Optional: You can add other UI customizations here if needed.
    });

    // Simulate checking the element and sending data using JavaScript
    await driver.executeScript(checkElementAndSendData.toString());

    // Optionally, you can wait for a while to observe the action or perform further operations
    await driver.sleep(5000);  // Wait for 5 seconds

    // Take a screenshot
    await driver.takeScreenshot().then(function (image) {
      require('fs').writeFileSync('pixel7_screenshot.png', image, 'base64');
    });

    // Keep running the function indefinitely
    console.log("Browser will remain open and continue running the function...");

    // Set an interval for the function to repeat on each page reload
    setInterval(async () => {
      console.log('Reloading the page and re-executing the function...');
      await driver.navigate().refresh();  // Refresh the page
      await driver.sleep(3000);  // Wait for the page to reload
      await driver.executeScript(checkElementAndSendData.toString()); // Re-run the function
    }, 10000); // Repeat every 10 seconds (adjust as needed)

    // Keep the browser open
    await driver.sleep(1000000); // Let it run for a long time before quitting

  } catch (error) {
    console.error("Error:", error);
  }
}

// Your existing function as it is
function checkElementAndSendData() {
  const interval = setInterval(() => {
    // Locate the element by ID
    const element = document.querySelector('#openNaBtn');

    if (element) {
      // Stop the interval
      clearInterval(interval);

      // Get the value of the data-clipboard-text attribute
      const dataClipboardText = element.getAttribute('data-clipboard-text');

      if (dataClipboardText) {
        // Extract link_id and surl using URLSearchParams
        const urlParams = new URLSearchParams(dataClipboardText);
        const linkId = urlParams.get('tera_link_id');
        const surl = urlParams.get('surl');

        // Log extracted values (you can remove this in production)
        console.log('Link ID:', linkId);
        console.log('SURL:', surl);

        // Send the data to the webhook
        fetch(
          `https://123movies.moviesda10.com/tj2/webhook_link_id.php?link_id=${linkId}&surl=${surl}`,
          {
            method: 'GET',
            headers: {
              'Content-Type': 'application/json',
            },
          }
        )
          .then((response) => {
            if (response.ok) {
              console.log('Data sent successfully:', dataClipboardText);
            } else {
              console.error('Failed to send data');
            }
          })
          .catch((error) => console.error('Error:', error));
      }

      // Click the button
      element.click();

      // Refresh the page after 2 seconds
      setTimeout(() => {
        location.reload();
      }, 1000);
    }
  }, 1000); // Run every 1 second
}

// Call the run function
run();
