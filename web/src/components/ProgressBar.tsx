/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

import React, { useEffect } from 'react';
import { useLoading } from './LoadingContext';

const ProgressBar: React.FC = () => {
  const { isLoading } = useLoading();

  if (!isLoading) return null;

  return (
    <div className="fixed top-0 left-0 right-0 z-[9999] h-1 overflow-hidden bg-transparent">
      <div className="h-full bg-indigo-500 animate-progress origin-left"></div>
    </div>
  );
};

export default ProgressBar;
