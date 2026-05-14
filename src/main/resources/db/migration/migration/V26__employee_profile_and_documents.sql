CREATE TABLE employee_profile (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    employee_account_id UUID NOT NULL UNIQUE
        REFERENCES employee_account(id) ON DELETE CASCADE,

    first_name VARCHAR(100),
    last_name VARCHAR(100),
    gender VARCHAR(30),
    date_of_birth DATE,

    alternate_phone VARCHAR(20),
    personal_email VARCHAR(150),

    address VARCHAR(500),
    city VARCHAR(100),
    state VARCHAR(100),
    district VARCHAR(100),
    pincode VARCHAR(20),

    emergency_contact_name VARCHAR(150),
    emergency_contact_phone VARCHAR(20),

    employee_code VARCHAR(50) UNIQUE,
    designation VARCHAR(120),
    employment_type VARCHAR(50),
    joining_date DATE,
    work_mode VARCHAR(50),
    reporting_manager_id UUID REFERENCES employee_account(id),

    aadhaar_number VARCHAR(20),

    highest_qualification VARCHAR(150),
    college_name VARCHAR(200),
    total_experience_years NUMERIC(4,1),
    previous_company VARCHAR(200),

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_employee_profile_employee
ON employee_profile(employee_account_id);

CREATE INDEX idx_employee_profile_code
ON employee_profile(employee_code);

CREATE TABLE employee_document (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    employee_account_id UUID NOT NULL
        REFERENCES employee_account(id) ON DELETE CASCADE,

    document_type VARCHAR(60) NOT NULL,

    original_file_name VARCHAR(255) NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    content_type VARCHAR(120),
    size_bytes BIGINT,

    verification_status VARCHAR(40) NOT NULL DEFAULT 'PENDING_REVIEW',

    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    verified_by_employee_id UUID REFERENCES employee_account(id),
    verified_at TIMESTAMPTZ,

    rejected_reason VARCHAR(500),

    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_employee_document_employee
ON employee_document(employee_account_id);

CREATE INDEX idx_employee_document_type
ON employee_document(document_type);

CREATE INDEX idx_employee_document_status
ON employee_document(verification_status);