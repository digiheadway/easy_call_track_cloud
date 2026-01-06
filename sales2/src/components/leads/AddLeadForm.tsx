import React, { useEffect } from 'react';
import { useForm } from '../../hooks';
import { Form, FormField, FormSection, FormActions } from '../ui';
import { Lead } from '../../types';
import TagInput from '../common/TagInput';
import { tagOptions } from '../../data/options';
import { dropdownOptions } from '../../data/options';
import Dropdown from '../common/Dropdown';
import { formatPhoneNumber, getClipboardText, setClipboardText, isValidPhoneNumber } from '../../utils/phone';

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
  stage: 'Init - General Enquiry',
  intent: 'Warm',
  budget: 0,
  preferredLocation: [],
  preferredSize: [],
  note: '',
  requirementDescription: '',
  propertyType: [],
  purchaseTimeline: '',
  purpose: '',
  about: '',
  segment: 'Panipat',
  source: 'Organic Social Media',
  priority: '3',
  tags: [],
  assignedTo: [],
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
    const input = e.target.value.replace(/\D/g, '').slice(0, 10);
    handleChange('phone', input);
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
              placeholder="Enter phone number"
              required
              maxLength={10}
            />
          </FormField>

          <FormField label="Name">
            <input
              type="text"
              value={values.name}
              onChange={(e) => handleChange('name', e.target.value)}
              className="input"
              placeholder="Enter lead's name"
            />
          </FormField>

          <FormField label="Budget (in Lakhs)">
            <input
              type="number"
              value={values.budget}
              onChange={(e) => handleChange('budget', parseFloat(e.target.value))}
              className="input"
              placeholder="Enter budget amount"
              min="0"
            />
          </FormField>

          <FormField label="Segment">
            <Dropdown
              options={dropdownOptions.segment}
              value={values.segment}
              onChange={(value) => handleChange('segment', value)}
              placeholder="Select segment"
            />
          </FormField>

          <FormField label="Source" required>
            <Dropdown
              options={dropdownOptions.source}
              value={values.source}
              onChange={(value) => handleChange('source', value)}
              placeholder="Select source"
            />
          </FormField>

          <FormField label="Tags">
            <TagInput
              options={tagOptions.tags}
              value={values.tags ?? []}
              onChange={(value) => handleChange('tags', value)}
              placeholder="Add tags"
            />
          </FormField>
          
          <FormField label="Note">
            <textarea
              value={values.note}
              onChange={(e) => handleChange('note', e.target.value)}
              className="input h-24"
              placeholder="Additional notes or comments"
            />
          </FormField>
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
          className="btn btn-primary"
          disabled={isSubmitting}
        >
          {isSubmitting ? 'Adding Lead...' : 'Add Lead'}
        </button>
      </FormActions>
    </Form>
  );
};

export default AddLeadForm;