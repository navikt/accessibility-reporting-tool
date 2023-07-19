CREATE TABLE IF NOT EXISTS organization_unit
(
    organization_unit_id VARCHAR PRIMARY KEY,
    name                 VARCHAR(100) NOT NULL,
    parent_unit          VARCHAR REFERENCES organization_unit (organization_unit_id)
);

CREATE TABLE IF NOT EXISTS report
(
    report_id            VARCHAR PRIMARY KEY,
    organization_unit_id VARCHAR REFERENCES organization_unit (organization_unit_id),
    created              TIMESTAMP,
    last_changed         TIMESTAMP,
    report_data           jsonb
);

CREATE TABLE IF NOT EXISTS changelog
(
    report_id VARCHAR NOT NULL REFERENCES report (report_id),
    time      TIMESTAMP,
    new_data  jsonb,
    old_data  jsonb
);
