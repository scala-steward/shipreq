/*
DROP FUNCTION IF EXISTS create_task_v01(int2,json,int2);
DROP TABLE    IF EXISTS task;
DROP SEQUENCE IF EXISTS node_seq;
DROP SEQUENCE IF EXISTS task_seq;
*/

CREATE SEQUENCE node_seq START WITH 1;
CREATE SEQUENCE task_seq START WITH 1000;

CREATE TABLE task (
  id               BIGINT       PRIMARY KEY DEFAULT NEXTVAL('task_seq')
  ,type            INT2         NOT NULL
  ,data            JSON         NULL
  ,priority        INT2         NOT NULL
  ,priority_base   INT2         NOT NULL
  ,status          "char"       NOT NULL CHECK(status='i' OR status='s' OR status='f')
  ,node            INT4         NULL
  ,worker          INT2         NULL
  ,failure_count   INT2         NOT NULL DEFAULT 0 CHECK(failure_count >= 0)
  ,created_at      TIMESTAMPTZ  NOT NULL
  ,updated_at      TIMESTAMPTZ  NOT NULL CHECK(updated_at >= created_at)
  ,effective_from  TIMESTAMPTZ  NOT NULL
  ,CONSTRAINT task_assignment CHECK(
     NOT(                                       -- Prevent the following:
          (status <> 'i' AND node IS NOT NULL)  -- Node being assigned when task is finished
       OR (node IS NULL AND worker IS NOT NULL) -- Worker being assigned without a node
     ))
);

CREATE FUNCTION create_task_v01(IN type INT2, IN data JSON, IN pri INT2)
RETURNS void AS $$
DECLARE n TIMESTAMPTZ;
BEGIN
  SELECT now() INTO n;
  INSERT INTO task(type, data, priority_base, priority, status, created_at, updated_at, effective_from)
    VALUES($1, $2, $3, $3, 'i', n, n, n);
END $$ LANGUAGE plpgsql;

/*
select create_task_v01(1::int2, NULL::json, 50::int2);
select create_task_v01(1::int2, NULL::json, 60::int2);
select * from task;
*/