CREATE TABLE comments
(
    id           BIGSERIAL PRIMARY KEY,
    post_id      BIGINT       NOT NULL,
    content      TEXT         NOT NULL,
    user_name    VARCHAR(255) NOT NULL,
    created_date TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_date TIMESTAMPTZ,
    deleted_date TIMESTAMPTZ,
    CONSTRAINT fk_post FOREIGN KEY (post_id) REFERENCES posts (id),
    CONSTRAINT fk_user_name FOREIGN KEY (user_name) REFERENCES users (name)
);

ALTER TABLE posts
    ADD CONSTRAINT fk_user_name
        FOREIGN KEY (user_name)
            REFERENCES users (name)