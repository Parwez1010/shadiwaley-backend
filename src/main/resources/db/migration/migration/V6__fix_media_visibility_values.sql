UPDATE media_file
SET visibility = 'CHAT_SHAREABLE'
WHERE visibility = 'PUBLIC_AFTER_APPROVAL';

UPDATE media_file
SET visibility = 'INTERNAL_ONLY'
WHERE visibility = 'CRM_VERIFIER_ONLY';