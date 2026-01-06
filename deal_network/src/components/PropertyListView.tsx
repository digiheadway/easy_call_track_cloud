import { Property } from '../types/property';
import { PropertyCard } from './PropertyCard';
import { PropertyCardSkeleton } from './PropertyCardSkeleton';

interface PropertyListViewProps {
    properties: Property[];
    loading: boolean;
    ownerId: number;
    onViewDetails: (property: Property) => void;
    selectedProperty: Property | null;
}

export function PropertyListView({
    properties,
    loading,
    ownerId,
    onViewDetails,
    selectedProperty
}: PropertyListViewProps) {
    if (loading && properties.length === 0) {
        return (
            <div className="space-y-3 p-4">
                {[...Array(6)].map((_, i) => (
                    <PropertyCardSkeleton key={i} />
                ))}
            </div>
        );
    }

    if (properties.length === 0) {
        return (
            <div className="flex flex-col items-center justify-center py-16 px-4">
                <div className="text-center">
                    <p className="text-gray-500 text-lg mb-2">No properties found</p>
                    <p className="text-gray-400 text-sm">Try adjusting your filters or search criteria</p>
                </div>
            </div>
        );
    }

    return (
        <div className="space-y-3 p-4">
            {properties.map((property) => (
                <PropertyCard
                    key={property.id}
                    property={property}
                    isOwned={property.owner_id === ownerId}
                    onViewDetails={onViewDetails}
                    isSelected={selectedProperty?.id === property.id}
                />
            ))}
        </div>
    );
}
