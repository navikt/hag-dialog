CREATE TABLE fritakagp_krav
(
    transmission_id UUID PRIMARY KEY,
    dialog_id       UUID        NOT NULL,
    krav_id         UUID        NOT NULL,
    krav_type       VARCHAR(50) NOT NULL,
    fnr             VARCHAR(50) NOT NULL,
    orgnr           VARCHAR(50) NOT NULL,
    opprettet       TIMESTAMP   NOT NULL DEFAULT now()
);
