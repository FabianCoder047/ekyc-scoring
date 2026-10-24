CREATE TABLE dossier_kyc (
  id BIGSERIAL PRIMARY KEY,
  client_id BIGINT NOT NULL REFERENCES client(id),
  agence_id BIGINT NOT NULL REFERENCES agence(id),
  version INT NOT NULL DEFAULT 1,
  statut VARCHAR(10) NOT NULL DEFAULT 'BROUILLON'
    CHECK (statut IN ('BROUILLON','SOUMIS','EN_REVUE','VALIDE','REJETE')),
  type_piece VARCHAR(20) NOT NULL DEFAULT 'CNI',
  numero_piece VARCHAR(40) NOT NULL,
  date_expiration_piece DATE NOT NULL,
  revenu_mensuel NUMERIC(18,0),
  charges_mensuelles NUMERIC(18,0),
  anciennete_activite_mois INT,
  nb_personnes_charge INT,
  niveau_risque VARCHAR(10)
    CHECK (niveau_risque IS NULL OR niveau_risque IN ('FAIBLE','MOYEN','ELEVE')),
  cree_par BIGINT NOT NULL REFERENCES utilisateur(id),
  soumis_par BIGINT REFERENCES utilisateur(id),
  soumis_le TIMESTAMPTZ,
  assigne_a BIGINT REFERENCES utilisateur(id),
  decide_par BIGINT REFERENCES utilisateur(id),
  decide_le TIMESTAMPTZ,
  motif_rejet VARCHAR(500),
  cree_le TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (client_id, version)
);

CREATE INDEX idx_dossier_statut ON dossier_kyc(statut);
CREATE INDEX idx_dossier_agence ON dossier_kyc(agence_id);
CREATE INDEX idx_dossier_assigne ON dossier_kyc(assigne_a);

CREATE TABLE document_kyc (
  id BIGSERIAL PRIMARY KEY,
  dossier_id BIGINT NOT NULL REFERENCES dossier_kyc(id),
  type VARCHAR(25) NOT NULL CHECK (type IN ('RECTO_PIECE','VERSO_PIECE','JUSTIFICATIF_DOMICILE')),
  nom_original VARCHAR(200) NOT NULL,
  cle_stockage VARCHAR(80) NOT NULL UNIQUE,
  mime_type VARCHAR(50) NOT NULL,
  taille_octets BIGINT NOT NULL,
  sha256 CHAR(64) NOT NULL,
  iv BYTEA NOT NULL,
  depose_par BIGINT NOT NULL REFERENCES utilisateur(id),
  depose_le TIMESTAMPTZ NOT NULL DEFAULT now(),
  remplace_par BIGINT REFERENCES document_kyc(id)
);

CREATE INDEX idx_document_dossier ON document_kyc(dossier_id);
CREATE INDEX idx_document_dossier_type ON document_kyc(dossier_id, type);

CREATE TABLE decision_kyc (
  id BIGSERIAL PRIMARY KEY,
  dossier_id BIGINT NOT NULL REFERENCES dossier_kyc(id),
  etape VARCHAR(20) NOT NULL
    CHECK (etape IN ('PRISE_EN_CHARGE','ANALYSE','VALIDATION')),
  decision VARCHAR(15) NOT NULL
    CHECK (decision IN ('ASSIGNE','RECOMMANDE','VALIDE','REJETE')),
  commentaire VARCHAR(500),
  decideur_id BIGINT NOT NULL REFERENCES utilisateur(id),
  cree_le TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_decision_dossier ON decision_kyc(dossier_id);

CREATE TABLE audit_log (
  id BIGSERIAL PRIMARY KEY,
  horodatage TIMESTAMPTZ NOT NULL DEFAULT now(),
  utilisateur_id BIGINT,
  role VARCHAR(20),
  action VARCHAR(40) NOT NULL,
  entite_type VARCHAR(30) NOT NULL,
  entite_id BIGINT,
  avant JSONB,
  apres JSONB,
  adresse_ip VARCHAR(45),
  user_agent VARCHAR(200),
  hash_precedent CHAR(64) NOT NULL,
  hash CHAR(64) NOT NULL
);

CREATE INDEX idx_audit_horodatage ON audit_log(horodatage);
CREATE INDEX idx_audit_action ON audit_log(action);
CREATE INDEX idx_audit_entite ON audit_log(entite_type, entite_id);

CREATE TABLE critere_scoring (
  id BIGSERIAL PRIMARY KEY,
  critere VARCHAR(30) NOT NULL
    CHECK (critere IN ('RATIO_ENDETTEMENT','ANCIENNETE_ACTIVITE','REVENU_MENSUEL','HISTORIQUE_REMBOURSEMENT')),
  borne_min NUMERIC(18,4),
  borne_max NUMERIC(18,4),
  points INT NOT NULL CHECK (points BETWEEN 0 AND 25),
  libelle VARCHAR(120) NOT NULL,
  actif BOOLEAN NOT NULL DEFAULT TRUE,
  CHECK (borne_min IS NULL OR borne_max IS NULL OR borne_min <= borne_max)
);

CREATE INDEX idx_critere_critere ON critere_scoring(critere, actif);

CREATE TABLE resultat_scoring (
  id BIGSERIAL PRIMARY KEY,
  dossier_id BIGINT NOT NULL REFERENCES dossier_kyc(id),
  score INT NOT NULL CHECK (score BETWEEN 0 AND 100),
  palier VARCHAR(15) NOT NULL
    CHECK (palier IN ('FAVORABLE','A_ETUDIER','DEFAVORABLE')),
  detail JSONB NOT NULL,
  calcule_par BIGINT NOT NULL REFERENCES utilisateur(id),
  calcule_le TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_resultat_dossier ON resultat_scoring(dossier_id);

CREATE TABLE tentative_connexion (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(150) NOT NULL,
  adresse_ip VARCHAR(45),
  succes BOOLEAN NOT NULL,
  horodatage TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_tentative_email_date ON tentative_connexion(email, horodatage);

-- Table additionnelle nécessaire à la rotation des refresh tokens (SEC-02).
CREATE TABLE refresh_token (
  id BIGSERIAL PRIMARY KEY,
  utilisateur_id BIGINT NOT NULL REFERENCES utilisateur(id),
  token_hash CHAR(64) NOT NULL UNIQUE,
  expire_le TIMESTAMPTZ NOT NULL,
  revoque_le TIMESTAMPTZ,
  remplace_par BIGINT REFERENCES refresh_token(id),
  cree_le TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_utilisateur ON refresh_token(utilisateur_id);
