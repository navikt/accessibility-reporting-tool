CREATE TABLE IF NOT EXISTS team
(
    team_id     SERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    parent_unit INT REFERENCES team (team_id)
);

CREATE TABLE IF NOT EXISTS report
(
    report_id    SERIAL PRIMARY KEY,
    team_id      INT REFERENCES team (team_id),
    name         VARCHAR(100) NOT NULL,
    parent_unit  INT REFERENCES team (team_id),
    url          VARCHAR      NOT NULL,
    created      TIMESTAMP,
    last_changed TIMESTAMP,
    reportdata   jsonb
);

CREATE TABLE IF NOT EXISTS changelog
(
    report_id INT NOT NULL REFERENCES report (report_id),
    new_data  jsonb,
    old_data  jsonb
);
