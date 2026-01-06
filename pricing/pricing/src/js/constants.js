export const PRICES = {
  IN: {
    monthly: { base: 10, discount: 0 },
    quarterly: { base: 22.5, discount: 0 },
    yearly: { base: 90, discount: 0 }
  },
  OUT: {
    monthly: { base: 0.15, discount: 0 },
    quarterly: { base: 0.45, discount: 0 },
    yearly: { base: 1.8, discount: 0 }
  }
};

export const CURRENCIES = {
  IN: {
    code: 'INR',
    symbol: '₹',
    locale: 'en-IN',
    maximumFractionDigits: 0
  },
  OUT: {
    code: 'USD',
    symbol: '$',
    locale: 'en-US',
    maximumFractionDigits: 2
  }
};

export const GST_PERCENTAGE = 18;

// export const ORDER_LIMITS = {
//   free: 25,
//   starter: 50,
//   pro: 100,
//   enterprise: 150,
//   plan1: 50,
//   plan2: 75,
//   plan3: 100,
//   plan4: 125,
//   plan5: 150,
//   plan6: 200,
//   plan7: 250,
//   plan8: 300,
//   plan9: 350,
//   plan10: 400,
//   plan11: 500

// };

export const BILLING_PERIODS = {
  monthly: 'Monthly',
  quarterly: 'Quarterly',
  yearly: 'Yearly'
};