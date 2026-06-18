package fr.android.carnetvoyage.data;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fr.android.carnetvoyage.model.Entry;

public class SyncManager {

    private final Context context;
    private final DatabaseManager databaseManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public SyncManager(Context context) {
        this.context = context.getApplicationContext();
        this.databaseManager = new DatabaseManager(context);
    }

    private String apiUrl() {
        return Settings.getServerUrl(context);
    }

    public void sync(SyncCallback callback) {
        executor.execute(() -> {
            int sent = pushLocalToRemote();
            int received = pullRemoteToLocal();
            if (callback != null) callback.onSyncFinished(sent + received);
        });
    }

    private int pushLocalToRemote() {
        List<Entry> entries = databaseManager.getAllEntries();
        int count = 0;
        for (Entry entry : entries) {
            if (entry.getRemoteId() == -1) {
                long remoteId = sendToServer(entry);
                if (remoteId != -1) {
                    databaseManager.updateRemoteId(entry.getId(), remoteId);
                    count++;
                }
            }
        }
        return count;
    }

    private int pullRemoteToLocal() {
        int count = 0;
        try {
            URL url = new URL(apiUrl() + "/list");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return 0;
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);

            JSONArray array = new JSONArray(sb.toString());
            Set<Long> known = databaseManager.getKnownRemoteIds();

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                long remoteId = obj.getLong("remote_id");

                if (known.contains(remoteId)) continue;

                Entry entry = new Entry();
                entry.setTitle(obj.optString("title"));
                entry.setNote(obj.optString("note"));
                entry.setLatitude(obj.optDouble("latitude", 0));
                entry.setLongitude(obj.optDouble("longitude", 0));
                entry.setAddress(obj.optString("address"));
                entry.setTimestamp(obj.optLong("timestamp"));
                entry.setRemoteId(remoteId);
                databaseManager.addEntry(entry);
                count++;
            }
        } catch (Exception e) {
            Log.e("SyncManager", "Erreur synchro descendante : " + e.getMessage());
        }
        return count;
    }

    private long sendToServer(Entry entry) {
        try {
            URL url = new URL(apiUrl());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");

            JSONObject json = new JSONObject();
            json.put("title", entry.getTitle());
            json.put("note", entry.getNote());
            json.put("latitude", entry.getLatitude());
            json.put("longitude", entry.getLongitude());
            json.put("address", entry.getAddress());
            json.put("timestamp", entry.getTimestamp());

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = json.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

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
