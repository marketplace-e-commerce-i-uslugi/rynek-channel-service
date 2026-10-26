-- Liczba opublikowanych ofert kanału. Ustawia ją catalog-service (PUT /internal/channels/{id}/offers-count),
-- dopóki nie ma zdarzeń offer.published / offer.unpublished.
ALTER TABLE channels ADD COLUMN offers_count INTEGER NOT NULL DEFAULT 0;
