-- drop function visitor_stats_per_hour_add;
-- drop table visitor_stats_per_hour;
-- delete from flyway_schema_history where version = '4.4';

------------------------------------------------------------------------------------------------------------------------

CREATE TYPE response_type AS ENUM ('1xx', '2xx', '3xx', '4xx', '5xx', 'other');

CREATE TABLE visitor_stats_per_hour (
  date     DATE          NOT NULL,
  hour     SMALLINT      NOT NULL CHECK(hour >= 0 AND hour <= 23),
  response response_type NOT NULL,
  ips      HLL           NOT NULL,
  requests INTEGER       NOT NULL CHECK(requests > 0),
  PRIMARY KEY(date, hour, response)
);

------------------------------------------------------------------------------------------------------------------------

CREATE FUNCTION visitor_stats_per_hour_add(
  arg_time     TIMESTAMPTZ,
  arg_response response_type,
  arg_ips      TEXT[],
  arg_requests INTEGER
) RETURNS void AS $$
DECLARE
  time_utc TIMESTAMP;
  new_ips  HLL;
  tgt_ctid visitor_stats_per_hour.ctid%TYPE;
  tgt_date visitor_stats_per_hour.date%TYPE;
  tgt_hour visitor_stats_per_hour.hour%TYPE;
BEGIN

  SELECT (arg_time at time zone 'UTC')::timestamp
    INTO time_utc;

  SELECT time_utc::date, EXTRACT(hours from time_utc)
    INTO tgt_date, tgt_hour;

  SELECT coalesce(hll_add_agg(hll_hash_text(ip)), hll_empty())
    INTO new_ips
    FROM unnest(arg_ips) as ip;

  SELECT ctid
    INTO tgt_ctid
    FROM visitor_stats_per_hour
   WHERE date = tgt_date AND hour = tgt_hour AND response = arg_response;

  IF NOT FOUND THEN
    -- Insert new row
    INSERT INTO visitor_stats_per_hour(date, hour, response, ips, requests)
      VALUES(tgt_date, tgt_hour, arg_response, new_ips, arg_requests);

  ELSE
    -- Update existing row
    UPDATE visitor_stats_per_hour
       SET ips = ips || new_ips,
           requests = requests + arg_requests
     WHERE ctid = tgt_ctid;

  END IF;

END;
$$ LANGUAGE plpgsql;
