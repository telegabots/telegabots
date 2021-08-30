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
    FOREIGN KEY (block_id) REFERENCES blocks (id)
);
