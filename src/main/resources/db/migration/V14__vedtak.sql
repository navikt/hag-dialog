CREATE TABLE vedtak
(
    vedtak_id           UUID UNIQUE NOT NULL PRIMARY KEY,
    sykmelding_id       UUID        NOT NULL,
    inntektsmelding_id  UUID        NOT NULL,
    orgnr               TEXT        NOT NULL,
    status              TEXT        NOT NULL,
    opprettet           TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE INDEX vedtak_sykmelding_id_index ON vedtak (sykmelding_id);
CREATE INDEX vedtak_inntektsmelding_id_index ON vedtak (inntektsmelding_id);
