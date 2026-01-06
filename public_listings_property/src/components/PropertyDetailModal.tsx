import { X, Home, MapPin, Maximize2, IndianRupee, Calendar } from 'lucide-react';
import { Property } from '../types/property';
import { formatPrice, formatSize, formatDate } from '../utils/formatters';

interface PropertyDetailModalProps {
  property: Property;
  isOpen: boolean;
  onClose: () => void;
  onGetMoreDetails: () => void;
}

export const PropertyDetailModal = ({
  property,
  isOpen,
  onClose,
  onGetMoreDetails,
}: PropertyDetailModalProps) => {
  if (!isOpen) return null;

  return (
    <>
      <div
        className="fixed inset-0 bg-black bg-opacity-50 z-40 transition-opacity duration-300"
        onClick={onClose}
      />

      <div className="fixed inset-x-0 bottom-0 z-50 animate-slideUp">
        <div className="bg-white rounded-t-3xl shadow-2xl max-h-[80vh] overflow-y-auto">
          <div className="sticky top-0 bg-white border-b border-gray-200 px-4 py-3 flex items-center justify-between rounded-t-3xl z-10">
            <h2 className="font-semibold text-gray-900 text-lg">Property Details</h2>
            <button
              onClick={onClose}
              className="p-2 hover:bg-gray-100 rounded-full transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          <div className="p-5 space-y-5">
            <div className="flex items-start justify-between gap-4">
              <div className="flex-1">
                <div className="flex items-center gap-2 text-blue-600 mb-2">
                  <Home className="w-5 h-5 flex-shrink-0" />
                  <span className="font-medium text-sm">{property.type}</span>
                </div>
                <h3 className="font-semibold text-gray-900 text-base leading-snug mb-3">
                  {property.heading}
                </h3>
              </div>
              <div className="text-right">
                <div className="flex items-center gap-1 text-green-600 font-bold text-xl">
                  <IndianRupee className="w-5 h-5" />
                  <span>{formatPrice(property.price_min, property.price_max)}</span>
                </div>
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="flex items-start gap-3 p-3 bg-gray-50 rounded-lg">
                <MapPin className="w-5 h-5 text-gray-600 mt-0.5 flex-shrink-0" />
                <div>
                  <div className="text-xs text-gray-500 mb-0.5">Location</div>
                  <div className="text-sm font-medium text-gray-900">
                    {property.area}, {property.city}
                  </div>
                </div>
              </div>

              <div className="flex items-start gap-3 p-3 bg-gray-50 rounded-lg">
                <Maximize2 className="w-5 h-5 text-gray-600 mt-0.5 flex-shrink-0" />
                <div>
                  <div className="text-xs text-gray-500 mb-0.5">Size</div>
                  <div className="text-sm font-medium text-gray-900">
                    {formatSize(property.size_min, property.size_max, property.size_unit)}
                  </div>
                </div>
              </div>
            </div>

            {property.description && (
              <div>
                <h4 className="font-medium text-gray-900 mb-2 text-sm">Description</h4>
                <p className="text-gray-600 text-sm leading-relaxed whitespace-pre-line">
                  {property.description}
                </p>
              </div>
            )}

            {(property.landmark_location || property.landmark_location_distance) && (
              <div className="p-3 bg-blue-50 rounded-lg">
                <h4 className="font-medium text-gray-900 mb-1 text-sm">Landmark</h4>
                <p className="text-gray-600 text-sm">
                  {property.landmark_location_distance
                    ? `${property.landmark_location_distance}m from landmark`
                    : 'Near landmark'}
                </p>
              </div>
            )}

            <div className="flex items-center gap-2 text-xs text-gray-500 pt-2">
              <Calendar className="w-4 h-4" />
              <span>Listed on {formatDate(property.created_on)}</span>
            </div>

            <button
              onClick={onGetMoreDetails}
              className="w-full bg-blue-600 hover:bg-blue-700 text-white font-medium py-3.5 px-4 rounded-lg transition-colors shadow-sm text-base"
            >
              Get More Details
            </button>
          </div>
        </div>
      </div>
    </>
  );
};
