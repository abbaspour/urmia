-- users
CREATE ROLE uadmin WITH LOGIN ENCRYPTED PASSWORD 'uadmin' CREATEDB;

-- database
CREATE DATABASE udb01 WITH OWNER uadmin ENCODING 'UTF8';
GRANT ALL PRIVILEGES ON DATABASE udb01 TO uadmin;

-- types
CREATE TYPE object_type AS ENUM ('file', 'directory');
CREATE TYPE namespace AS ENUM ('index', 'jobs', 'public', 'reports', 'stor');

-- tables

/*
CREATE TABLE namespace_details (
  name namespace    NOT NULL,
  etag CHARACTER(2) NOT NULL,
  CONSTRAINT ns_details_pkey PRIMARY KEY (name)
);

ALTER TABLE namespace_details OWNER TO uadmin;

insert into namespace_details VALUES
('index', '10'), ('jobs', '20'), ('public', '30'), ('reports', '40'), ('stor', 50);
*/

-- owners
CREATE TABLE owners (
  username VARCHAR(50) NOT NULL,
  key_id   VARCHAR(50) NOT NULL,  -- todo: rename to fingerprint
  --etag     CHAR(8)     NOT NULL,
  CONSTRAINT owners_pkey PRIMARY KEY (username)
);
ALTER TABLE owners OWNER TO uadmin;


-- objects
CREATE TABLE objects (
  owner      TEXT                     NOT NULL REFERENCES owners (username),
  ns         namespace                NOT NULL,
  parent     TEXT                     NOT NULL,
  name       TEXT                     NOT NULL,
  type       object_type              NOT NULL,
  size       BIGINT                   NOT NULL DEFAULT 0,
  mtime      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  md5        CHARACTER(25),
  etag       CHARACTER VARYING,
  durability SMALLINT                 NOT NULL,
  deleted    BOOLEAN                  NOT NULL DEFAULT FALSE,
  CONSTRAINT objects_pkey PRIMARY KEY (owner, ns, parent, name, deleted)
);

ALTER TABLE objects OWNER TO uadmin;

CREATE INDEX objects_size_idx ON objects USING BTREE (size);
CREATE INDEX objects_mtime_idx ON objects USING BTREE (mtime);
CREATE UNIQUE INDEX objects_etag_uidx ON objects USING BTREE (etag);

-- storage
/*
CREATE TABLE availability_zones (
  id      SMALLINT NOT NULL,
  details TEXT,
  CONSTRAINT az_pkey PRIMARY KEY (id)
);

ALTER TABLE availability_zones OWNER TO uadmin;

insert into availability_zones values (1, 'default');

CREATE TABLE storage_nodes (
  location TEXT              NOT NULL,
  az       SMALLINT          NOT NULL REFERENCES availability_zones (id),
  hostname CHARACTER VARYING NOT NULL,
  port     INT               NOT NULL DEFAULT (8081),
  CONSTRAINT st_node_pkey PRIMARY KEY (location)
);

ALTER TABLE storage_nodes OWNER TO uadmin;
*/

CREATE TABLE storage (
  etag     TEXT                     NOT NULL, --REFERENCES objects(etag), -- why not? because we insert this before objects
  location TEXT                     NOT NULL, -- REFERENCES storage_nodes (location),
  mtime    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
  --CONSTRAINT storage_pkey PRIMARY KEY (etag)
);

CREATE INDEX storage_etag_idx ON storage USING BTREE (etag);

ALTER TABLE storage OWNER TO uadmin;
