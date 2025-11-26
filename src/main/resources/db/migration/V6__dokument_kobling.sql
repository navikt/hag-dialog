CREATE TABLE sykmelding
(
    sykmelding_id UUID UNIQUE NOT NULL PRIMARY KEY,
    status        TEXT        NOT NULL,
    data          JSONB       NOT NULL,
    opprettet     TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE TABLE sykepengesoeknad
(
    soeknad_id    UUID UNIQUE NOT NULL PRIMARY KEY,
    sykmelding_id UUID        NOT NULL,
    orgnr         TEXT        NOT NULL,
    status        TEXT        NOT NULL,
    opprettet     TIMESTAMP   NOT NULL DEFAULT now(),
    UNIQUE (soeknad_id, sykmelding_id)
);

CREATE INDEX soeknad_sykmelding_id_index ON sykepengesoeknad (sykmelding_id);