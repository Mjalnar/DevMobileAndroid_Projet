package fr.android.carnetvoyage.data;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import fr.android.carnetvoyage.model.Entry;

public class RemoteRepository implements Repository {

    private final String baseUrl;

    public RemoteRepository(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public long add(Entry entry) {
        try {
            URL url = new URL(baseUrl + "add.php");
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
            // Note: photoPath n'est pas envoyé ici car on ne synchronise que les données textuelles/GPS pour l'instant
            // (L'envoi d'image en Base64 ou Multipart serait une amélioration)

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = json.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) response.append(line);
                
                JSONObject resJson = new JSONObject(response.toString());
                return resJson.getLong("remote_id");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public List<Entry> getAll() {
        List<Entry> entries = new ArrayList<>();
        try {
            URL url = new URL(baseUrl + "list.php");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) response.append(line);

                JSONArray array = new JSONArray(response.toString());
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    Entry e = new Entry();
                    e.setRemoteId(obj.getLong("id"));
                    e.setTitle(obj.getString("title"));
                    e.setNote(obj.getString("note"));
                    e.setLatitude(obj.getDouble("latitude"));
                    e.setLongitude(obj.getDouble("longitude"));
                    e.setAddress(obj.getString("address"));
                    e.setTimestamp(obj.getLong("timestamp"));
                    entries.add(e);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return entries;
    }

    @Override
    public Entry getById(long id) { return null; } // Non implémenté pour le remote dans ce projet

    @Override
    public boolean update(Entry entry) { return false; } // Non implémenté

    @Override
    public boolean delete(long remoteId) {
        try {
            URL url = new URL(baseUrl + "delete.php?id=" + remoteId);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            return conn.getResponseCode() == HttpURLConnection.HTTP_OK;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
