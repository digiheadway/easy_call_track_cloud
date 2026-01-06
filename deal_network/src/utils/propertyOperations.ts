import { Property, PropertyFormData } from '../types/property';
import { propertyApi } from '../services/api';
import { formatPriceWithLabel } from './priceFormatter';
import { formatSize } from './sizeFormatter';

/**
 * Handle property sharing functionality
 */
export async function shareProperty(
    property: Property,
    onSuccess: () => void,
    onError: (message: string) => void
): Promise<void> {
    const sizeText = formatSize(property.size_min, property.size_max, property.size_unit);
    const priceText = formatPriceWithLabel(property.price_min, property.price_max);
    const shareUrl = property.is_public === 1
        ? `${window.location.origin}/property/${property.id}`
        : undefined;

    const textForShare = `${property.type} in ${property.area}, ${property.city}\n${property.description}\nSize: ${sizeText}\nPrice: ${priceText}`;
    const textForClipboard = `${textForShare}${shareUrl ? `\n\nView: ${shareUrl}` : ''}`;

    if (navigator.share) {
        try {
            await navigator.share({
                title: `${property.type} - ${property.area}`,
                text: textForShare,
                url: shareUrl,
            });
            onSuccess();
        } catch (error) {
            if ((error as Error).name !== 'AbortError') {
                onError('Failed to share');
            }
        }
    } else {
        try {
            await navigator.clipboard.writeText(textForClipboard);
            onSuccess();
        } catch (error) {
            onError('Failed to copy to clipboard');
        }
    }
}

/**
 * Add a property
 */
export async function addProperty(
    ownerId: number,
    data: PropertyFormData,
    onSuccess: (propertyId: number) => void,
    onError: (message: string) => void
): Promise<void> {
    try {
        const response = await propertyApi.addProperty(ownerId, data);

        // Update cache with new city/area only on successful add
        const { updateCacheWithCityArea } = await import('./areaCityApi');
        if (data.city && data.area) {
            updateCacheWithCityArea(data.city.trim(), data.area.trim());
        }

        onSuccess(response.id);
    } catch (error) {
        onError('Failed to add property');
    }
}

/**
 * Update a property
 */
export async function updateProperty(
    propertyId: number,
    ownerId: number,
    data: PropertyFormData,
    onSuccess: () => void,
    onError: (message: string) => void
): Promise<void> {
    try {
        await propertyApi.updateProperty(propertyId, ownerId, data);

        // Update cache with new city/area only on successful update
        const { updateCacheWithCityArea } = await import('./areaCityApi');
        if (data.city && data.area) {
            updateCacheWithCityArea(data.city.trim(), data.area.trim());
        }

        onSuccess();
    } catch (error) {
        onError('Failed to update property');
    }
}

/**
 * Delete a property
 */
export async function deleteProperty(
    propertyId: number,
    ownerId: number,
    onSuccess: () => void,
    onError: (message: string) => void
): Promise<void> {
    try {
        await propertyApi.deleteProperty(propertyId, ownerId);
        onSuccess();
    } catch (error) {
        onError('Failed to delete property');
    }
}

/**
 * Toggle property public/private status
 */
export async function togglePropertyPublic(
    propertyId: number,
    ownerId: number,
    isPublic: boolean,
    onSuccess: () => void,
    onError: (message: string) => void
): Promise<void> {
    try {
        await propertyApi.updateProperty(propertyId, ownerId, { is_public: isPublic ? 1 : 0 });
        onSuccess();
    } catch (error) {
        onError('Failed to update property');
    }
}

/**
 * Update property highlights and tags
 */
export async function updatePropertyHighlightsAndTags(
    propertyId: number,
    ownerId: number,
    highlights: string,
    tags: string,
    onSuccess: () => void,
    onError: (message: string) => void
): Promise<void> {
    try {
        await propertyApi.updateProperty(propertyId, ownerId, { highlights, tags });
        onSuccess();
    } catch (error) {
        onError('Failed to update highlights and tags');
    }
}

/**
 * Update property location
 */
export async function updatePropertyLocation(
    propertyId: number,
    ownerId: number,
    location: string,
    locationAccuracy: string,
    onSuccess: () => void,
    onError: (message: string) => void
): Promise<void> {
    try {
        await propertyApi.updateProperty(propertyId, ownerId, { location, location_accuracy: locationAccuracy });
        onSuccess();
    } catch (error) {
        onError('Failed to update location');
    }
}

/**
 * Update property landmark location
 */
export async function updatePropertyLandmarkLocation(
    propertyId: number,
    ownerId: number,
    landmarkLocation: string,
    landmarkLocationDistance: string,
    onSuccess: () => void,
    onError: (message: string) => void
): Promise<void> {
    try {
        await propertyApi.updateProperty(propertyId, ownerId, {
            landmark_location: landmarkLocation,
            landmark_location_distance: landmarkLocationDistance
        });
        onSuccess();
    } catch (error) {
        onError('Failed to update landmark location');
    }
}

/**
 * Favorite/unfavorite a property
 */
export async function favoriteProperty(
    ownerId: number,
    propertyId: number,
    isFavourite: boolean,
    userNote: string,
    onSuccess: () => void,
    onError: (message: string) => void
): Promise<void> {
    try {
        await propertyApi.favProperty(ownerId, propertyId, isFavourite ? 1 : 0, userNote);
        onSuccess();
    } catch (error) {
        onError('Failed to update favorite');
    }
}
