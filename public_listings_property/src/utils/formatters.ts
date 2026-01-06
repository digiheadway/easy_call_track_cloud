export const formatPrice = (min: number, max?: number) => {
  const convertPrice = (price: number) => {
    if (price >= 100) {
      const crores = (price / 100).toFixed(price % 100 === 0 ? 0 : 1);
      return `₹${crores} Cr`;
    }
    return `₹${price}L`;
  };

  if (min === max || !max) {
    return convertPrice(min);
  }

  return `${convertPrice(min)} - ${convertPrice(max)}`;
};

export const formatSize = (min: number, max?: number, unit?: string) => {
  if (min === max || !max) {
    return `${min} ${unit}`;
  }
  return `${min}-${max} ${unit}`;
};

export const formatDate = (dateString: string) => {
  const date = new Date(dateString);
  return date.toLocaleDateString('en-IN', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  });
};
