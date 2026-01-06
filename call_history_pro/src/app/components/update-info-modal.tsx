
'use client';

import { useState, useEffect } from 'react';
import type { Caller } from '@/lib/types';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';

interface UpdateInfoModalProps {
  isOpen: boolean;
  onClose: () => void;
  caller: Caller;
  onUpdate: (callerId: string, info: { custom_name?: string; caller_type?: string }) => void;
}

const CALLER_TYPE_OPTIONS = [
  'Dealer',
  'Builder Sales',
  'Low Budget Lead',
  'Waste Lead',
  'Good Lead',
  'Seller',
  'Renting',
  'Researching',
  'Personal',
  'Sales',
  'Other'
];

export default function UpdateInfoModal({ isOpen, onClose, caller, onUpdate }: UpdateInfoModalProps) {
  const [name, setName] = useState('');
  const [type, setType] = useState('');
  const [customType, setCustomType] = useState('');

  useEffect(() => {
    if (isOpen) {
      setName(caller.custom_name || '');
      
      if (!caller.caller_type) {
        setType('');
        setCustomType('');
      } else {
        const predefinedType = CALLER_TYPE_OPTIONS.includes(caller.caller_type);
        if (predefinedType) {
          setType(caller.caller_type);
          setCustomType('');
        } else {
          setType('Other');
          setCustomType(caller.caller_type);
        }
      }
    }
  }, [isOpen, caller]);

  const handleUpdate = () => {
    const finalType = type === 'Other' ? customType : type;
    onUpdate(caller.id, { custom_name: name, caller_type: finalType });
    onClose();
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-[425px]">
        <DialogHeader>
          <DialogTitle>Update Info for {caller.phone}</DialogTitle>
          <DialogDescription>
            Add or update the custom name and type for this contact.
          </DialogDescription>
        </DialogHeader>
        <div className="grid gap-4 py-4">
          <div className="space-y-2">
            <Label htmlFor="name">Name</Label>
            <Input
              id="name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Enter custom name"
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="type">Type</Label>
              <Select value={type} onValueChange={setType}>
                <SelectTrigger id="type">
                  <SelectValue placeholder="Select a type" />
                </SelectTrigger>
                <SelectContent>
                  {CALLER_TYPE_OPTIONS.map(option => (
                    <SelectItem key={option} value={option}>
                      {option}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
          </div>
          {type === 'Other' && (
            <div className="space-y-2">
              <Label htmlFor="custom-type">Custom Type</Label>
              <Input
                id="custom-type"
                value={customType}
                onChange={(e) => setCustomType(e.target.value)}
                placeholder="Enter custom type"
              />
            </div>
          )}
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>Cancel</Button>
          <Button onClick={handleUpdate}>Save Changes</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
    
