CREATE TABLE forespoersel
(
    id                   UUID        NOT NULL PRIMARY KEY,
    forespoersel_id      UUID        NOT NULL,
    vedtaksperiode_id    UUID        NOT NULL,
    status               TEXT        NOT NULL,
    forespoersel_status  TEXT        NOT NULL,
    opprettet            TIMESTAMP   NOT NULL DEFAULT now(),
    UNIQUE (forespoersel_id, forespoersel_status)
);