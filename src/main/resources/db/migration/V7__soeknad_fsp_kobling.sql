CREATE TABLE vedtaksperiode_soeknad
(
    id                UUID UNIQUE NOT NULL PRIMARY KEY,
    vedtaksperiode_id UUID        NOT NULL,
    soeknad_id        UUID        NOT NULL,
    opprettet         TIMESTAMP   NOT NULL DEFAULT now(),
    UNIQUE (vedtaksperiode_id, soeknad_id)
);