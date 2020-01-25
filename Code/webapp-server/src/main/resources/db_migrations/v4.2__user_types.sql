-- drop view projects_by_owner_type;
-- drop view users_by_type;
-- drop function user_type_by_username;

create or replace function user_type_by_username(varchar) returns varchar as $$
  select case
           when $1 = 'japgolly' then 'staff'
           else 'public'
         end
$$ language sql immutable;

create or replace view users_by_type as
  select user_type_by_username(username) user_type,
         hll_add_agg(hll_hash_bigint(id)) users,
         count(1) count
  from usr
  group by user_type_by_username(username);

create or replace view projects_by_owner_type as
  select user_type_by_username(username) owner_type,
         hll_add_agg(hll_hash_bigint(p.id)) projects,
         count(1) count
  from project p, usr u
  where usr_id = u.id
  group by user_type_by_username(username);
