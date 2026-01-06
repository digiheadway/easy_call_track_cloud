import { initPricing } from './pricing.js';
import { initCalculator } from './calculator.js';

function initApp() {
  initPricing();
  initCalculator();
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initApp);
} else {
  initApp();
}