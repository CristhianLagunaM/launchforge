ALTER TABLE orders
    ADD COLUMN requirement_description TEXT,
    ADD COLUMN project_objective TEXT,
    ADD COLUMN contact_email VARCHAR(180),
    ADD COLUMN contact_phone VARCHAR(40),
    ADD COLUMN desired_delivery_date DATE,
    ADD COLUMN references_url TEXT;