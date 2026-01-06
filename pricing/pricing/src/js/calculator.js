import { PRICES, CURRENCIES } from './constants.js';
import { formatPrice, calculateMonthlyPrice, calculateDiscountedPrice } from './utils.js';

export function initCalculator() {
  const modal = document.getElementById('calculatorModal');
  const calculatorBtns = document.querySelectorAll('.pp2-calculator-btn');
  const closeBtn = document.querySelector('.pp2-close');
  const slider = document.getElementById('orderSlider');
  const sliderValue = document.querySelector('.pp2-slider-value');
  const priceValue = document.querySelector('.pp2-price-value');
  const calcBillingPeriod = document.getElementById('calcBillingPeriod');

  function updateCalculator() {
    const orders = parseInt(slider.value);
    sliderValue.textContent = `${orders} orders/day`;

    const billing = calcBillingPeriod.value;
    const country = document.querySelector('.pp2-country-btn.active').dataset.country;
    const { base: pricePerOrder, discount } = PRICES[country][billing];
    
    const monthlyPrice = calculateMonthlyPrice(orders, pricePerOrder);
    const discountedPrice = calculateDiscountedPrice(monthlyPrice, discount);
    
    const period = billing === 'monthly' ? '/month' : 
                  billing === 'quarterly' ? '/quarter' : '/year';

    priceValue.textContent = `${formatPrice(discountedPrice, country)}${period}`;
  }

  if (calculatorBtns) {
    calculatorBtns.forEach(btn => {
      btn.addEventListener('click', () => {
        if (modal) {
          modal.style.display = 'flex';
          updateCalculator();
        }
      });
    });
  }

  if (closeBtn) {
    closeBtn.addEventListener('click', () => {
      modal.style.display = 'none';
    });
  }

  window.addEventListener('click', (e) => {
    if (e.target === modal) {
      modal.style.display = 'none';
    }
  });

  if (slider) {
    slider.addEventListener('input', updateCalculator);
  }

  if (calcBillingPeriod) {
    calcBillingPeriod.addEventListener('change', updateCalculator);
  }
}