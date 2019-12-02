FROM wrouesnel/postgres_exporter:v0.8.0

ENV PG_EXPORTER_DISABLE_DEFAULT_METRICS=true \
    PG_EXPORTER_DISABLE_SETTINGS_METRICS=true \
    PG_EXPORTER_EXTEND_QUERY_PATH=/queries.yml

COPY ./queries.yml /queries.yml
