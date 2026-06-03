package fr.android.carnetvoyage.data;

import java.util.ArrayList;
import java.util.List;

import fr.android.carnetvoyage.model.Entry;

/**
 * TEMPORAIRE (Personne A). Faux Repository en mémoire pour développer et tester
 * l'UI (Liste, Carte) AVANT que le LocalRepository SQLite de B existe.
 */
public class StubRepository implements Repository {

    private final List<Entry> entries = new ArrayList<>();
    private long nextId = 1;

    public StubRepository() {
        long now = System.currentTimeMillis();
        entries.add(new Entry(nextId++, "Tour Eiffel",
                "Vue magnifique au coucher du soleil", null,
                48.8584, 2.2945, "Champ de Mars, 75007 Paris", now - 86_400_000L, -1L));
        entries.add(new Entry(nextId++, "Calanque de Sormiou",
                "Randonnée puis baignade", null,
                43.2096, 5.4180, "Marseille", now - 3_600_000L, -1L));
        entries.add(new Entry(nextId++, "Mont Saint-Michel",
                "Marée impressionnante", null,
                48.6361, -1.5115, "Le Mont-Saint-Michel", now, -1L));
    }

    @Override
    public long add(Entry entry) {
        long id = nextId++;
        entry.setId(id);
        entries.add(entry);
        return id;
    }

    @Override
    public List<Entry> getAll() {
        return new ArrayList<>(entries);
    }

    @Override
    public Entry getById(long id) {
        for (Entry entry : entries) {
            if (entry.getId() == id) {
                return entry;
            }
        }
        return null;
    }

    @Override
    public boolean update(Entry entry) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getId() == entry.getId()) {
                entries.set(i, entry);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean delete(long id) {
        return entries.removeIf(entry -> entry.getId() == id);
    }
}
