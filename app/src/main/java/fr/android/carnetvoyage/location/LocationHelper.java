package fr.android.carnetvoyage.location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
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
    private final LocationRequest locationRequest;
    private final LocationCallback locationCallback;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public LocationHelper(@NonNull Fragment fragment, @NonNull Callback callback) {
        this.fragment = fragment;
        this.activity = fragment.requireActivity();
        this.callback = callback;
        this.fusedClient = LocationServices.getFusedLocationProviderClient(activity);

        this.locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build();

        this.locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                Location location = result.getLastLocation();
                if (location != null) {
                    reverseGeocode(location.getLatitude(), location.getLongitude());
                }
            }
        };
    }

    public static boolean hasPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public void requestLocation() {
        if (hasPermission(activity)) {
            startUpdates();
        } else {
            fragment.requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    REQUEST_LOCATION_PERMISSION);
        }
    }

    @SuppressLint("MissingPermission")
    private void startUpdates() {
        fusedClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
                .addOnFailureListener(e -> callback.onLocationError(e.getMessage()));
    }

    public void stopLocation() {
        fusedClient.removeLocationUpdates(locationCallback);
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

        if (granted) startUpdates();
        else callback.onPermissionDenied();
    }

    private void reverseGeocode(double latitude, double longitude) {
        executor.execute(() -> {
            String addr = null;
            try {
                Geocoder geocoder = new Geocoder(activity, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    addr = addresses.get(0).getAddressLine(0);
                }
            } catch (IOException ignored) {
            }
            final String finalAddr = addr;
            activity.runOnUiThread(() -> callback.onLocationReady(latitude, longitude, finalAddr));
        });
    }
}
