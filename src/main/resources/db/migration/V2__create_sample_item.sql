CREATE TABLE IF NOT EXISTS sample_item (
    id                 UUID           PRIMARY KEY,
    name               VARCHAR(100)   NOT NULL UNIQUE,
    description        VARCHAR(500),
    quantity           INTEGER        NOT NULL CHECK (quantity >= 0),
    price              NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
    created_by         VARCHAR(255)   NOT NULL,
    created_date       TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_by   VARCHAR(255),
    last_modified_date TIMESTAMP WITH TIME ZONE
);
