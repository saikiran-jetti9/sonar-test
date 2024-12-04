CREATE TABLE template (
    id bigint PRIMARY KEY NOT NULL GENERATED ALWAYS AS IDENTITY,
    name varchar(255),
    description TEXT,
    primary_version_id  bigint,
    created TIMESTAMP,
    modified TIMESTAMP
);

CREATE TABLE Template_Version (
    id bigint PRIMARY KEY NOT NULL GENERATED ALWAYS AS IDENTITY,
    template_id BIGINT NOT NULL,
    template_code TEXT,
    template_description TEXT,
    created TIMESTAMP ,
    modified TIMESTAMP,
    CONSTRAINT fk_template
        FOREIGN KEY (template_id)
        REFERENCES Template(id)
);

alter table template add constraint fk_primary_version_id
    FOREIGN KEY (primary_version_id)
    REFERENCES Template_Version(id);

CREATE TABLE Workflow_step_template (
   id bigint PRIMARY KEY NOT NULL GENERATED ALWAYS AS IDENTITY,
    workflow_step_id bigint NOT NULL,
    template_id bigint NOT NULL,
    created TIMESTAMP,
    modified TIMESTAMP,
    CONSTRAINT fk_workflow_step_id FOREIGN KEY (workflow_step_id) REFERENCES Workflow_step(id),
    CONSTRAINT fk_template_id FOREIGN KEY (template_id) REFERENCES Template(id)
);