CREATE TABLE dialog
(
    id            UUID UNIQUE NOT NULL PRIMARY KEY,
    sykmelding_id UUID UNIQUE NOT NULL,
    opprettet     TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE INDEX sykmelding_id_index ON dialog (sykmelding_id);

CREATE TABLE transmission
(
    id                   UUID UNIQUE NOT NULL PRIMARY KEY,
    dialog_id            UUID NOT NULL,
    dokument_id          UUID NOT NULL,
    dokument_type        VARCHAR(50) NOT NULL,
    related_transmission UUID,
    opprettet            TIMESTAMP   NOT NULL DEFAULT now(),
    FOREIGN KEY (dialog_id) REFERENCES dialog (id)
);
CREATE INDEX dokument_id_index ON transmission (dokument_id);