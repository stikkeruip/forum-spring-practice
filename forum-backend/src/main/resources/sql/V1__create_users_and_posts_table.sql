CREATE TABLE users
(
    name     VARCHAR(255) PRIMARY KEY,
    password VARCHAR(255) NOT NULL
);

CREATE TABLE posts
(
    id            BIGSERIAL PRIMARY KEY,
    user_name     VARCHAR(255) NOT NULL,
    creation_date TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_date  TIMESTAMPTZ,
    date_deleted  TIMESTAMPTZ,
    title         VARCHAR(255),
    content       TEXT
);