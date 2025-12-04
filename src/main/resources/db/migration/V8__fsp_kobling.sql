CREATE TABLE forespoersel
(
    forespoersel_id      UUID        NOT NULL PRIMARY KEY,
    vedtaksperiode_id    UUID        NOT NULL,
    status               TEXT        NOT NULL,
    forespoersel_status  TEXT        NOT NULL,
    opprettet            TIMESTAMP   NOT NULL DEFAULT now(),
    UNIQUE (vedtaksperiode_id, forespoersel_status)
);