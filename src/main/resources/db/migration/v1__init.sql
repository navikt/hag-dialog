CREATE TABLE dialog
(
    id            UUID UNIQUE NOT NULL,
    sykmelding_id UUID UNIQUE NOT NULL,
    opprettet     TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE INDEX sykmelding_id_index ON dialog (sykmelding_id);

CREATE TABLE transmission
(
    id                   UUID UNIQUE NOT NULL,
    dialog_id            UUID UNIQUE NOT NULL,
    dokument_id          UUID UNIQUE NOT NULL,
    dokument_type        TEXT        NOT NULL,
    related_transmission UUID,
    opprettet            TIMESTAMP   NOT NULL DEFAULT now(),
    FOREIGN KEY (dialog_id) REFERENCES dialog (id)
);
CREATE INDEX dokument_id_index ON transmission (dokument_id);