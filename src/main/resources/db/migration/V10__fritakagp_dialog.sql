CREATE TABLE fritakagp_dialog
(
    id            BIGSERIAL PRIMARY KEY,
    dialog_id     UUID UNIQUE NOT NULL,
    dokument_id   UUID UNIQUE NOT NULL,
    dokument_type TEXT        NOT NULL,
    fnr           TEXT        NOT NULL,
    opprettet     TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE INDEX dokumnet_id_index ON fritakagp_dialog (dokument_id);