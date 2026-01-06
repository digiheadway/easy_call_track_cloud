import React from 'react';
import Modal from '../common/Modal';
import { Lead } from '../../types';
import AddLeadForm from './AddLeadForm';
import { useAppContext } from '../../contexts/AppContext';

interface AddLeadModalProps {
  isOpen: boolean;
  onClose: () => void;
}

const AddLeadModal: React.FC<AddLeadModalProps> = ({ isOpen, onClose }) => {
  const { addLead } = useAppContext();

  const handleSubmit = async (data: Omit<Lead, 'id' | 'createdAt' | 'updatedAt'>) => {
    await addLead(data);
    onClose();
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      size="lg"
      title="Add New Lead"
    >
      <AddLeadForm
        onSubmit={handleSubmit}
        onCancel={onClose}
      />
    </Modal>
  );
};

export default AddLeadModal;