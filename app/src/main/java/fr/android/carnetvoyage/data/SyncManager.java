package fr.android.carnetvoyage.data;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fr.android.carnetvoyage.model.Entry;

/**
 * Gère la synchronisation entre la base locale (SQLite) et la base distante (MySQL).
 * Feature "Plus" du projet.
 */
public class SyncManager {

    private static final String TAG = "SyncManager";
    private final LocalRepository localRepo;
    private final RemoteRepository remoteRepo;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public SyncManager(Context context) {
        this.localRepo = new LocalRepository(context);
        // Remplacez par l'URL réelle de votre serveur PHP
        this.remoteRepo = new RemoteRepository("http://votre-serveur.com/api/");
    }

    /**
     * Tente d'envoyer les entrées locales non synchronisées vers le serveur.
     */
    public void syncLocalToRemote(SyncCallback callback) {
        executor.execute(() -> {
            List<Entry> allEntries = localRepo.getAll();
            int syncCount = 0;

            for (Entry entry : allEntries) {
                // Si l'entrée n'a pas encore de remoteId, on tente de l'ajouter au serveur
                if (entry.getRemoteId() == -1) {
                    long remoteId = remoteRepo.add(entry);
                    if (remoteId != -1) {
                        entry.setRemoteId(remoteId);
                        localRepo.update(entry);
                        syncCount++;
                        Log.d(TAG, "Synchronisé : " + entry.getTitle() + " (Remote ID: " + remoteId + ")");
                    }
                }
            }

            if (callback != null) {
                int finalSyncCount = syncCount;
                callback.onSyncFinished(finalSyncCount);
            }
        });
    }

    public interface SyncCallback {
        void onSyncFinished(int count);
    }

    /**
     * Vérifie si le réseau est disponible.
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
}
