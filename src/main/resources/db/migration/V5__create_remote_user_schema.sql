create table remote_user (
id bigint PRIMARY KEY NOT NULL GENERATED ALWAYS AS IDENTITY,
username varchar(255),
  created timestamp,
  modified timestamp
);

create table bookmark (
    id bigint primary key not null generated always as identity,
    user_id bigint,
    workflow_id bigint,
    created timestamp,
    modified timestamp,
    CONSTRAINT bookmark_user_id_fk FOREIGN KEY (user_id) REFERENCES remote_user(id)
);