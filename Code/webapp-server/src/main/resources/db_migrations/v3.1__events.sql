CREATE TABLE event (
  project_id  BIGINT      NOT NULL REFERENCES project,
  ord         INTEGER     NOT NULL CHECK (ord > 0),
  type        SMALLINT    NOT NULL CHECK (type > 0),
  data        JSONB       NOT NULL,
  usr_id      BIGINT      NOT NULL REFERENCES usr,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (project_id, ord)
);
