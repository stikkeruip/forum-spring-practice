-- Add parent comment support for nested replies
ALTER TABLE comments 
ADD COLUMN parent_comment_id BIGINT NULL,
ADD CONSTRAINT fk_parent_comment 
    FOREIGN KEY (parent_comment_id) 
    REFERENCES comments (id);

-- Create index for better performance when querying replies
CREATE INDEX idx_comments_parent_comment_id ON comments (parent_comment_id);