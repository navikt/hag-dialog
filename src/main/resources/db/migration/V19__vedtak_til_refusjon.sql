ALTER TABLE vedtak RENAME TO refusjon;
ALTER TABLE refusjon RENAME COLUMN vedtak_id TO refusjon_utfall_id;
ALTER INDEX vedtak_sykmelding_id_index RENAME TO refusjon_sykmelding_id_index;
ALTER INDEX vedtak_inntektsmelding_id_index RENAME TO refusjon_inntektsmelding_id_index;
