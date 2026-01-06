import { PRICES, CURRENCIES, GST_PERCENTAGE } from './constants.js';
import { formatPrice, calculateMonthlyPrice, calculateDiscountedPrice } from './utils.js';

export function initPricing() {
  const billingSelect = document.getElementById('billingPeriod');
  const countryBtns = document.querySelectorAll('.pp2-country-btn');
  const cards = document.querySelectorAll('.pp2-plan-card');
  
  let currentCountry = 'IN';

  function updatePrices() {
    const billing = billingSelect.value;
    const prices = PRICES[currentCountry];

    cards.forEach((card) => {
      const priceEl = card.querySelector('.pp2-amount');
      const gstEl = card.querySelector('.pp2-gst-amount');
      const originalPriceEl = card.querySelector('.pp2-original-price');
      const orderRange = card.dataset.orders;

      if (priceEl && orderRange) {
        if (orderRange === '25') {
          priceEl.textContent = 'Free';
          if (gstEl) gstEl.style.display = 'none';
          if (originalPriceEl) originalPriceEl.style.display = 'none';
          return;
        }

        if (orderRange === 'custom') {
          priceEl.textContent = 'Custom';
          if (gstEl) {
            gstEl.textContent = 'Contact for pricing';
            gstEl.style.display = 'block';
          }
          if (originalPriceEl) originalPriceEl.style.display = 'none';
          return;
        }

        const { base: pricePerOrder, discount } = prices[billing];
        const [orders] = orderRange.split('-').map(Number);
        const monthlyPrice = calculateMonthlyPrice(orders, pricePerOrder);
        const discountedPrice = calculateDiscountedPrice(monthlyPrice, discount);

        if (discount > 0 && originalPriceEl) {
          const originalPrice = calculateMonthlyPrice(orders, prices.monthly.base);
          originalPriceEl.textContent = formatPrice(originalPrice, currentCountry);
          originalPriceEl.style.display = 'inline';
        } else if (originalPriceEl) {
          originalPriceEl.style.display = 'none';
        }

        const period = billing === 'monthly' ? '/month' : 
                      billing === 'quarterly' ? '/quarter' : '/year';
        
        priceEl.textContent = `${formatPrice(discountedPrice, currentCountry)}${period}`;
        
        if (gstEl) {
          if (currentCountry === 'IN') {
            gstEl.textContent = `+${GST_PERCENTAGE}% GST`;
            gstEl.style.display = 'block';
          } else {
            gstEl.style.display = 'none';
          }
        }
      }
    });
  }

  if (billingSelect) {
    billingSelect.addEventListener('change', updatePrices);
  }

  if (countryBtns) {
    countryBtns.forEach(btn => {
      btn.addEventListener('click', () => {
        countryBtns.forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        currentCountry = btn.dataset.country;
        updatePrices();
      });
    });
  }

  // Initial price update
  updatePrices();
}