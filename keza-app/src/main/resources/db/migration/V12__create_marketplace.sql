CREATE TABLE marketplace_listings (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seller_id           UUID NOT NULL REFERENCES users(id),
    investment_id       UUID NOT NULL REFERENCES investments(id),
    campaign_id         UUID NOT NULL REFERENCES campaigns(id),
    shares_listed       BIGINT NOT NULL,
    price_per_share     DECIMAL(15,4) NOT NULL,
    total_price         DECIMAL(15,2) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    company_consent     BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at          TIMESTAMPTZ,
    sold_at             TIMESTAMPTZ,
    buyer_id            UUID REFERENCES users(id),
    seller_fee          DECIMAL(15,2),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version             BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE marketplace_transactions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id      UUID NOT NULL REFERENCES marketplace_listings(id),
    buyer_id        UUID NOT NULL REFERENCES users(id),
    seller_id       UUID NOT NULL REFERENCES users(id),
    shares          BIGINT NOT NULL,
    price_per_share DECIMAL(15,4) NOT NULL,
    total_amount    DECIMAL(15,2) NOT NULL,
    seller_fee      DECIMAL(15,2) NOT NULL,
    net_amount      DECIMAL(15,2) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_marketplace_listings_seller ON marketplace_listings (seller_id);
CREATE INDEX idx_marketplace_listings_campaign ON marketplace_listings (campaign_id);
CREATE INDEX idx_marketplace_listings_status ON marketplace_listings (status);
CREATE INDEX idx_marketplace_transactions_listing ON marketplace_transactions (listing_id);
CREATE INDEX idx_marketplace_transactions_buyer ON marketplace_transactions (buyer_id);
