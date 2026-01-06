// Constants
// const PRICES = {
//   monthly: { base: 10, discount: 0 },
//   quarterly: { base: 40, discount: 12 },
//   yearly: { base: 120, discount: 24 },
// };

// const DAYS_IN_MONTH = 30;

// Utility functions
function formatPrice(amount) {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  })
    .format(amount)
    .replace('INR', '₹');
}

function calculateMonthlyPrice(orders, pricePerOrder) {
  return orders * pricePerOrder;
}

function calculateDiscountedPrice(originalPrice, discountPercentage) {
  return originalPrice * (1 - discountPercentage / 100);
}

// Initialize the application
document.addEventListener('DOMContentLoaded', () => {
  // DOM Elements
  const toggleBtns = document.querySelectorAll('.pp2-toggle-btn');
  const calculatorBtns = document.querySelectorAll('.pp2-calculator-btn');
  const modal = document.getElementById('calculatorModal');
  const closeBtn = document.querySelector('.pp2-close');
  const slider = document.getElementById('orderSlider');
  const sliderValue = document.querySelector('.pp2-slider-value');
  const priceValue = document.querySelector('.pp2-price-value');
  const plansScroll = document.querySelector('.pp2-plans-scroll');

  // Billing toggle functionality
  function updatePrices(billing) {
    const { base: pricePerOrder, discount } = PRICES[billing];
    const cards = document.querySelectorAll('.pp2-plan-card');

    cards.forEach((card) => {
      const priceEl = card.querySelector('.pp2-amount');
      const originalPriceEl = card.querySelector('.pp2-original-price');
      const orderRange = card.dataset.orders;

      if (priceEl && orderRange && orderRange !== 'custom') {
        const [minOrders] = orderRange.split('-').map(Number);
        const monthlyPrice = calculateMonthlyPrice(minOrders, pricePerOrder);
        const discountedPrice = calculateDiscountedPrice(monthlyPrice, discount);

        if (discount > 0 && originalPriceEl) {
          const originalPrice = calculateMonthlyPrice(minOrders, PRICES.monthly.base);
          originalPriceEl.textContent = formatPrice(originalPrice);
          originalPriceEl.style.display = 'inline';
        } else if (originalPriceEl) {
          originalPriceEl.style.display = 'none';
        }

        priceEl.textContent = formatPrice(discountedPrice);
      }
    });

    // Update discount badges
    toggleBtns.forEach((btn) => {
      const btnDiscount = PRICES[btn.dataset.billing].discount;
      const discountBadge = btn.querySelector('.pp2-discount-badge');
      if (discountBadge) {
        if (btnDiscount > 0) {
          discountBadge.textContent = `Save ${btnDiscount}%`;
          discountBadge.style.display = 'inline-flex';
        } else {
          discountBadge.style.display = 'none';
        }
      }
    });
  }

  toggleBtns.forEach((btn) => {
    btn.addEventListener('click', () => {
      toggleBtns.forEach((b) => b.classList.remove('active'));
      btn.classList.add('active');
      updatePrices(btn.dataset.billing);
    });
  });

  // Calculator functionality
  function updateCalculator() {
    const orders = parseInt(slider.value);
    sliderValue.textContent = `${orders} orders/day`;

    const activeBilling = document.querySelector('.pp2-toggle-btn.active').dataset.billing;
    const { base: pricePerOrder, discount } = PRICES[activeBilling];
    const monthlyPrice = calculateMonthlyPrice(orders, pricePerOrder);
    const discountedPrice = calculateDiscountedPrice(monthlyPrice, discount);

    if (orders > 2000) {
      priceValue.textContent = 'Contact us for custom pricing';
    } else {
      priceValue.textContent = `${formatPrice(discountedPrice)}/month`;
    }
  }

  // Modal handlers
  calculatorBtns.forEach((btn) => {
    btn.addEventListener('click', () => {
      modal.style.display = 'flex';
      if (btn.dataset.orders) {
        slider.value = btn.dataset.orders;
        updateCalculator();
      }
    });
  });

  closeBtn.addEventListener('click', () => modal.style.display = 'none');
  window.addEventListener('click', (e) => {
    if (e.target === modal) modal.style.display = 'none';
  });

  slider.addEventListener('input', updateCalculator);

  // Smooth scrolling for plan cards
  let isDown = false;
  let startX;
  let scrollLeft;

  plansScroll.addEventListener('mousedown', (e) => {
    isDown = true;
    plansScroll.style.cursor = 'grabbing';
    startX = e.pageX - plansScroll.offsetLeft;
    scrollLeft = plansScroll.scrollLeft;
  });

  plansScroll.addEventListener('mouseleave', () => {
    isDown = false;
    plansScroll.style.cursor = 'grab';
  });

  plansScroll.addEventListener('mouseup', () => {
    isDown = false;
    plansScroll.style.cursor = 'grab';
  });

  plansScroll.addEventListener('mousemove', (e) => {
    if (!isDown) return;
    e.preventDefault();
    const x = e.pageX - plansScroll.offsetLeft;
    const walk = (x - startX) * 2;
    plansScroll.scrollLeft = scrollLeft - walk;
  });

  // Initialize with quarterly billing
  updatePrices('quarterly');
});