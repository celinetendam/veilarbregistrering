ALTER TABLE FORMIDLINGSGRUPPE add FORMIDLINGSGRUPPE_LEST TIMESTAMP NOT NULL;
ALTER TABLE FORMIDLINGSGRUPPE add PERSON_ID VARCHAR(8) NOT NULL;
ALTER TABLE FORMIDLINGSGRUPPE add FORR_FORMIDLINGSGRUPPE_ENDRET TIMESTAMP;
ALTER TABLE FORMIDLINGSGRUPPE add FORR_FORMIDLINGSGRUPPE VARCHAR(5);