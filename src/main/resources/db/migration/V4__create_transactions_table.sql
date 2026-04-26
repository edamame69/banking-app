CREATE TABLE transactions (
                              id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
                              account_id          UUID            NOT NULL REFERENCES accounts(id),
                              type                VARCHAR(10)     NOT NULL CHECK (type IN ('DEBIT', 'CREDIT')),
                              amount              NUMERIC(19, 4)  NOT NULL CHECK (amount > 0),
                              balance_after       NUMERIC(19, 4)  NOT NULL,
                              related_account_id  VARCHAR(50)     NOT NULL,
                              description         VARCHAR(255)    NOT NULL,
                              reference_number    VARCHAR(64)     NOT NULL UNIQUE,
                              created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transactions_account_id ON transactions(account_id);
CREATE INDEX idx_transactions_created_at ON transactions(created_at DESC);
CREATE INDEX idx_transactions_reference_number ON transactions(reference_number);