CREATE TYPE project_perm AS ENUM ('admin', 'collaborator');

CREATE TABLE project_usr (
  project_id BIGINT NOT NULL REFERENCES project,
  usr_id     BIGINT NOT NULL REFERENCES usr,
  perm       project_perm NOT NULL,
  UNIQUE(project_id, usr_id) -- unlike usr_group perms, only one type is permitted
);

CREATE INDEX project_usr_idx_by_usr ON project_usr(usr_id, project_id);

CREATE TABLE project_usr_group (
  project_id   BIGINT NOT NULL REFERENCES project,
  usr_group_id BIGINT NOT NULL REFERENCES usr_group,
  perm         project_perm NOT NULL,
  UNIQUE(project_id, usr_group_id) -- unlike usr_group perms, only one type is permitted
);

CREATE INDEX project_usr_group_idx_by_usr_group ON project_usr_group(usr_group_id, project_id);

------------------------------------------------------------------------------------------------------------------------

CREATE VIEW projects_usr_access AS
  WITH RECURSIVE graph AS (
    SELECT usr_id, grp_id, perm from usr_group_usr
    UNION
    SELECT g.usr_id, t.to_id, t.perm
      FROM usr_group_tree t, graph g
     WHERE g.grp_id = t.from_id
       AND g.perm = t.perm
  )
  SELECT usr_id, project_id, perm from project_usr
  UNION ALL
  SELECT g.usr_id, a.project_id, a.perm
    FROM project_usr_group a, graph g
   WHERE g.grp_id = a.usr_group_id;

CREATE OR REPLACE VIEW projects_by_owner_type AS
  SELECT user_type_by_username(username) owner_type,
         hll_add_agg(hll_hash_bigint(p.id)) projects,
         count(1) count
    FROM project p, usr u, projects_usr_access a
   WHERE u.id = a.usr_id
     AND a.project_id = p.id
     AND username IS NOT NULL
   GROUP BY user_type_by_username(username);

------------------------------------------------------------------------------------------------------------------------

INSERT INTO project_usr
SELECT p.id AS project_id,
       p.usr_id,
       'admin'::project_perm AS perm
  FROM project p;

ALTER TABLE project DROP COLUMN usr_id;
