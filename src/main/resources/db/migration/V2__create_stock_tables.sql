-- Stock master data table
CREATE TABLE stocks (
    id BIGSERIAL PRIMARY KEY,
    ticker VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    exchange VARCHAR(50) NOT NULL,
    sector VARCHAR(100),
    industry VARCHAR(100),
    currency VARCHAR(10) NOT NULL,
    current_price DECIMAL(19,4),
    last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_stock_ticker ON stocks(ticker);

-- Holdings/Positions table for user investments
CREATE TABLE positions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    stock_id BIGINT NOT NULL REFERENCES stocks(id),
    quantity DECIMAL(19,6) NOT NULL,
    average_cost DECIMAL(19,4) NOT NULL,
    first_purchased TIMESTAMP NOT NULL,
    last_transaction TIMESTAMP NOT NULL,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT unique_user_stock UNIQUE (user_id, stock_id)
);

CREATE INDEX idx_position_user ON positions(user_id);
CREATE INDEX idx_position_stock ON positions(stock_id);

-- Transactions table for detailed purchase/sell history
CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    stock_id BIGINT NOT NULL REFERENCES stocks(id),
    position_id BIGINT NOT NULL REFERENCES positions(id),
    transaction_type VARCHAR(10) NOT NULL CHECK (transaction_type IN ('BUY', 'SELL')),
    quantity DECIMAL(19,6) NOT NULL,
    price DECIMAL(19,4) NOT NULL,
    fee DECIMAL(19,4) NOT NULL DEFAULT 0.0,
    transaction_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_transaction_user ON transactions(user_id);
CREATE INDEX idx_transaction_stock ON transactions(stock_id);
CREATE INDEX idx_transaction_position ON transactions(position_id);
CREATE INDEX idx_transaction_date ON transactions(transaction_date);

-- Update trigger for positions table
CREATE TRIGGER update_position_updated_at
    BEFORE UPDATE ON positions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();