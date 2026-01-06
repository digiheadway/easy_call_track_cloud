import { Home, MapPin, Maximize2, IndianRupee } from 'lucide-react';
import { Property } from '../types/property';
import { formatPrice, formatSize } from '../utils/formatters';

interface PropertyCardProps {
  property: Property;
  onClick: () => void;
}

export const PropertyCard = ({ property, onClick }: PropertyCardProps) => {

  return (
    <div
      onClick={onClick}
      className="bg-white rounded-lg shadow-sm hover:shadow-md transition-all duration-200 p-4 cursor-pointer border border-gray-100 hover:border-gray-300"
    >
      <div className="flex items-start justify-between mb-3">
        <div className="flex items-center gap-2 text-gray-700">
          <Home className="w-5 h-5 flex-shrink-0" />
          <span className="font-semibold text-sm">{property.type}</span>
        </div>
        <div className="flex items-center gap-1 text-green-600 font-bold text-base">
          <IndianRupee className="w-4 h-4" />
          <span>{formatPrice(property.price_min, property.price_max)}</span>
        </div>
      </div>

      <h3 className="font-medium text-gray-900 mb-2 line-clamp-2 text-sm leading-snug">
        {property.heading}
      </h3>

      <div className="flex items-center gap-1.5 text-gray-600 mb-2 text-xs">
        <MapPin className="w-4 h-4 flex-shrink-0" />
        <span className="truncate">
          {property.area}, {property.city}
        </span>
      </div>

      <div className="flex items-center gap-1.5 text-gray-600 mb-3 text-xs">
        <Maximize2 className="w-4 h-4 flex-shrink-0" />
        <span>{formatSize(property.size_min, property.size_max, property.size_unit)}</span>
      </div>

      {property.description && (
        <p className="text-gray-600 text-xs line-clamp-2 leading-relaxed">
          {property.description}
        </p>
      )}
    </div>
  );
};
