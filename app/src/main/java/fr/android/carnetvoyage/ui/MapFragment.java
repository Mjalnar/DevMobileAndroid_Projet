package fr.android.carnetvoyage.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fr.android.carnetvoyage.R;
import fr.android.carnetvoyage.data.LocalRepository;
import fr.android.carnetvoyage.location.LocationHelper;
import fr.android.carnetvoyage.model.Entry;

public class MapFragment extends Fragment implements OnMapReadyCallback, LocationHelper.Callback {

    private GoogleMap googleMap;
    private LocationHelper locationHelper;
    private Marker myPositionMarker;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private LocalRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new LocalRepository(requireContext());

        // Récupère la carte Google déclarée dans le layout et demande son chargement.
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        locationHelper = new LocationHelper(this, this);
        view.findViewById(R.id.btn_my_location)
                .setOnClickListener(v -> locationHelper.requestLocation());
    }

    // Appelé par Google quand la carte est prête à être utilisée.
    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        // Vue d'ensemble de la France au démarrage.
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(46.6, 2.5), 5f));
        loadEntryMarkers();
    }

    private void loadEntryMarkers() {
        executor.execute(() -> {
            List<Entry> entries = repository.getAll();

            Activity activity = getActivity();
            if (activity == null) {
                return;
            }
            activity.runOnUiThread(() -> {
                if (!isAdded() || googleMap == null) {
                    return;
                }
                for (Entry entry : entries) {
                    googleMap.addMarker(new MarkerOptions()
                            .position(new LatLng(entry.getLatitude(), entry.getLongitude()))
                            .title(entry.getTitle())
                            .snippet(entry.getAddress()));
                }
            });
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        locationHelper.onPermissionResult(requestCode, grantResults);
    }

    @Override
    public void onLocationReady(double latitude, double longitude, String address) {
        if (googleMap == null) {
            return;
        }
        LatLng here = new LatLng(latitude, longitude);
        if (myPositionMarker == null) {
            myPositionMarker = googleMap.addMarker(new MarkerOptions()
                    .position(here)
                    .title(getString(R.string.loc_my_position)));
        } else {
            myPositionMarker.setPosition(here);
        }
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(here, 16f));
    }

    @Override
    public void onLocationError(String message) {
        Toast.makeText(getContext(),
                getString(R.string.loc_error) + " : " + message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPermissionDenied() {
        Toast.makeText(getContext(), R.string.loc_permission_denied, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (locationHelper != null) {
            locationHelper.stopLocation();
        }
    }
}
