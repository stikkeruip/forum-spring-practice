-- Add deleted_by column to posts table to track who deleted the post
ALTER TABLE posts ADD COLUMN deleted_by VARCHAR(255);

-- Add foreign key constraint to reference users table
ALTER TABLE posts ADD CONSTRAINT fk_posts_deleted_by 
    FOREIGN KEY (deleted_by) REFERENCES users(name);