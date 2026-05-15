ALTER TABLE user_profile
ADD CONSTRAINT uk_user_profile_user_account UNIQUE (user_account_id);

ALTER TABLE parent_profile
ADD CONSTRAINT uk_parent_profile_user_account UNIQUE (user_account_id);

ALTER TABLE user_preferences
ADD CONSTRAINT uk_user_preferences_profile UNIQUE (user_profile_id);