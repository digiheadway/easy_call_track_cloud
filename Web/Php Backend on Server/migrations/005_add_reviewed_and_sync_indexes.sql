-- =====================================================
-- MIGRATION: Add reviewed column and ensure updated_at columns
-- Run this on the production database
-- =====================================================

-- 1. Add reviewed column to calls table (if not exists)
-- The reviewed column allows tracking whether a call has been reviewed
-- This syncs bidirectionally between app and web dashboard
ALTER TABLE calls 
ADD COLUMN IF NOT EXISTS reviewed TINYINT(1) NOT NULL DEFAULT 0;

-- 2. Ensure updated_at columns exist and have proper defaults
-- These are critical for delta sync to work correctly

-- For calls table
ALTER TABLE calls 
MODIFY COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- For contacts table
ALTER TABLE contacts 
MODIFY COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- 3. Add index on updated_at columns for efficient delta sync queries
CREATE INDEX IF NOT EXISTS idx_calls_updated_at ON calls(updated_at);
CREATE INDEX IF NOT EXISTS idx_calls_org_updated ON calls(org_id, updated_at);

CREATE INDEX IF NOT EXISTS idx_contacts_updated_at ON contacts(updated_at);
CREATE INDEX IF NOT EXISTS idx_contacts_org_updated ON contacts(org_id, updated_at);

-- 4. Verify the changes
DESCRIBE calls;
DESCRIBE contacts;

-- =====================================================
-- NOTES:
-- - The ON UPDATE CURRENT_TIMESTAMP ensures updated_at is auto-updated
-- - Delta sync queries use: WHERE updated_at > FROM_UNIXTIME(last_sync)
-- - Indexes improve performance for frequent sync queries
-- =====================================================
