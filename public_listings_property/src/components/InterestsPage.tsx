import { useState, useEffect } from 'react';
import { Heart, Trash2, ArrowLeft, MapPin, Home, IndianRupee } from 'lucide-react';
import { UserInterest } from '../types/property';
import { getUserInterests, removeUserInterest } from '../utils/localStorage';

interface InterestsPageProps {
  onClose: () => void;
}

export const InterestsPage = ({ onClose }: InterestsPageProps) => {
  const [interests, setInterests] = useState<UserInterest[]>([]);

  useEffect(() => {
    loadInterests();
  }, []);

  const loadInterests = () => {
    const saved = getUserInterests();
    setInterests(saved.sort((a, b) =>
      new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
    ));
  };

  const handleRemove = (propertyId: number) => {
    if (confirm('Remove this property from your interests?')) {
      removeUserInterest(propertyId);
      loadInterests();
    }
  };

  const formatDate = (timestamp: string) => {
    const date = new Date(timestamp);
    return date.toLocaleDateString('en-IN', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
    });
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="bg-white shadow-sm sticky top-0 z-30">
        <div className="p-4 flex items-center gap-3">
          <button
            onClick={onClose}
            className="p-2 hover:bg-gray-100 rounded-full transition-colors"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div className="flex-1">
            <h1 className="text-lg font-semibold text-gray-900">My Interests</h1>
            <p className="text-xs text-gray-500">{interests.length} saved properties</p>
          </div>
        </div>
      </div>

      <div className="p-4">
        {interests.length === 0 ? (
          <div className="text-center py-16 px-4">
            <div className="w-20 h-20 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <Heart className="w-10 h-10 text-gray-400" />
            </div>
            <h3 className="text-lg font-semibold text-gray-900 mb-2">
              No Interests Yet
            </h3>
            <p className="text-gray-600 text-sm max-w-sm mx-auto leading-relaxed">
              When you express interest in properties, they'll appear here for easy access.
            </p>
            <button
              onClick={onClose}
              className="mt-6 bg-blue-600 hover:bg-blue-700 text-white font-medium py-2.5 px-6 rounded-lg transition-colors text-sm"
            >
              Browse Properties
            </button>
          </div>
        ) : (
          <div className="space-y-3">
            {interests.map((interest) => (
              <div
                key={interest.propertyId}
                className="bg-white rounded-lg shadow-sm border border-gray-100 p-4"
              >
                <div className="flex items-start justify-between gap-3 mb-3">
                  <div className="flex-1">
                    <div className="flex items-center gap-2 text-blue-600 mb-1.5">
                      <Home className="w-4 h-4 flex-shrink-0" />
                      <span className="text-xs font-medium">{interest.propertyDetails.type}</span>
                    </div>
                    <h3 className="font-medium text-gray-900 text-sm leading-snug mb-2">
                      {interest.propertyDetails.heading}
                    </h3>
                  </div>
                  <button
                    onClick={() => handleRemove(interest.propertyId)}
                    className="p-2 hover:bg-red-50 rounded-lg transition-colors text-red-600"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>

                <div className="space-y-2 mb-3">
                  <div className="flex items-center gap-2 text-gray-600 text-xs">
                    <MapPin className="w-3.5 h-3.5 flex-shrink-0" />
                    <span className="truncate">
                      {interest.propertyDetails.area}, {interest.propertyDetails.city}
                    </span>
                  </div>

                  <div className="flex items-center gap-2 text-green-600 font-semibold text-sm">
                    <IndianRupee className="w-3.5 h-3.5" />
                    <span>{interest.propertyDetails.price}</span>
                  </div>
                </div>

                <div className="pt-3 border-t border-gray-100">
                  <p className="text-xs text-gray-500">
                    Saved on {formatDate(interest.timestamp)}
                  </p>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
