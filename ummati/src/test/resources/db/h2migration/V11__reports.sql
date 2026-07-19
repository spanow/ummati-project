CREATE TABLE reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id UUID NOT NULL REFERENCES users(id),
    target_type VARCHAR(30) NOT NULL CHECK (target_type IN ('ORGANIZATION', 'EVENT', 'ORG_ANNOUNCEMENT')),
    target_id UUID NOT NULL,
    reason VARCHAR(30) NOT NULL CHECK (reason IN ('SPAM', 'INAPPROPRIATE_CONTENT', 'FRAUD', 'HARASSMENT', 'OTHER')),
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'REVIEWED', 'DISMISSED', 'ACTION_TAKEN')),
    resolution_note TEXT,
    reviewed_by UUID REFERENCES users(id),
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_reports_status ON reports(status);
CREATE INDEX idx_reports_target ON reports(target_type, target_id);
CREATE INDEX idx_reports_reporter ON reports(reporter_id);
