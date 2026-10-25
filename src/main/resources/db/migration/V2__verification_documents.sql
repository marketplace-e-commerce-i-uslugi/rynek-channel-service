-- Dokumenty zgłoszone do weryfikacji (mediaId z media-service, przeznaczenie KYC_DOCUMENT). Ocenia je admin.
CREATE TABLE verification_documents (
    channel_id   uuid        NOT NULL REFERENCES channels (id) ON DELETE CASCADE,
    media_id     uuid        NOT NULL,
    submitted_at timestamptz NOT NULL,
    PRIMARY KEY (channel_id, media_id)
);
