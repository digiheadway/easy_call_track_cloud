import { useState, useEffect } from 'react';
import { X, CheckCircle } from 'lucide-react';
import { Property, UserProfile } from '../types/property';
import { getUserProfile, saveUserProfile } from '../utils/localStorage';

interface InterestCaptureFormProps {
  property: Property;
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (profile: UserProfile, propertyQuestion: string) => void;
}

export const InterestCaptureForm = ({
  property,
  isOpen,
  onClose,
  onSubmit,
}: InterestCaptureFormProps) => {
  const [step, setStep] = useState<'intro' | 'profile' | 'question' | 'success'>('intro');
  const [profile, setProfile] = useState<UserProfile>({
    budget: '',
    address: '',
    lookingFor: '',
    isDealer: false,
  });
  const [propertyQuestion, setPropertyQuestion] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (isOpen) {
      const savedProfile = getUserProfile();
      if (savedProfile) {
        setProfile(savedProfile);
      }
      setStep('intro');
      setPropertyQuestion('');
      setErrors({});
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const validateProfile = () => {
    const newErrors: Record<string, string> = {};

    if (!profile.budget.trim()) {
      newErrors.budget = 'Budget is required';
    }
    if (!profile.address.trim()) {
      newErrors.address = 'Address is required';
    }
    if (!profile.lookingFor) {
      newErrors.lookingFor = 'Please select who you are looking for';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleProfileSubmit = () => {
    if (validateProfile()) {
      saveUserProfile(profile);
      setStep('question');
    }
  };

  const handleFinalSubmit = () => {
    onSubmit(profile, propertyQuestion);
    setStep('success');
  };

  const handleClose = () => {
    if (step === 'success') {
      onClose();
    } else {
      if (confirm('Are you sure you want to close? Your progress will be lost.')) {
        onClose();
      }
    }
  };

  return (
    <>
      <div
        className="fixed inset-0 bg-black bg-opacity-50 z-40 transition-opacity duration-300"
        onClick={handleClose}
      />

      <div className="fixed inset-x-0 bottom-0 z-50 animate-slideUp">
        <div className="bg-white rounded-t-3xl shadow-2xl max-h-[85vh] overflow-y-auto">
          <div className="sticky top-0 bg-white border-b border-gray-200 px-4 py-3 flex items-center justify-between rounded-t-3xl z-10">
            <h2 className="font-semibold text-gray-900 text-lg">
              {step === 'intro' && 'Connect with Owner'}
              {step === 'profile' && 'Your Details'}
              {step === 'question' && 'Property Query'}
              {step === 'success' && 'Interest Saved'}
            </h2>
            <button
              onClick={handleClose}
              className="p-2 hover:bg-gray-100 rounded-full transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          <div className="p-5">
            {step === 'intro' && (
              <div className="text-center py-6 space-y-4">
                <div className="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center mx-auto">
                  <CheckCircle className="w-8 h-8 text-blue-600" />
                </div>
                <h3 className="text-xl font-semibold text-gray-900">
                  Before Connecting
                </h3>
                <p className="text-gray-600 text-sm max-w-md mx-auto leading-relaxed">
                  Please provide some details so the owner can better understand your requirements and serve you better.
                </p>
                <button
                  onClick={() => setStep('profile')}
                  className="w-full max-w-xs mx-auto block bg-blue-600 hover:bg-blue-700 text-white font-medium py-3 px-4 rounded-lg transition-colors"
                >
                  Continue
                </button>
              </div>
            )}

            {step === 'profile' && (
              <div className="space-y-4 pb-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1.5">
                    Budget <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    placeholder="e.g., 50-60 Lakhs"
                    value={profile.budget}
                    onChange={(e) => setProfile({ ...profile, budget: e.target.value })}
                    className={`w-full px-4 py-2.5 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm ${
                      errors.budget ? 'border-red-500' : 'border-gray-300'
                    }`}
                  />
                  {errors.budget && (
                    <p className="text-red-500 text-xs mt-1">{errors.budget}</p>
                  )}
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1.5">
                    Address <span className="text-red-500">*</span>
                  </label>
                  <textarea
                    placeholder="Your current address"
                    value={profile.address}
                    onChange={(e) => setProfile({ ...profile, address: e.target.value })}
                    rows={3}
                    className={`w-full px-4 py-2.5 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm resize-none ${
                      errors.address ? 'border-red-500' : 'border-gray-300'
                    }`}
                  />
                  {errors.address && (
                    <p className="text-red-500 text-xs mt-1">{errors.address}</p>
                  )}
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Looking for <span className="text-red-500">*</span>
                  </label>
                  <div className="space-y-2">
                    {[
                      { value: 'myself', label: 'Myself' },
                      { value: 'clients', label: 'My Clients' },
                      { value: 'friends', label: 'My Friends/Relatives' },
                    ].map((option) => (
                      <label
                        key={option.value}
                        className="flex items-center p-3 border border-gray-300 rounded-lg cursor-pointer hover:bg-gray-50 transition-colors"
                      >
                        <input
                          type="radio"
                          name="lookingFor"
                          value={option.value}
                          checked={profile.lookingFor === option.value}
                          onChange={(e) =>
                            setProfile({
                              ...profile,
                              lookingFor: e.target.value as UserProfile['lookingFor'],
                            })
                          }
                          className="w-4 h-4 text-blue-600 focus:ring-blue-500"
                        />
                        <span className="ml-3 text-sm text-gray-900">{option.label}</span>
                      </label>
                    ))}
                  </div>
                  {errors.lookingFor && (
                    <p className="text-red-500 text-xs mt-1">{errors.lookingFor}</p>
                  )}
                </div>

                <label className="flex items-start p-3 border border-gray-300 rounded-lg cursor-pointer hover:bg-gray-50 transition-colors">
                  <input
                    type="checkbox"
                    checked={profile.isDealer}
                    onChange={(e) =>
                      setProfile({ ...profile, isDealer: e.target.checked })
                    }
                    className="w-4 h-4 mt-0.5 text-blue-600 focus:ring-blue-500 rounded"
                  />
                  <span className="ml-3 text-sm text-gray-900">
                    I am a property dealer
                  </span>
                </label>

                <button
                  onClick={handleProfileSubmit}
                  className="w-full bg-blue-600 hover:bg-blue-700 text-white font-medium py-3 px-4 rounded-lg transition-colors"
                >
                  Next
                </button>
              </div>
            )}

            {step === 'question' && (
              <div className="space-y-4 pb-4">
                <div className="bg-gray-50 p-4 rounded-lg mb-4">
                  <h4 className="font-medium text-gray-900 text-sm mb-1">Property</h4>
                  <p className="text-gray-600 text-xs">{property.heading}</p>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1.5">
                    Any questions about this property?
                  </label>
                  <textarea
                    placeholder="e.g., Is it available for immediate possession? What are the amenities?"
                    value={propertyQuestion}
                    onChange={(e) => setPropertyQuestion(e.target.value)}
                    rows={4}
                    className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm resize-none"
                  />
                  <p className="text-xs text-gray-500 mt-1">Optional</p>
                </div>

                <button
                  onClick={handleFinalSubmit}
                  className="w-full bg-blue-600 hover:bg-blue-700 text-white font-medium py-3 px-4 rounded-lg transition-colors"
                >
                  Save Interest
                </button>
              </div>
            )}

            {step === 'success' && (
              <div className="text-center py-8 space-y-4">
                <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto">
                  <CheckCircle className="w-8 h-8 text-green-600" />
                </div>
                <h3 className="text-xl font-semibold text-gray-900">
                  Interest Saved!
                </h3>
                <p className="text-gray-600 text-sm max-w-md mx-auto leading-relaxed">
                  Your interest has been recorded. You can view all your saved properties in the interests section.
                </p>
                <button
                  onClick={onClose}
                  className="w-full max-w-xs mx-auto block bg-blue-600 hover:bg-blue-700 text-white font-medium py-3 px-4 rounded-lg transition-colors"
                >
                  Close
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </>
  );
};
