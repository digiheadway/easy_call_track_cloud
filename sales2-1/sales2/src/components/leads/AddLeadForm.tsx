
import React, { useEffect } from 'react';
import { useForm } from '../../hooks';
import { Form, FormField, FormSection, FormActions } from '../ui';
import { Lead, LeadStage, Purpose, Source, Priority } from '../../types';
import TagInput from '../common/TagInput';
import { dropdownOptions } from '../../data/options';
import { useAppContext } from '../../contexts/AppContext';
import Dropdown from '../common/Dropdown';
import { formatPhoneNumber, getClipboardText, isValidPhoneNumber } from '../../utils/phone';

type AddLeadFormData = Omit<Lead, 'id' | 'createdAt' | 'updatedAt'> & {
  name?: string;
  budget?: number;
  segment?: Lead['segment'];
};

interface AddLeadFormProps {
  onSubmit: (data: AddLeadFormData) => Promise<void>;
  onCancel: () => void;
  initialValues?: Partial<AddLeadFormData>;
}

const initialState: AddLeadFormData = {
  name: '',
  phone: '',
  stage: 'Init - General Enquiry' as LeadStage,
  intent: 'Warm',
  budget: 0,
  preferredLocation: [],
  preferredSize: [],
  address: '',
  note: '',
  requirementDescription: '',
  propertyType: [],
  purpose: '' as Purpose,
  about: '',
  segment: 'Panipat',
  source: 'Organic Social Media' as Source,
  priority: 'General' as Priority,
  tags: [],
  assignedTo: [],
  data3: '1', // Default to sync contacts
};

const AddLeadForm: React.FC<AddLeadFormProps> = ({
  onSubmit,
  onCancel,
  initialValues = {},
}) => {
  const { values, handleChange, handleSubmit, isSubmitting } = useForm({
    initialValues: { ...initialState, ...initialValues },
    onSubmit,
  });

  const { options } = useAppContext();

  useEffect(() => {
    const checkClipboard = async () => {
      if (!values.phone) {
        const clipboardText = await getClipboardText();
        const formattedNumber = formatPhoneNumber(clipboardText);
        if (isValidPhoneNumber(formattedNumber)) {
          handleChange('phone', formattedNumber);
        }
      }
    };
    checkClipboard();
  }, []);

  const handlePhoneChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    let input = e.target.value;
    const hasPlus = input.startsWith('+');
    input = input.replace(/\D/g, '').slice(0, 15);
    if (hasPlus) {
      input = '+' + input;
    }
    handleChange('phone', input);
  };

  const handleBudgetChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const budget = parseFloat(e.target.value);
    handleChange('budget', budget);
  };

  const handleBudgetBlur = (e: React.ChangeEvent<HTMLInputElement>) => {
    const budget = parseFloat(e.target.value);

    // Auto-manage contact sync based on budget
    if (budget < 70 && values.data3 === '1') {
      // Auto-uncheck contact sync if budget is below 70
      handleChange('data3', '');
    } else if (budget >= 70 && values.data3 !== '1') {
      // Auto-check contact sync if budget is 70 or more
      handleChange('data3', '1');
    }
  };

  const handleContactSyncChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    handleChange('data3', e.target.checked ? '1' : '');
  };

  return (
    <Form onSubmit={handleSubmit}>
      <FormSection>
        <div className="grid grid-cols-1 gap-4">
          <FormField label="Phone" required>
            <input
              type="tel"
              value={values.phone}
              onChange={handlePhoneChange}
              className="input"
              placeholder="Enter phone number (6-15 digits)"
              required
              maxLength={16}
            />
          </FormField>

          <FormField label="Name">
            <input
              type="text"
              value={values.name}
              onChange={(e: React.ChangeEvent<HTMLInputElement>) => handleChange('name', e.target.value)}
              className="input"
              placeholder="Enter lead's name"
            />
          </FormField>

          <FormField label="Budget (in Lakhs)">
            <input
              type="number"
              value={values.budget}
              onChange={handleBudgetChange}
              onBlur={handleBudgetBlur}
              className="input"
              placeholder="Enter budget amount"
              min="0"
            />
          </FormField>

          <FormField label="Segment">
            <Dropdown
              options={dropdownOptions.segment}
              value={values.segment}
              onChange={(value: string) => handleChange('segment', value)}
              placeholder="Select segment"
            />
          </FormField>

          <FormField label="Source" required>
            <Dropdown
              options={dropdownOptions.source}
              value={values.source}
              onChange={(value: string) => handleChange('source', value as Source)}
              placeholder="Select source"
            />
          </FormField>

          <FormField label="Tags">
            <TagInput
              options={options.tags}
              value={values.tags ?? []}
              onChange={(value: string[]) => handleChange('tags', value)}
              placeholder="Add tags"
            />
          </FormField>

          <FormField label="Address">
            <input
              type="text"
              value={values.address}
              onChange={(e: React.ChangeEvent<HTMLInputElement>) => handleChange('address', e.target.value)}
              className="input"
              placeholder="Enter lead's address (optional)"
            />
          </FormField>

          <FormField label="Note">
            <textarea
              value={values.note}
              onChange={(e) => handleChange('note', e.target.value)}
              className="input h-16"
              placeholder="Additional notes or comments"
              rows={2}
            />
          </FormField>

          <div className="flex items-center space-x-2">
            <input
              type="checkbox"
              id="contactSync"
              checked={values.data3 === '1'}
              onChange={handleContactSyncChange}
              className="w-4 h-4 text-blue-600 bg-gray-100 border-gray-300 rounded focus:ring-blue-500 focus:ring-2"
            />
            <label htmlFor="contactSync" className="text-sm font-medium text-gray-700">
              Keep updated with Contacts
            </label>
          </div>
        </div>
      </FormSection>

      <FormActions>
        <button
          type="button"
          className="btn btn-outline"
          onClick={onCancel}
        >
          Cancel
        </button>
        <button
          type="submit"
          className="btn btn-primary w-full"
          disabled={isSubmitting}
        >
          {isSubmitting ? 'Adding Lead...' : 'Add Lead'}
        </button>
      </FormActions>
    </Form>
  );
};

export default AddLeadForm;