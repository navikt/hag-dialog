CREATE TABLE dialog
(
    id                       BIGSERIAL PRIMARY KEY,
    dialog_id                UUID UNIQUE NOT NULL,
    sykmelding_id            UUID UNIQUE NOT NULL,
    opprettet                TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE INDEX sykmelding_id_index ON dialog (sykmelding_id);
