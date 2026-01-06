import { Property } from '../types/property';

/**
 * Trim description to specified length
 */
export function trimDescription(description: string | undefined, maxLength: number = 150): string {
    if (!description) return '';
    if (description.length <= maxLength) return description;
    return description.substring(0, maxLength) + '...';
}

/**
 * Calculate rate per unit for a property
 */
export function calculateRatePerUnit(property: Property): string | null {
    const avgPrice = property.price_min > 0 && property.price_max > 0
        ? (property.price_min + property.price_max) / 2
        : property.price_min > 0
            ? property.price_min
            : property.price_max > 0
                ? property.price_max
                : 0;

    const avgSize = property.size_min > 0 && property.size_max > 0
        ? (property.size_min + property.size_max) / 2
        : property.size_min > 0
            ? property.size_min
            : property.size_max > 0
                ? property.size_max
                : 0;

    if (avgPrice > 0 && avgSize > 0) {
        const priceInRupees = avgPrice * 100000;
        const ratePerUnit = priceInRupees / avgSize;
        return `₹ ${Math.round(ratePerUnit).toLocaleString('en-IN')}`;
    }

    return null;
}

/**
 * Format location based on user's city
 */
export function formatLocation(property: Property, userCity: string): string {
    return property.city.toLowerCase() === userCity.toLowerCase()
        ? property.area
        : `${property.area}, ${property.city}`;
}

/**
 * Format created date to relative time
 */
export function formatCreatedDate(dateString: string): string {
    const date = new Date(dateString);
    const now = new Date();
    const diffTime = Math.abs(now.getTime() - date.getTime());
    const diffMinutes = Math.floor(diffTime / (1000 * 60));
    const diffHours = Math.floor(diffTime / (1000 * 60 * 60));
    const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));

    if (diffMinutes < 1) {
        return 'Just now';
    } else if (diffMinutes < 60) {
        return `${diffMinutes} ${diffMinutes === 1 ? 'min' : 'mins'} ago`;
    } else if (diffHours < 24) {
        return `${diffHours} ${diffHours === 1 ? 'hour' : 'hours'} ago`;
    } else if (diffDays === 1) {
        return 'Yesterday';
    } else if (diffDays < 7) {
        return `${diffDays} Days ago`;
    } else if (diffDays < 30) {
        const weeks = Math.floor(diffDays / 7);
        return `${weeks} ${weeks === 1 ? 'Week' : 'Weeks'} ago`;
    } else {
        return date.toLocaleDateString('en-US', {
            month: 'short',
            day: 'numeric',
            year: date.getFullYear() !== now.getFullYear() ? 'numeric' : undefined
        });
    }
}
