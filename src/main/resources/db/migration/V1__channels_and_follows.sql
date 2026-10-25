-- Jeden kanał na konto (owner_id = sub z JWT identity). Handle zapisany małymi literami, bez "@".
CREATE TABLE channels (
    id                  uuid PRIMARY KEY,
    owner_id            uuid          NOT NULL UNIQUE,
    handle              varchar(30)   NOT NULL UNIQUE,
    name                varchar(80)   NOT NULL,
    description         varchar(2000) NOT NULL DEFAULT '',
    avatar_url          varchar(500),
    banner_url          varchar(500),
    -- Dane kontaktowe i "O nas"; puste = nie podano.
    contact_phone       varchar(20),
    contact_email       varchar(254),
    address             varchar(200),
    opening_hours       varchar(200),
    -- Dane firmy (PUT /business-details): wszystkie trzy albo żadne. NIP jako 10 cyfr.
    company_name        varchar(200),
    nip                 varchar(10),
    registered_address  varchar(300),
    verification_status varchar(10)   NOT NULL DEFAULT 'NONE',
    created_at          timestamptz   NOT NULL,
    updated_at          timestamptz   NOT NULL
);

CREATE TABLE follows (
    user_id          uuid        NOT NULL,
    channel_id       uuid        NOT NULL REFERENCES channels (id) ON DELETE CASCADE,
    notifications_on boolean     NOT NULL DEFAULT false,
    created_at       timestamptz NOT NULL,
    PRIMARY KEY (user_id, channel_id)
);

CREATE INDEX follows_channel_id_idx ON follows (channel_id);
