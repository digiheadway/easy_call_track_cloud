import { createContext, useContext, useState } from 'react';

const PersonModalContext = createContext();

export function PersonModalProvider({ children }) {
    const [personData, setPersonData] = useState(null); // { phoneNumber: string, name: string, ... }
    const [isOpen, setIsOpen] = useState(false);

    const openPersonModal = (data) => {
        setPersonData(data);
        setIsOpen(true);
    };

    const closePersonModal = () => {
        setIsOpen(false);
        // Delay clearing data for animation
        setTimeout(() => setPersonData(null), 300);
    };

    return (
        <PersonModalContext.Provider value={{ personData, isOpen, openPersonModal, closePersonModal }}>
            {children}
        </PersonModalContext.Provider>
    );
}

export function usePersonModal() {
    return useContext(PersonModalContext);
}
