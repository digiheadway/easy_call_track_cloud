import { useState, useEffect } from 'react';
import { Heart, Loader2, Map as MapIcon, List } from 'lucide-react';
import { PropertyMap } from './components/PropertyMap';
import { Property, UserProfile, UserInterest } from './types/property';
import { fetchProperties } from './utils/api';
import { saveUserInterest } from './utils/localStorage';
import { SearchAndFilters, PropertyFilters } from './components/SearchAndFilters';
import { PropertyCard } from './components/PropertyCard';
import { PropertyDetailModal } from './components/PropertyDetailModal';
import { InterestCaptureForm } from './components/InterestCaptureForm';
import { InterestsPage } from './components/InterestsPage';
import { Pagination } from './components/Pagination';

function App() {
  const [properties, setProperties] = useState<Property[]>([]);
  const [filteredProperties, setFilteredProperties] = useState<Property[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedProperty, setSelectedProperty] = useState<Property | null>(null);
  const [showDetailModal, setShowDetailModal] = useState(false);
  const [showInterestForm, setShowInterestForm] = useState(false);
  const [showInterestsPage, setShowInterestsPage] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const [searchQuery, setSearchQuery] = useState('');
  const [filters, setFilters] = useState<PropertyFilters>({
    type: '',
    city: '',
    minPrice: '',
    maxPrice: '',
    minSize: '',
    maxSize: '',
    sizeUnit: '',
  });
  const [viewMode, setViewMode] = useState<'list' | 'map'>('list');

  const itemsPerPage = 12;

  useEffect(() => {
    loadProperties();
  }, []);

  useEffect(() => {
    applyFilters();
  }, [properties, searchQuery, filters]);

  const loadProperties = async () => {
    try {
      setLoading(true);
      const response = await fetchProperties(1, 100);
      if (response.success) {
        setProperties(response.data);
      } else {
        setError('Failed to load properties');
      }
    } catch (err) {
      setError('Failed to load properties. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const applyFilters = () => {
    let filtered = [...properties];

    if (searchQuery.trim()) {
      const query = searchQuery.toLowerCase();
      filtered = filtered.filter(
        (p) =>
          p.heading.toLowerCase().includes(query) ||
          p.description?.toLowerCase().includes(query) ||
          p.city.toLowerCase().includes(query) ||
          p.area.toLowerCase().includes(query) ||
          p.type.toLowerCase().includes(query)
      );
    }

    if (filters.type) {
      filtered = filtered.filter((p) => p.type === filters.type);
    }

    if (filters.city) {
      filtered = filtered.filter((p) => p.city === filters.city);
    }

    if (filters.minPrice) {
      const minPrice = parseFloat(filters.minPrice);
      filtered = filtered.filter((p) => p.price_min >= minPrice);
    }

    if (filters.maxPrice) {
      const maxPrice = parseFloat(filters.maxPrice);
      filtered = filtered.filter((p) => p.price_max <= maxPrice);
    }

    if (filters.minSize) {
      const minSize = parseFloat(filters.minSize);
      filtered = filtered.filter((p) => p.size_max >= minSize);
    }

    if (filters.maxSize) {
      const maxSize = parseFloat(filters.maxSize);
      filtered = filtered.filter((p) => p.size_min <= maxSize);
    }

    if (filters.sizeUnit) {
      filtered = filtered.filter((p) => p.size_unit === filters.sizeUnit);
    }

    setFilteredProperties(filtered);
    setCurrentPage(1);
  };

  const handlePropertyClick = (property: Property) => {
    setSelectedProperty(property);
    setShowDetailModal(true);
  };

  const handleGetMoreDetails = () => {
    setShowDetailModal(false);
    setShowInterestForm(true);
  };

  const handleInterestSubmit = (_profile: UserProfile, _propertyQuestion: string) => {
    if (selectedProperty) {
      const interest: UserInterest = {
        propertyId: selectedProperty.id,
        timestamp: new Date().toISOString(),
        propertyDetails: {
          heading: selectedProperty.heading,
          city: selectedProperty.city,
          area: selectedProperty.area,
          type: selectedProperty.type,
          price: `₹${selectedProperty.price_min}${selectedProperty.price_max !== selectedProperty.price_min
            ? `-${selectedProperty.price_max}`
            : ''
            }L`,
        },
      };

      saveUserInterest(interest);
    }
  };

  const getUniqueValues = (key: keyof Property): string[] => {
    const values = properties.map((p) => p[key] as string);
    return Array.from(new Set(values)).sort();
  };

  const totalPages = Math.ceil(filteredProperties.length / itemsPerPage);
  const startIndex = (currentPage - 1) * itemsPerPage;
  const endIndex = startIndex + itemsPerPage;
  const currentProperties = filteredProperties.slice(startIndex, endIndex);

  if (showInterestsPage) {
    return <InterestsPage onClose={() => setShowInterestsPage(false)} />;
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white shadow-sm border-b border-gray-200 sticky top-0 z-30">
        <div className="max-w-7xl mx-auto px-4 py-4 flex items-center justify-between">
          <div>
            <h1 className="text-xl font-bold text-gray-900">Property Listings</h1>
            <p className="text-xs text-gray-500 mt-0.5">
              {filteredProperties.length} properties available
            </p>
          </div>
          <button
            onClick={() => setShowInterestsPage(true)}
            className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors text-sm font-medium"
          >
            <Heart className="w-4 h-4" />
            <span className="hidden sm:inline">My Interests</span>
          </button>
        </div>
      </header>

      <SearchAndFilters
        onSearch={setSearchQuery}
        onFilterChange={setFilters}
        propertyTypes={getUniqueValues('type')}
        cities={getUniqueValues('city')}
        sizeUnits={getUniqueValues('size_unit')}
      />

      <main className="max-w-7xl mx-auto px-4 py-6">
        <div className="flex justify-end mb-4">
          <div className="bg-white p-1 rounded-lg border border-gray-200 shadow-sm flex gap-1">
            <button
              onClick={() => setViewMode('list')}
              className={`p-2 rounded-md transition-colors ${viewMode === 'list'
                ? 'bg-blue-50 text-blue-600'
                : 'text-gray-500 hover:bg-gray-50'
                }`}
              title="List View"
            >
              <List className="w-5 h-5" />
            </button>
            <button
              onClick={() => setViewMode('map')}
              className={`p-2 rounded-md transition-colors ${viewMode === 'map'
                ? 'bg-blue-50 text-blue-600'
                : 'text-gray-500 hover:bg-gray-50'
                }`}
              title="Map View"
            >
              <MapIcon className="w-5 h-5" />
            </button>
          </div>
        </div>

        {loading ? (
          <div className="flex items-center justify-center py-20">
            <Loader2 className="w-8 h-8 text-blue-600 animate-spin" />
          </div>
        ) : error ? (
          <div className="text-center py-20">
            <p className="text-red-600 mb-4">{error}</p>
            <button
              onClick={loadProperties}
              className="px-6 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors text-sm font-medium"
            >
              Try Again
            </button>
          </div>
        ) : currentProperties.length === 0 ? (
          <div className="text-center py-20">
            <p className="text-gray-600 mb-4">No properties found</p>
            <button
              onClick={() => {
                setSearchQuery('');
                setFilters({
                  type: '',
                  city: '',
                  minPrice: '',
                  maxPrice: '',
                  minSize: '',
                  maxSize: '',
                  sizeUnit: '',
                });
              }}
              className="px-6 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors text-sm font-medium"
            >
              Clear Filters
            </button>
          </div>
        ) : viewMode === 'map' ? (
          <PropertyMap
            properties={filteredProperties}
            onPropertySelect={handlePropertyClick}
          />
        ) : (
          <>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
              {currentProperties.map((property) => (
                <PropertyCard
                  key={property.id}
                  property={property}
                  onClick={() => handlePropertyClick(property)}
                />
              ))}
            </div>

            {totalPages > 1 && (
              <Pagination
                currentPage={currentPage}
                totalPages={totalPages}
                onPageChange={setCurrentPage}
              />
            )}
          </>
        )}
      </main>

      {selectedProperty && (
        <>
          <PropertyDetailModal
            property={selectedProperty}
            isOpen={showDetailModal}
            onClose={() => setShowDetailModal(false)}
            onGetMoreDetails={handleGetMoreDetails}
          />

          <InterestCaptureForm
            property={selectedProperty}
            isOpen={showInterestForm}
            onClose={() => setShowInterestForm(false)}
            onSubmit={handleInterestSubmit}
          />
        </>
      )}
    </div>
  );
}

export default App;
