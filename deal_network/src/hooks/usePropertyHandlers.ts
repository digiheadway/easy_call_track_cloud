import { useCallback } from 'react';
import { Property, PropertyFormData } from '../types/property';
import { propertyApi } from '../services/api';

interface UsePropertyHandlersProps {
    ownerId: number;
    onSuccess: (message: string) => void;
    onError: (message: string) => void;
    onPropertyUpdated?: () => void;
}

export function usePropertyHandlers({
    ownerId,
    onSuccess,
    onError,
    onPropertyUpdated
}: UsePropertyHandlersProps) {

    const handleAddProperty = useCallback(async (data: PropertyFormData): Promise<number | null> => {
        try {
            const response = await propertyApi.addProperty(ownerId, data);

            // Update cache with new city/area
            const { updateCacheWithCityArea } = await import('../utils/areaCityApi');
            if (data.city && data.area) {
                updateCacheWithCityArea(data.city.trim(), data.area.trim());
            }

            onSuccess('Property added successfully');
            onPropertyUpdated?.();
            return response.id;
        } catch (error) {
            onError('Failed to add property');
            return null;
        }
    }, [ownerId, onSuccess, onError, onPropertyUpdated]);

    const handleUpdateProperty = useCallback(async (propertyId: number, data: PropertyFormData): Promise<boolean> => {
        try {
            await propertyApi.updateProperty(propertyId, ownerId, data);

            // Update cache with new city/area
            const { updateCacheWithCityArea } = await import('../utils/areaCityApi');
            if (data.city && data.area) {
                updateCacheWithCityArea(data.city.trim(), data.area.trim());
            }

            onSuccess('Property updated successfully');
            onPropertyUpdated?.();
            return true;
        } catch (error) {
            onError('Failed to update property');
            return false;
        }
    }, [ownerId, onSuccess, onError, onPropertyUpdated]);

    const handleDeleteProperty = useCallback(async (propertyId: number): Promise<boolean> => {
        try {
            await propertyApi.deleteProperty(propertyId, ownerId);
            onSuccess('Property deleted successfully');
            onPropertyUpdated?.();
            return true;
        } catch (error) {
            onError('Failed to delete property');
            return false;
        }
    }, [ownerId, onSuccess, onError, onPropertyUpdated]);

    const handleTogglePublic = useCallback(async (propertyId: number, isPublic: boolean): Promise<boolean> => {
        try {
            await propertyApi.updateProperty(propertyId, ownerId, { is_public: isPublic ? 1 : 0 });
            onSuccess(`Property made ${isPublic ? 'public' : 'private'}`);
            onPropertyUpdated?.();
            return true;
        } catch (error) {
            onError('Failed to update property');
            return false;
        }
    }, [ownerId, onSuccess, onError, onPropertyUpdated]);

    const handleUpdateHighlightsAndTags = useCallback(async (
        propertyId: number,
        highlights: string,
        tags: string
    ): Promise<boolean> => {
        try {
            await propertyApi.updateProperty(propertyId, ownerId, { highlights, tags });
            onSuccess('Highlights and tags updated successfully');
            onPropertyUpdated?.();
            return true;
        } catch (error) {
            onError('Failed to update highlights and tags');
            return false;
        }
    }, [ownerId, onSuccess, onError, onPropertyUpdated]);

    const handleUpdateLocation = useCallback(async (
        propertyId: number,
        location: string,
        locationAccuracy: string
    ): Promise<boolean> => {
        try {
            await propertyApi.updateProperty(propertyId, ownerId, {
                location,
                location_accuracy: locationAccuracy
            });
            onSuccess('Location updated successfully');
            onPropertyUpdated?.();
            return true;
        } catch (error) {
            onError('Failed to update location');
            return false;
        }
    }, [ownerId, onSuccess, onError, onPropertyUpdated]);

    const handleUpdateLandmarkLocation = useCallback(async (
        propertyId: number,
        landmarkLocation: string,
        landmarkLocationDistance: string
    ): Promise<boolean> => {
        try {
            await propertyApi.updateProperty(propertyId, ownerId, {
                landmark_location: landmarkLocation,
                landmark_location_distance: landmarkLocationDistance
            });
            onSuccess('Landmark location updated successfully');
            onPropertyUpdated?.();
            return true;
        } catch (error) {
            onError('Failed to update landmark location');
            return false;
        }
    }, [ownerId, onSuccess, onError, onPropertyUpdated]);

    const handleFavProperty = useCallback(async (
        propertyId: number,
        isFavourite: boolean,
        userNote: string
    ): Promise<boolean> => {
        try {
            await propertyApi.favProperty(ownerId, propertyId, isFavourite ? 1 : 0, userNote);
            onSuccess(isFavourite ? 'Added to favorites' : 'Removed from favorites');
            onPropertyUpdated?.();
            return true;
        } catch (error) {
            onError('Failed to update favorite');
            return false;
        }
    }, [ownerId, onSuccess, onError, onPropertyUpdated]);

    return {
        handleAddProperty,
        handleUpdateProperty,
        handleDeleteProperty,
        handleTogglePublic,
        handleUpdateHighlightsAndTags,
        handleUpdateLocation,
        handleUpdateLandmarkLocation,
        handleFavProperty,
    };
}
