package com.sfd.kyc.kyc.service;

/** Abstraction du stockage binaire (SEC-07), remplaçable par un stockage objet. */
public interface FileStorage {

    void enregistrer(String cle, byte[] contenu);

    byte[] lire(String cle);
}
