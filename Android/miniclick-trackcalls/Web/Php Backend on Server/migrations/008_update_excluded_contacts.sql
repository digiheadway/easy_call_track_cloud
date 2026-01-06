ALTER TABLE excluded_contacts 
ADD COLUMN exclude_from_sync TINYINT(1) DEFAULT 1,
ADD COLUMN exclude_from_list TINYINT(1) DEFAULT 1;

-- Initialize new columns based on is_active
UPDATE excluded_contacts SET exclude_from_sync = is_active, exclude_from_list = is_active;
