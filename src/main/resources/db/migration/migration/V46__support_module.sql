CREATE TABLE support_ticket (
    id UUID PRIMARY KEY,

    user_account_id UUID NOT NULL,

    assigned_employee_id UUID NULL,

    subject VARCHAR(200) NOT NULL,

    category VARCHAR(50) NOT NULL,

    priority VARCHAR(30) NOT NULL,

    status VARCHAR(30) NOT NULL,

    created_at TIMESTAMP NOT NULL,

    updated_at TIMESTAMP NOT NULL,

    resolved_at TIMESTAMP NULL,

    closed_at TIMESTAMP NULL
);

CREATE TABLE support_ticket_reply (
    id UUID PRIMARY KEY,

    ticket_id UUID NOT NULL,

    sender_user_id UUID NULL,

    sender_employee_id UUID NULL,

    message TEXT NOT NULL,

    media_file_id UUID NULL,

    internal_note BOOLEAN NOT NULL,

    created_at TIMESTAMP NOT NULL
);