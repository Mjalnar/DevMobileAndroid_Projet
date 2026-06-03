package fr.android.carnetvoyage.data;

import java.util.List;

import fr.android.carnetvoyage.model.Entry;

public interface Repository {
    long add(Entry entry);        // insère et renvoie l'id généré
    List<Entry> getAll();         // lit toutes les entrées
    Entry getById(long id);       // lit une entrée précise
    boolean update(Entry entry);  // met à jour
    boolean delete(long id);      // supprime
}