import { UserProfile, UserInterest } from '../types/property';

const USER_PROFILE_KEY = 'user_profile';
const USER_INTERESTS_KEY = 'user_interests';

export const saveUserProfile = (profile: UserProfile): void => {
  localStorage.setItem(USER_PROFILE_KEY, JSON.stringify(profile));
};

export const getUserProfile = (): UserProfile | null => {
  const data = localStorage.getItem(USER_PROFILE_KEY);
  return data ? JSON.parse(data) : null;
};

export const saveUserInterest = (interest: UserInterest): void => {
  const interests = getUserInterests();
  const exists = interests.find(i => i.propertyId === interest.propertyId);

  if (!exists) {
    interests.push(interest);
    localStorage.setItem(USER_INTERESTS_KEY, JSON.stringify(interests));
  }
};

export const getUserInterests = (): UserInterest[] => {
  const data = localStorage.getItem(USER_INTERESTS_KEY);
  return data ? JSON.parse(data) : [];
};

export const removeUserInterest = (propertyId: number): void => {
  const interests = getUserInterests();
  const filtered = interests.filter(i => i.propertyId !== propertyId);
  localStorage.setItem(USER_INTERESTS_KEY, JSON.stringify(filtered));
};

export const clearAllInterests = (): void => {
  localStorage.removeItem(USER_INTERESTS_KEY);
};
