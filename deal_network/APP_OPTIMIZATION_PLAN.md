# App.tsx Size Reduction Plan

**Current:** 2,634 lines  
**Target:** Under 1,500 lines  
**Status:** Foundations ready, integration pending

## Why App.tsx is Still Large

We created great utilities but haven't integrated them yet. The file still contains:

- Property loading logic (128 lines) → Use `usePropertyData` ✅
- Filter/search (500+ lines) → Use `usePropertyFilters` ✅  
- CRUD operations (400+ lines) → Use `propertyOperations` ✅
- Large useEffects (800+ lines) → Needs refactoring
- UI mixed with logic → Extract components

## Quick Win: Extract UI Components (SAFEST)

### 1. Create PropertyListView Component

Extract the property list rendering (~100 lines saved).

### 2. Create FilterBar Component

Extract filter buttons UI (~80 lines saved).

### 3. Create PropertyModals Component  

Consolidate modal logic (~200 lines saved).

**Total Quick Win: ~380 lines removed with low risk**

## Recommendation

Start by extracting UI components (safer) before integrating data hooks (riskier).

The hooks we created (`usePropertyData`, `usePropertyFilters`) are ready but need careful integration due to App.tsx's complexity.
