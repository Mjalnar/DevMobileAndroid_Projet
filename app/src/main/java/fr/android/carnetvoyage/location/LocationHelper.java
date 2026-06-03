package fr.android.carnetvoyage.location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fr.android.carnetvoyage.R;

/**
 * Regroupe toute la localisation : permission runtime, position GPS fraîche
 * (FusedLocationProviderClient) et géocodage inverse (Geocoder).
 * Réutilisable depuis n'importe quel Fragment (écran Carte, écran Ajout…).
 */
public class LocationHelper {

    // Code arbitraire qui nous permet de reconnaître NOTRE demande de permission
    // quand le système nous rappelle dans onRequestPermissionsResult().
    public static final int REQUEST_LOCATION_PERMISSION = 1001;

    /** Le Fragment qui utilise le helper implémente ceci pour recevoir le résultat. */
    public interface Callback {
        void onLocationReady(double latitude, double longitude, String address);
        void onLocationError(String message);
        void onPermissionDenied();
    }

    private final Fragment fragment;
    private final Activity activity;
    private final Callback callback;
    private final FusedLocationProviderClient fusedClient;

    // Le géocodage inverse (version < API 33) est BLOQUANT : on l'exécute sur un
    // thread de fond pour ne pas geler l'UI.
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public LocationHelper(@NonNull Fragment fragment, @NonNull Callback callback) {
        this.fragment = fragment;
        this.activity = fragment.requireActivity();
        this.callback = callback;
        this.fusedClient = LocationServices.getFusedLocationProviderClient(activity);
    }

    /** Vrai si la permission de localisation fine est déjà accordée. */
    public static boolean hasPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Point d'entrée. Si la permission est déjà là -> on récupère la position.
     * Sinon -> on la demande à l'utilisateur ; la suite se fait dans onPermissionResult().
     */
    public void requestLocation() {
        if (hasPermission(activity)) {
            fetchLocation();
        } else {
            // On passe par le Fragment pour que la réponse revienne dans
            // SON onRequestPermissionsResult() (et pas celui de l'Activity).
            fragment.requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        }
    }

    /** À appeler depuis onRequestPermissionsResult() du Fragment hôte. */
    public void onPermissionResult(int requestCode, @NonNull int[] grantResults) {
        if (requestCode != REQUEST_LOCATION_PERMISSION) {
            return;
        }
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchLocation();
        } else {
            callback.onPermissionDenied();
        }
    }

    // @SuppressLint : Android ne "voit" pas notre vérification faite via hasPermission(),
    // donc il signale un faux positif. On l'a bien vérifiée juste avant d'arriver ici.
    @SuppressLint("MissingPermission")
    private void fetchLocation() {
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location == null) {
                        callback.onLocationError(activity.getString(R.string.loc_unavailable));
                    } else {
                        reverseGeocode(location.getLatitude(), location.getLongitude());
                    }
                })
                .addOnFailureListener(e -> callback.onLocationError(e.getMessage()));
    }

    /** Transforme lat/lng en adresse lisible. */
    private void reverseGeocode(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(activity, Locale.getDefault());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // API 33+ : version asynchrone (le système gère le thread pour nous).
            geocoder.getFromLocation(latitude, longitude, 1, addresses -> {
                String address = addresses.isEmpty() ? null : addresses.get(0).getAddressLine(0);
                activity.runOnUiThread(() ->
                        callback.onLocationReady(latitude, longitude, address));
            });
        } else {
            // API < 33 : version bloquante -> thread de fond, puis retour UI.
            executor.execute(() -> {
                String address = null;
                try {
                    List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        address = addresses.get(0).getAddressLine(0);
                    }
                } catch (IOException e) {
                    // Pas de réseau ou service indisponible : on renvoie l'adresse nulle,
                    // le lieu reste utilisable avec ses seules coordonnées.
                }
                final String result = address;
                activity.runOnUiThread(() ->
                        callback.onLocationReady(latitude, longitude, result));
            });
        }
    }
}
