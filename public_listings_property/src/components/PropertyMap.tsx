
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
import { Property } from '../types/property';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';
import { useEffect, useState } from 'react';

// Fix for default marker icon in Leaflet with React
import icon from 'leaflet/dist/images/marker-icon.png';
import iconShadow from 'leaflet/dist/images/marker-shadow.png';

let DefaultIcon = L.icon({
    iconUrl: icon,
    shadowUrl: iconShadow,
    iconSize: [25, 41],
    iconAnchor: [12, 41]
});

L.Marker.prototype.options.icon = DefaultIcon;

interface PropertyMapProps {
    properties: Property[];
    onPropertySelect: (property: Property) => void;
}

export function PropertyMap({ properties, onPropertySelect }: PropertyMapProps) {
    const [center, setCenter] = useState<[number, number]>([20.5937, 78.9629]); // Default to India center
    const [zoom, setZoom] = useState(5);

    // Filter properties with valid location
    const mapProperties = properties.filter(p => {
        return (p.location && p.location.includes(',')) || (p.landmark_location && p.landmark_location.includes(','));
    }).map(p => {
        const locString = p.location || p.landmark_location || '';
        const [lat, lng] = locString.split(',').map(Number);
        return { ...p, lat, lng };
    });

    useEffect(() => {
        if (mapProperties.length > 0) {
            // Calculate center based on properties
            const lats = mapProperties.map(p => p.lat);
            const lngs = mapProperties.map(p => p.lng);
            const minLat = Math.min(...lats);
            const maxLat = Math.max(...lats);
            const minLng = Math.min(...lngs);
            const maxLng = Math.max(...lngs);

            setCenter([(minLat + maxLat) / 2, (minLng + maxLng) / 2]);
            setZoom(10); // Adjust zoom as needed, or use bounds
        }
    }, [properties]);

    return (
        <div className="h-[calc(100vh-200px)] w-full rounded-lg overflow-hidden shadow-lg border border-gray-200">
            <MapContainer
                center={center}
                zoom={zoom}
                style={{ height: '100%', width: '100%' }}
                key={`${center[0]}-${center[1]}-${zoom}`} // Force re-render on center change if needed
            >
                <TileLayer
                    attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                    url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                />
                {mapProperties.map((property) => (
                    <Marker
                        key={property.id}
                        position={[property.lat, property.lng]}
                        eventHandlers={{
                            click: () => onPropertySelect(property),
                        }}
                    >
                        <Popup>
                            <div className="min-w-[200px]">
                                <h3 className="font-bold text-sm mb-1">{property.heading}</h3>
                                <p className="text-xs text-gray-600 mb-1">{property.city}, {property.area}</p>
                                <p className="text-sm font-semibold text-blue-600">
                                    ₹{property.price_min}L - ₹{property.price_max}L
                                </p>
                                <button
                                    onClick={() => onPropertySelect(property)}
                                    className="mt-2 text-xs text-blue-600 hover:underline"
                                >
                                    View Details
                                </button>
                            </div>
                        </Popup>
                    </Marker>
                ))}
            </MapContainer>
        </div>
    );
}
