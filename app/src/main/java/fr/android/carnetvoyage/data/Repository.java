package fr.android.carnetvoyage.data;

import java.util.List;

import fr.android.carnetvoyage.model.Entry;

/**
 * Contrat commun à un "endroit où sont stockées les entrées".
 * Implémenté à la fois par LocalRepository (SQLite) et RemoteRepository (MySQL),
 * qui savent tous les deux ajouter une entrée et lire toutes les entrées.
 */
public interface Repository {
    long add(Entry entry);   // insère et renvoie l'id généré
    List<Entry> getAll();    // lit toutes les entrées
}
