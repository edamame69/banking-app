CREATE TABLE accounts (
                          id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
                          account_number  VARCHAR(20)     NOT NULL UNIQUE,
                          balance         NUMERIC(19, 4)  NOT NULL DEFAULT 0,
                          currency        CHAR(3)         NOT NULL,
                          status          VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE',
                          created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

                          CONSTRAINT chk_balance_non_negative CHECK (balance >= 0)
);

CREATE INDEX idx_accounts_account_number ON accounts(account_number);