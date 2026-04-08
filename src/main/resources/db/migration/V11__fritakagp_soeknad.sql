DROP TABLE fritakagp_dialog;
CREATE TABLE fritakagp_soeknad
(
    dialog_id    UUID PRIMARY KEY,
    soeknad_id   UUID UNIQUE NOT NULL,
    soeknad_type VARCHAR(50) NOT NULL,
    fnr          VARCHAR(50) NOT NULL,
    orgnr        VARCHAR(50) NOT NULL,
    opprettet    TIMESTAMP   NOT NULL DEFAULT now()
);