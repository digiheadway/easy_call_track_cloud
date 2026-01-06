const { Builder, By, until } = require('selenium-webdriver');
require('chromedriver');

(async () => {
  const driver = await new Builder().forBrowser('chrome').build();
  await driver.get('https://www.terabox.app/wap/share/filelist?surl=LKnUAElleaenD4yM1XmsaA');
  await driver.sleep(5000);  // Wait for 5 seconds
  await driver.takeScreenshot().then(function (image) {
    require('fs').writeFileSync('selenium_screenshot.png', image, 'base64');
  });
  await driver.quit();
})();
