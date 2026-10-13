CREATE TABLE notifikasjon
(
    notifikasjon_id UUID UNIQUE NOT NULL PRIMARY KEY,
    dokument_id     UUID        NOT NULL,
    tjeneste        TEXT        NOT NULL,
    status          TEXT        NOT NULL,
    opprettet       TIMESTAMP   NOT NULL DEFAULT now(),
    CONSTRAINT notifikasjon_dokument_unik UNIQUE (dokument_id, tjeneste)
);

CREATE INDEX notifikasjon_status_index ON notifikasjon (status);
