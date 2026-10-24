-- Socle minimal du projet 01 (core banking) lorsque celui-ci n'est pas présent.
-- Ces tables sont des versions réduites : elles ne contiennent que les colonnes
-- nécessaires au module e-KYC et au calcul de l'historique de remboursement.

CREATE TABLE agence (
  id BIGSERIAL PRIMARY KEY,
  code VARCHAR(20) NOT NULL UNIQUE,
  nom VARCHAR(150) NOT NULL,
  cree_le TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE utilisateur (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(150) NOT NULL UNIQUE,
  mot_de_passe_hash VARCHAR(100) NOT NULL,
  nom VARCHAR(100) NOT NULL,
  prenom VARCHAR(100) NOT NULL,
  role VARCHAR(20) NOT NULL
    CHECK (role IN ('AGENT_ACCUEIL','ANALYSTE_KYC','SUPERVISEUR','ADMIN')),
  agence_id BIGINT REFERENCES agence(id),
  actif BOOLEAN NOT NULL DEFAULT TRUE,
  cree_le TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE client (
  id BIGSERIAL PRIMARY KEY,
  agence_id BIGINT NOT NULL REFERENCES agence(id),
  nom VARCHAR(100) NOT NULL,
  prenom VARCHAR(100) NOT NULL,
  telephone VARCHAR(30),
  email VARCHAR(150),
  cree_le TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE credit (
  id BIGSERIAL PRIMARY KEY,
  client_id BIGINT NOT NULL REFERENCES client(id),
  montant NUMERIC(18,0) NOT NULL,
  statut VARCHAR(20) NOT NULL DEFAULT 'EN_COURS',
  date_debut DATE,
  cree_le TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE echeance (
  id BIGSERIAL PRIMARY KEY,
  credit_id BIGINT NOT NULL REFERENCES credit(id),
  montant NUMERIC(18,0) NOT NULL,
  date_echeance DATE NOT NULL,
  date_paiement DATE,
  statut VARCHAR(20) NOT NULL DEFAULT 'A_PAYER',
  cree_le TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_credit_client ON credit(client_id);
CREATE INDEX idx_echeance_credit ON echeance(credit_id);
