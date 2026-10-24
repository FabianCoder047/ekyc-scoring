-- Données de démonstration. Les mots de passe des comptes ci-dessous valent
-- tous « Demo123456! » (hash BCrypt coût 12) et ne doivent jamais être utilisés
-- en production (voir README).

INSERT INTO agence (code, nom) VALUES
  ('LOME-CENTRE', 'Agence de Lomé Centre'),
  ('KARA', 'Agence de Kara')
ON CONFLICT (code) DO NOTHING;

INSERT INTO utilisateur (email, mot_de_passe_hash, nom, prenom, role, agence_id)
SELECT 'agent@sfd.tg', '$2b$12$N7NTPPTUFo9MA0kHFfZBuuF5P4j2yIUlse5T2U8dna40qL3jx/TaG',
       'ADJOVI', 'Kossi', 'AGENT_ACCUEIL', a.id
FROM agence a WHERE a.code = 'LOME-CENTRE'
ON CONFLICT (email) DO NOTHING;

INSERT INTO utilisateur (email, mot_de_passe_hash, nom, prenom, role, agence_id)
SELECT 'analyste@sfd.tg', '$2b$12$QP9cgzmQTvNS2F/nzGVopOcG1yh1B3Iev2eK5JouplOQJn4zl/Wbu',
       'MENSAH', 'Ama', 'ANALYSTE_KYC', a.id
FROM agence a WHERE a.code = 'LOME-CENTRE'
ON CONFLICT (email) DO NOTHING;

INSERT INTO utilisateur (email, mot_de_passe_hash, nom, prenom, role, agence_id)
SELECT 'superviseur@sfd.tg', '$2b$12$ESlOCmr16pe0EZLSbwDP3Ok66N1NqwLPbfqvvkM6cu.8EEqTHUvzm',
       'KOFFI', 'Yao', 'SUPERVISEUR', a.id
FROM agence a WHERE a.code = 'LOME-CENTRE'
ON CONFLICT (email) DO NOTHING;

INSERT INTO utilisateur (email, mot_de_passe_hash, nom, prenom, role, agence_id)
SELECT 'admin@sfd.tg', '$2b$12$g4JsVppGTAf4nrMzlZ36M.6aajhRLV/oUI9yBKN7SlFd.IqPk3lc6',
       'SANTOS', 'Léa', 'ADMIN', a.id
FROM agence a WHERE a.code = 'LOME-CENTRE'
ON CONFLICT (email) DO NOTHING;

INSERT INTO client (agence_id, nom, prenom, telephone)
SELECT a.id, 'ADJOVI', 'Kossi', '+22890000001'
FROM agence a WHERE a.code = 'LOME-CENTRE'
  AND NOT EXISTS (SELECT 1 FROM client c WHERE c.telephone = '+22890000001');

INSERT INTO client (agence_id, nom, prenom, telephone)
SELECT a.id, 'MENSAH', 'Ama', '+22890000002'
FROM agence a WHERE a.code = 'LOME-CENTRE'
  AND NOT EXISTS (SELECT 1 FROM client c WHERE c.telephone = '+22890000002');

INSERT INTO client (agence_id, nom, prenom, telephone)
SELECT a.id, 'KOFFI', 'Yao', '+22890000003'
FROM agence a WHERE a.code = 'KARA'
  AND NOT EXISTS (SELECT 1 FROM client c WHERE c.telephone = '+22890000003');

-- Historique de remboursement : 10 échéances échues, 1 payée en retard (10 %).
INSERT INTO credit (client_id, montant, statut, date_debut)
SELECT c.id, 500000, 'EN_COURS', CURRENT_DATE - INTERVAL '12 months'
FROM client c WHERE c.telephone = '+22890000001'
  AND NOT EXISTS (SELECT 1 FROM credit cr WHERE cr.client_id = c.id);

INSERT INTO echeance (credit_id, montant, date_echeance, date_paiement, statut)
SELECT cr.id, 50000, (CURRENT_DATE - (n || ' months')::interval)::date,
       CASE WHEN n = 3 THEN (CURRENT_DATE - (n || ' months')::interval)::date + 5
            ELSE (CURRENT_DATE - (n || ' months')::interval)::date END,
       'PAYEE'
FROM credit cr
JOIN client c ON c.id = cr.client_id
CROSS JOIN generate_series(1, 10) AS n
WHERE c.telephone = '+22890000001'
  AND NOT EXISTS (SELECT 1 FROM echeance e WHERE e.credit_id = cr.id);

-- Tranches de scoring (exemple paramétrable, voir section 8 du cahier des charges).
INSERT INTO critere_scoring (critere, borne_min, borne_max, points, libelle)
SELECT * FROM (VALUES
  ('RATIO_ENDETTEMENT',        0.0000,     30.0000, 25, 'Endettement ≤ 30 %'),
  ('RATIO_ENDETTEMENT',       30.0001,     50.0000, 15, 'Endettement de 30 à 50 %'),
  ('RATIO_ENDETTEMENT',       50.0001,     NULL::numeric, 0, 'Endettement > 50 %'),
  ('ANCIENNETE_ACTIVITE',      0.0000,     11.0000,  5, 'Activité < 12 mois'),
  ('ANCIENNETE_ACTIVITE',     12.0000,     35.0000, 15, 'Activité de 12 à 36 mois'),
  ('ANCIENNETE_ACTIVITE',     36.0000,     NULL::numeric, 25, 'Activité ≥ 36 mois'),
  ('REVENU_MENSUEL',           0.0000,  99999.9999,  8, 'Revenu < 100 000 FCFA'),
  ('REVENU_MENSUEL',      100000.0000, 299999.9999, 15, 'Revenu de 100 000 à 300 000 FCFA'),
  ('REVENU_MENSUEL',      300000.0000,     NULL::numeric, 25, 'Revenu ≥ 300 000 FCFA'),
  ('HISTORIQUE_REMBOURSEMENT', 0.0000,      0.0000, 25, 'Aucun retard de remboursement'),
  ('HISTORIQUE_REMBOURSEMENT', 0.0001,     20.0000, 15, 'Retards ≤ 20 %'),
  ('HISTORIQUE_REMBOURSEMENT',20.0001,     NULL::numeric, 0, 'Retards > 20 %')
) AS v(critere, borne_min, borne_max, points, libelle)
WHERE NOT EXISTS (SELECT 1 FROM critere_scoring);
