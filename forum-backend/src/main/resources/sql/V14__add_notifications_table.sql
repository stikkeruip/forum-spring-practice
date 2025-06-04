CREATE TYPE notification_type AS ENUM (
    'POST_LIKED',
    'COMMENT_LIKED',
    'POST_COMMENTED',
    'COMMENT_REPLIED',
    'POST_DELETED_BY_MODERATOR',
    'COMMENT_DELETED_BY_MODERATOR'
);

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    recipient_username VARCHAR(255) NOT NULL,
    actor_username VARCHAR(255) NOT NULL,
    type notification_type NOT NULL,
    target_post_id BIGINT,
    target_comment_id BIGINT,
    message TEXT NOT NULL,
    read BOOLEAN DEFAULT FALSE,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_recipient_username FOREIGN KEY (recipient_username) REFERENCES users(name),
    CONSTRAINT fk_actor_username FOREIGN KEY (actor_username) REFERENCES users(name),
    CONSTRAINT fk_target_post FOREIGN KEY (target_post_id) REFERENCES posts(id),
    CONSTRAINT fk_target_comment FOREIGN KEY (target_comment_id) REFERENCES comments(id)
);

CREATE INDEX idx_notifications_recipient_read ON notifications(recipient_username, read);
CREATE INDEX idx_notifications_created_date ON notifications(created_date DESC);