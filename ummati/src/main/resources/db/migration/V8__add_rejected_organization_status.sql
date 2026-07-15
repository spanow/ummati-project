ALTER TABLE organizations ADD CONSTRAINT organizations_status_check
    CHECK (status IN ('PENDING', 'ACTIVE', 'REJECTED', 'SUSPENDED', 'ARCHIVED'));