-- Migration: Add composite unique key on (uniqueId, landingpage, date)
-- Run this SQL on your database

-- First, drop the existing unique key on uniqueId (if exists)
-- Note: Replace 'uniqueId' with the actual constraint name if different
ALTER TABLE tracking_stats DROP INDEX IF EXISTS uniqueId;

-- Add new composite unique key
ALTER TABLE tracking_stats
ADD UNIQUE KEY unique_tracking (uniqueId, landingpage, date);

-- If landingpage column doesn't exist, create it first:
-- ALTER TABLE tracking_stats ADD COLUMN landingpage VARCHAR(50) DEFAULT NULL AFTER uniqueId;