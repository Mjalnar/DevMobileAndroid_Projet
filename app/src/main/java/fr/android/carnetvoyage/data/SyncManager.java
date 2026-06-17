package fr.android.carnetvoyage.data;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fr.android.carnetvoyage.model.Entry;

/**
 * GESTIONNAIRE DE SYNCHRONISATION (Réseau)
 * S'occupe d'envoyer les données locales vers un serveur distant.
 * Comment l'app communique avec Internet.
 */
public class SyncManager {

    private final DatabaseManager databaseManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    
    // URL de l'API.
    // 10.0.2.2 = adresse spéciale qui, depuis l'ÉMULATEUR Android, pointe vers le PC (localhost).
    private static final String API_URL = "http://10.0.2.2:8000";

    public SyncManager(Context context) {
        this.databaseManager = new DatabaseManager(context);
    }

    /**
     * Parcourt la base locale et envoie ce qui n'est pas encore sur le serveur.
     */
    public void syncLocalToRemote(SyncCallback callback) {
        executor.execute(() -> {
            List<Entry> entries = databaseManager.getAllEntries();
            int count = 0;

            for (Entry entry : entries) {
                // Si remoteId == -1, l'entrée n'est pas encore synchronisée
                if (entry.getRemoteId() == -1) {
                    long remoteId = sendToServer(entry);
                    if (remoteId != -1) {
                        databaseManager.updateRemoteId(entry.getId(), remoteId);
                        count++;
                    }
                }
            }
            
            final int finalCount = count;
            if (callback != null) callback.onSyncFinished(finalCount);
        });
    }

    /** Envoi HTTP POST d'un objet JSON vers le serveur. */
    private long sendToServer(Entry entry) {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");

            // Création du JSON
            JSONObject json = new JSONObject();
            json.put("title", entry.getTitle());
            json.put("note", entry.getNote());
            json.put("latitude", entry.getLatitude());
            json.put("longitude", entry.getLongitude());
            json.put("address", entry.getAddress());
            json.put("timestamp", entry.getTimestamp());

            // Envoi des données
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = json.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Lecture de la réponse (on attend l'ID généré par MySQL)
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                
                JSONObject res = new JSONObject(sb.toString());
                return res.getLong("remote_id");
            }
        } catch (Exception e) {
            Log.e("SyncManager", "Erreur synchro : " + e.getMessage());
        }
        return -1;
    }

    public interface SyncCallback {
        void onSyncFinished(int count);
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo net = cm.getActiveNetworkInfo();
        return net != null && net.isConnected();
    }
}
