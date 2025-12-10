CREATE TABLE inntektsmelding
(
    id                     UUID      NOT NULL PRIMARY KEY,
    forespoersel_id        UUID      NOT NULL,
    vedtaksperiode_id      UUID      NOT NULL,
    status                 TEXT      NOT NULL,
    inntektsmelding_status TEXT      NOT NULL,
    innsending_type         TEXT      NOT NULL,
    opprettet              TIMESTAMP NOT NULL DEFAULT now()
);