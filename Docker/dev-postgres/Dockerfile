FROM postgres:13.5

RUN sed -i 's|apt.postgresql.org|apt-archive.postgresql.org|g' /etc/apt/sources.list.d/pgdg.list

RUN apt-get -o Acquire::Check-Valid-Until=false -qq update \
 && apt-get -o Acquire::Check-Valid-Until=false -qq -y --no-install-recommends install \
      postgresql-13-hll

COPY ./create-extensions.sql /docker-entrypoint-initdb.d/00-create-extensions.sql

RUN chmod 666 /docker-entrypoint-initdb.d/00-create-extensions.sql
