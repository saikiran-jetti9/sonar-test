create table workflow (
  id bigint PRIMARY KEY NOT NULL GENERATED ALWAYS AS IDENTITY,
  name varchar(255),
  description text,
  enabled boolean NOT NULL DEFAULT true,
  task_chain_is_valid boolean NOT NULL DEFAULT false,
  throttle_limit integer NOT NULL DEFAULT 100,
  paused boolean NOT NULL DEFAULT false,
  alias varchar(255),
  asset_ingestion_wait_time varchar(255),
  data_ingestion_wait_time varchar(255),
  created timestamp,
  modified timestamp
);

create table workflow_configuration (
  id bigint PRIMARY KEY NOT NULL GENERATED ALWAYS AS IDENTITY,
  workflow_id bigint NOT NULL,
  key varchar(255),
  value text,
  created timestamp,
  modified timestamp,
  CONSTRAINT workflow_configuration_workflow_id_fk FOREIGN KEY (workflow_id) REFERENCES workflow(id)
);

create table workflow_step (
  id bigint PRIMARY KEY NOT NULL GENERATED ALWAYS AS IDENTITY,
  workflow_id bigint NOT NULL,
  execution_order integer NOT NULL,
  name varchar(255),
  type varchar(255),
  created timestamp,
  modified timestamp,
  CONSTRAINT workflow_step_workflow_id_fk FOREIGN KEY (workflow_id) REFERENCES workflow(id)
);

create table workflow_step_configuration (
  id bigint PRIMARY KEY NOT NULL GENERATED ALWAYS AS IDENTITY,
  workflow_step_id bigint NOT NULL,
  key varchar(255),
  value text,
  created timestamp,
  modified timestamp,
  CONSTRAINT workflow_step_configuration_workflow_step_id_fk FOREIGN KEY (workflow_step_id) REFERENCES workflow_step(id)
);

create table workflow_instance (
  id bigint PRIMARY KEY NOT NULL GENERATED ALWAYS AS IDENTITY,
  workflow_id bigint NOT NULL,
  status varchar(255),
  completed timestamp,
  duration bigint,
  trigger_data text,
  log text,
  identifier varchar(255),
  error_message text,
  priority varchar(10) NOT NULL,
  delivery_type varchar(255),
  created timestamp,
  started timestamp,
  modified timestamp,
  CONSTRAINT workflow_instance_workflow_id_fk FOREIGN KEY (workflow_id) REFERENCES workflow(id)
);

create table workflow_instance_artifact (
  id bigint PRIMARY KEY NOT NULL GENERATED ALWAYS AS IDENTITY,
  workflow_step_id bigint NOT NULL,
  workflow_instance_id bigint NOT NULL,
  description varchar(255),
  filename varchar(255) NOT NULL,
  unique_filename varchar(255) NOT NULL,
  created timestamp,
  modified timestamp,
  CONSTRAINT workflow_instance_artifact_workflow_instance_id_fk FOREIGN KEY (workflow_instance_id) REFERENCES workflow_instance(id),
  CONSTRAINT workflow_instance_artifact_workflow_step_id_fk FOREIGN KEY (workflow_step_id) REFERENCES workflow_step(id)
);
