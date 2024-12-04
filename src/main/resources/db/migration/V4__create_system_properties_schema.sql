create table system_properties (
id bigint PRIMARY KEY NOT NULL GENERATED ALWAYS AS IDENTITY,
key varchar(255),
value varchar(255),
description varchar(255),
  created timestamp,
  modified timestamp
);