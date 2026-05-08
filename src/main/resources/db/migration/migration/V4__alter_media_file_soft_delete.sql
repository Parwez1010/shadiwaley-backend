ALTER TABLE media_file
ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE media_file
ADD COLUMN deleted_at TIMESTAMPTZ;

CREATE INDEX idx_media_is_deleted ON media_file(is_deleted);