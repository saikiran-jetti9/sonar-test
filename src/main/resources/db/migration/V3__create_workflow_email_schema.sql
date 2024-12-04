create table workflow_email (
  id bigint PRIMARY KEY NOT NULL GENERATED ALWAYS AS IDENTITY,
  workflow_id bigint,
  email varchar(255),
  name varchar(255),
  status varchar(255),
  created timestamp,
  modified timestamp
);