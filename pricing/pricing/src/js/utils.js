import { CURRENCIES } from './constants.js';

export function formatPrice(amount, country = 'IN') {
  const currency = CURRENCIES[country];
  return new Intl.NumberFormat(currency.locale, {
    style: 'currency',
    currency: currency.code,
    maximumFractionDigits: currency.maximumFractionDigits
  })
    .format(amount)
    .replace(currency.code, currency.symbol);
}

export function calculateMonthlyPrice(orders, pricePerOrder) {
  return orders * pricePerOrder;
}

export function calculateDiscountedPrice(originalPrice, discountPercentage) {
  return originalPrice * (1 - discountPercentage / 100);
}

export function getFeatureInfo(key) {
  return {
    title: key,
    description: 'Feature information'
  };
}