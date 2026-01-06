import React, { useState, useRef, useEffect } from 'react';
import { X, ChevronDown } from 'lucide-react';
import { TagOption } from '../../types';

interface TagInputProps {
  value: string[];
  onChange: (value: string[]) => void;
  options: TagOption[];
  placeholder?: string;
  label?: string;
  singleSelect?: boolean;
}

const TagInput: React.FC<TagInputProps> = ({
  value = [],
  onChange,
  options = [],
  placeholder = 'Add items...',
  label,
  singleSelect = false,
}) => {
  const [inputValue, setInputValue] = useState('');
  const [isOpen, setIsOpen] = useState(false);
  const [filteredOptions, setFilteredOptions] = useState<TagOption[]>(options);
  const containerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    // Ensure options is an array before filtering
    if (!Array.isArray(options)) {
      setFilteredOptions([]);
      return;
    }

    if (inputValue) {
      setFilteredOptions(
        options.filter((option) =>
          option.label.toLowerCase().includes(inputValue.toLowerCase())
        )
      );
    } else {
      setFilteredOptions(options);
    }
  }, [inputValue, options]);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        containerRef.current &&
        event.target instanceof Node &&
        !containerRef.current.contains(event.target)
      ) {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newValue = e.target.value;
    setInputValue(newValue);
    if (!isOpen) setIsOpen(true);
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && inputValue) {
      e.preventDefault();

      // Check if the input value matches an existing option
      const existingOption = options.find(
        (option) => option.label.toLowerCase() === inputValue.toLowerCase()
      );

      if (existingOption) {
        if (singleSelect) {
          onChange([existingOption.label]);
        } else if (!value.includes(existingOption.label)) {
          onChange([...value, existingOption.label]);
        }
      } else if (!singleSelect) {
        // Only add custom values in multi-select mode
        onChange([...value, inputValue]);
      }

      setInputValue('');
      setIsOpen(false);
    } else if (e.key === 'Backspace' && !inputValue && value.length > 0) {
      const newValue = value.slice(0, -1);
      onChange(newValue);
      setInputValue('');
    }
  };

  const handleOptionClick = (option: TagOption) => {
    if (singleSelect) {
      onChange([option.label]);
      setInputValue('');
      setIsOpen(false);
    } else if (!value.includes(option.label)) {
      onChange([...value, option.label]);
      setInputValue('');
    }
    inputRef.current?.focus();
  };

  const removeTag = (tag: string) => {
    const newValue = value.filter((t) => t !== tag);
    onChange(newValue);
    if (singleSelect) {
      setInputValue('');
    }
  };

  const handleInputFocus = () => {
    setIsOpen(true);
  };

  return (
    <div className="relative" ref={containerRef}>
      {label && (
        <label className="block text-sm font-medium text-gray-700 mb-1">
          {label}
        </label>
      )}
      <div
        className="min-h-10 px-3 py-2 border border-gray-300 rounded-md shadow-sm focus-within:ring-1 focus-within:ring-blue-500 focus-within:border-blue-500"
        onClick={() => inputRef.current?.focus()}
      >
        <div className="flex flex-wrap gap-1">
          {!singleSelect &&
            value.map((tag) => (
              <span key={tag} className="tag">
                {tag}
                <button
                  type="button"
                  className="tag-close"
                  onClick={() => removeTag(tag)}
                >
                  <X size={10} />
                </button>
              </span>
            ))}
          {singleSelect && value[0] && (
            <span className="tag">
              {value[0]}
              <button
                type="button"
                className="tag-close"
                onClick={() => removeTag(value[0])}
              >
                <X size={10} />
              </button>
            </span>
          )}
          <input
            ref={inputRef}
            type="text"
            className="flex-1 min-w-[60px] outline-none text-sm"
            value={inputValue}
            onChange={handleInputChange}
            onKeyDown={handleKeyDown}
            onFocus={handleInputFocus}
            placeholder={value.length === 0 ? placeholder : ''}
          />
        </div>
        <button
          type="button"
          className="absolute inset-y-0 right-0 flex items-center pr-2"
          onClick={() => setIsOpen(!isOpen)}
        >
          <ChevronDown
            className={`h-4 w-4 text-gray-400 transition-transform ${
              isOpen ? 'transform rotate-180' : ''
            }`}
          />
        </button>
      </div>

      {isOpen && filteredOptions.length > 0 && (
        <div className="absolute z-10 mt-1 w-full bg-white border border-gray-200 shadow-lg max-h-60 rounded-md py-1 text-base overflow-auto focus:outline-none sm:text-sm animate-fade-in">
          {filteredOptions.map((option) => (
            <div
              key={option.value}
              className={`cursor-pointer select-none relative py-2 pl-3 pr-9 hover:bg-blue-50 border-l-2 border-transparent hover:border-blue-500 ${
                value.includes(option.label) ? 'bg-blue-50 border-l-2 border-blue-500' : ''
              }`}
              onClick={() => handleOptionClick(option)}
            >
              {option.label}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default TagInput;