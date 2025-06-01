-- Add likes and dislikes columns to posts table
ALTER TABLE posts ADD COLUMN likes INT DEFAULT 0 NOT NULL;
ALTER TABLE posts ADD COLUMN dislikes INT DEFAULT 0 NOT NULL;

-- Add likes and dislikes columns to comments table
ALTER TABLE comments ADD COLUMN likes INT DEFAULT 0 NOT NULL;
ALTER TABLE comments ADD COLUMN dislikes INT DEFAULT 0 NOT NULL;

-- Create table to track post likes/dislikes by users
CREATE TABLE post_reactions (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_name VARCHAR(255) NOT NULL,
    reaction_type VARCHAR(10) NOT NULL, -- 'LIKE' or 'DISLIKE'
    created_date TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_post_reaction_post FOREIGN KEY (post_id) REFERENCES posts(id),
    CONSTRAINT fk_post_reaction_user FOREIGN KEY (user_name) REFERENCES users(name),
    UNIQUE (post_id, user_name)
);

-- Create table to track comment likes/dislikes by users
CREATE TABLE comment_reactions (
    id BIGSERIAL PRIMARY KEY,
    comment_id BIGINT NOT NULL,
    user_name VARCHAR(255) NOT NULL,
    reaction_type VARCHAR(10) NOT NULL, -- 'LIKE' or 'DISLIKE'
    created_date TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_comment_reaction_comment FOREIGN KEY (comment_id) REFERENCES comments(id),
    CONSTRAINT fk_comment_reaction_user FOREIGN KEY (user_name) REFERENCES users(name),
    UNIQUE (comment_id, user_name)
);
