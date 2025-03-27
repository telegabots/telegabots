-- Sqlite schema for sqlite-based implementation of StateDbProvider

CREATE TABLE blocks
(
    id           INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    message_id   INTEGER NOT NULL,
    user_id      INTEGER NOT NULL,
    message_type TEXT(10) NOT NULL,
    created_at   INTEGER NOT NULL
);

CREATE TABLE pages
(
    id           INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    block_id     INTEGER NOT NULL,
    handler      TEXT(100) NOT NULL,
    command_defs TEXT,
    created_at   INTEGER NOT NULL,
    updated_at   INTEGER NOT NULL,
    FOREIGN KEY (block_id) REFERENCES blocks (id) ON DELETE CASCADE
);

CREATE TABLE local_states
(
    id           INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    page_id      INTEGER NOT NULL,
    state_def    TEXT,
    FOREIGN KEY (page_id) REFERENCES pages (id) ON DELETE CASCADE
);

CREATE TABLE shared_states
(
    id           INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    block_id     INTEGER NOT NULL,
    state_def    TEXT,
    FOREIGN KEY (block_id) REFERENCES blocks (id) ON DELETE CASCADE
);

CREATE TABLE user_states
(
    id           INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    user_id      INTEGER NOT NULL,
    state_def    TEXT
);

CREATE TABLE global_states
(
    id           INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    state_def    TEXT
);

CREATE TABLE entity_types
(
    id           INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    type_name    TEXT(255) NOT NULL UNIQUE,
    created_at   INTEGER NOT NULL
);

CREATE TABLE user_entities
(
    id              INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    user_id         INTEGER NOT NULL,
    entity_types_id INTEGER NOT NULL,
    entity          TEXT NOT NULL,
    unique1         TEXT(255) NULL,
    index1          INTEGER NULL,
    updated_at      INTEGER NOT NULL,
    UNIQUE(user_id, unique1),
    FOREIGN KEY (entity_types_id) REFERENCES entity_types (id) ON DELETE CASCADE
);

CREATE INDEX idx_user_entities_index1 ON user_entities (index1);
