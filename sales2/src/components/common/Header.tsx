import React from 'react';
import { Menu, Search, Bell, Filter } from 'lucide-react';

interface HeaderProps {
  title: string;
  onOpenSidebar: () => void;
  onOpenFilter?: () => void;
  showFilter?: boolean;
  onSearch?: (query: string) => void;
}

const Header: React.FC<HeaderProps> = ({
  title,
  onOpenSidebar,
  onOpenFilter,
  showFilter = false,
  onSearch,
}) => {
  const handleSearch = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (onSearch) {
      onSearch(e.target.value);
    }
  };

  return (
    <header className="bg-white shadow-sm py-3 px-4 flex items-center justify-between">
      <div className="flex items-center">
        <button
          onClick={onOpenSidebar}
          className="p-2 rounded-md text-gray-500 hover:text-gray-900 hover:bg-gray-100 focus:outline-none md:hidden"
        >
          <Menu size={20} />
        </button>
        <h1 className="text-xl font-semibold ml-2 md:ml-0 text-gray-800">
          {title}
        </h1>
      </div>

      <div className="flex items-center space-x-3">
        {onSearch && (
          <div className="relative w-64 hidden md:block">
            <div className="absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none">
              <Search size={16} className="text-gray-400" />
            </div>
            <input
              type="text"
              className="input pl-10"
              placeholder="Search..."
              onChange={handleSearch}
            />
          </div>
        )}

        {showFilter && onOpenFilter && (
          <button
            onClick={onOpenFilter}
            className="p-2 rounded-md text-gray-500 hover:text-gray-900 hover:bg-gray-100 focus:outline-none"
          >
            <Filter size={20} />
          </button>
        )}

        <div className="flex items-center">
          <div className="h-8 w-8 rounded-full bg-blue-800 flex items-center justify-center text-white font-bold">
            U
          </div>
        </div>
      </div>
    </header>
  );
};

export default Header;
