/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

import React, { createContext, useContext, useState, useCallback } from 'react';

interface LoadingContextType {
  isLoading: boolean;
  startLoading: () => void;
  stopLoading: () => void;
}

const LoadingContext = createContext<LoadingContextType | undefined>(undefined);

export const LoadingProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [activeRequests, setActiveRequests] = useState(0);

  const startLoading = useCallback(() => {
    setActiveRequests((prev) => prev + 1);
  }, []);

  const stopLoading = useCallback(() => {
    setActiveRequests((prev) => Math.max(0, prev - 1));
  }, []);

  const isLoading = activeRequests > 0;

  return (
    <LoadingContext.Provider value={{ isLoading, startLoading, stopLoading }}>
      {children}
    </LoadingContext.Provider>
  );
};

export const useLoading = () => {
  const context = useContext(LoadingContext);
  if (!context) {
    throw new Error('useLoading must be used within a LoadingProvider');
  }
  return context;
};
