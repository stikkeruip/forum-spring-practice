CREATE TYPE friendship_status AS ENUM (
    'PENDING',
    'ACCEPTED', 
    'DECLINED',
    'BLOCKED'
);

CREATE TABLE friendships (
    id BIGSERIAL PRIMARY KEY,
    requester_username VARCHAR(255) NOT NULL,
    addressee_username VARCHAR(255) NOT NULL,
    status friendship_status NOT NULL DEFAULT 'PENDING',
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_friendship_requester FOREIGN KEY (requester_username) REFERENCES users(name),
    CONSTRAINT fk_friendship_addressee FOREIGN KEY (addressee_username) REFERENCES users(name),
    CONSTRAINT unique_friendship_pair UNIQUE (requester_username, addressee_username),
    CONSTRAINT no_self_friendship CHECK (requester_username != addressee_username)
);

CREATE INDEX idx_friendships_requester ON friendships(requester_username);
CREATE INDEX idx_friendships_addressee ON friendships(addressee_username);
CREATE INDEX idx_friendships_status ON friendships(status);
CREATE INDEX idx_friendships_requester_status ON friendships(requester_username, status);
CREATE INDEX idx_friendships_addressee_status ON friendships(addressee_username, status);