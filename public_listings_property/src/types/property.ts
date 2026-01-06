export interface Property {
  id: number;
  owner_id: number;
  city: string;
  area: string;
  type: string;
  description: string;
  note_private: string | null;
  size_min: number;
  size_max: number;
  size_unit: string;
  price_min: number;
  price_max: number;
  location: string | null;
  location_accuracy: number | null;
  landmark_location: string | null;
  landmark_location_distance: number | null;
  note_for_friends: string | null;
  is_public: boolean;
  public_rating: number;
  my_rating: number;
  created_on: string;
  updated_on: string;
  tags: string | null;
  highlights: string;
  owner_name: string;
  owner_phone: string;
  owner_firm_name: string | null;
  heading: string;
  is_owner: boolean;
}

export interface PropertyResponse {
  success: boolean;
  message: string;
  timestamp: string;
  data: Property[];
}

export interface UserInterest {
  propertyId: number;
  timestamp: string;
  propertyDetails: {
    heading: string;
    city: string;
    area: string;
    type: string;
    price: string;
  };
}

export interface UserProfile {
  budget: string;
  address: string;
  lookingFor: 'myself' | 'clients' | 'friends' | '';
  isDealer: boolean;
  propertyQuestions?: Record<number, string>;
}
