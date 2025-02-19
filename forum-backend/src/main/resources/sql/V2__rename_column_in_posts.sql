ALTER TABLE posts
    RENAME COLUMN date_deleted TO deleted_date;
ALTER TABLE posts
    RENAME COLUMN creation_date TO created_date;