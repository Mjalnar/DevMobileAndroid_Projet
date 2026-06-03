package fr.android.carnetvoyage.location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
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

public class LocationHelper {

    public static final int REQUEST_LOCATION_PERMISSION = 1001;

    public interface Callback {
        void onLocationReady(double latitude, double longitude, String address);
        void onLocationError(String message);
        void onPermissionDenied();
    }

    private final Fragment fragment;
    private final Activity activity;
    private final Callback callback;
    private final FusedLocationProviderClient fusedClient;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public LocationHelper(@NonNull Fragment fragment, @NonNull Callback callback) {
        this.fragment = fragment;
        this.activity = fragment.requireActivity();
        this.callback = callback;
        this.fusedClient = LocationServices.getFusedLocationProviderClient(activity);
    }

    public static boolean hasPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isLocationEnabled() {
        LocationManager lm = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || 
               lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public void requestLocation() {
        if (!isLocationEnabled()) {
            callback.onLocationError(activity.getString(R.string.loc_unavailable));
            return;
        }

        if (hasPermission(activity)) {
            fetchLocation();
        } else {
            fragment.requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    REQUEST_LOCATION_PERMISSION);
        }
    }

    public void onPermissionResult(int requestCode, @NonNull int[] grantResults) {
        if (requestCode != REQUEST_LOCATION_PERMISSION) return;
        
        boolean granted = false;
        for (int res : grantResults) {
            if (res == PackageManager.PERMISSION_GRANTED) {
                granted = true;
                break;
            }
        }

        if (granted) fetchLocation();
        else callback.onPermissionDenied();
    }

    @SuppressLint("MissingPermission")
    private void fetchLocation() {
        // 1. Tenter d'abord la position rapide
        fusedClient.getLastLocation().addOnSuccessListener(activity, location -> {
            if (location != null) {
                reverseGeocode(location.getLatitude(), location.getLongitude());
            }
            
            // 2. Demander une position fraîche et précise
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(freshLocation -> {
                        if (freshLocation != null) {
                            reverseGeocode(freshLocation.getLatitude(), freshLocation.getLongitude());
                        } else if (location == null) {
                            callback.onLocationError(activity.getString(R.string.loc_unavailable));
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (location == null) callback.onLocationError(e.getMessage());
                    });
        });
    }

    private void reverseGeocode(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(activity, Locale.getDefault());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(latitude, longitude, 1, addresses -> {
                String addr = addresses.isEmpty() ? null : addresses.get(0).getAddressLine(0);
                activity.runOnUiThread(() -> callback.onLocationReady(latitude, longitude, addr));
            });
        } else {
            executor.execute(() -> {
                String addr = null;
                try {
                    List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                    if (addresses != null && !addresses.isEmpty()) addr = addresses.get(0).getAddressLine(0);
                } catch (IOException ignored) {}
                final String finalAddr = addr;
                activity.runOnUiThread(() -> callback.onLocationReady(latitude, longitude, finalAddr));
            });
        }
    }
}
