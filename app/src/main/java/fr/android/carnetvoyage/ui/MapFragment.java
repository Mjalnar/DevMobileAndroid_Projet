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

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fr.android.carnetvoyage.R;
import fr.android.carnetvoyage.data.Repository;
import fr.android.carnetvoyage.data.StubRepository;
import fr.android.carnetvoyage.location.LocationHelper;
import fr.android.carnetvoyage.model.Entry;

public class MapFragment extends Fragment implements LocationHelper.Callback {

    private MapView map;
    private LocationHelper locationHelper;
    private Marker myPositionMarker;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Même contrat que la Liste : on ne connaît que l'interface.
    // TODO : remplacer StubRepository par le LocalRepository (SQLite) de B.
    private final Repository repository = new StubRepository();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // osmdroid EXIGE un "user agent" identifiant l'app, sinon les serveurs
        // de tuiles OpenStreetMap refusent les requêtes. À faire avant d'inflater.
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        map = view.findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);   // tuiles OpenStreetMap standard
        map.setMultiTouchControls(true);               // pincer pour zoomer
        map.getController().setZoom(5.5);
        map.getController().setCenter(new GeoPoint(46.6, 2.5)); // centre approx. de la France

        locationHelper = new LocationHelper(this, this);
        view.findViewById(R.id.btn_my_location)
                .setOnClickListener(v -> locationHelper.requestLocation());

        loadEntryMarkers();
    }

    /** Lit les entrées (thread de fond) puis pose un marqueur par lieu sur l'UI. */
    private void loadEntryMarkers() {
        executor.execute(() -> {
            List<Entry> entries = repository.getAll();

            Activity activity = getActivity();
            if (activity == null) {
                return;
            }
            activity.runOnUiThread(() -> {
                if (!isAdded()) {
                    return;
                }
                for (Entry entry : entries) {
                    Marker marker = new Marker(map);
                    marker.setPosition(new GeoPoint(entry.getLatitude(), entry.getLongitude()));
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    marker.setTitle(entry.getTitle());
                    marker.setSnippet(entry.getAddress());
                    map.getOverlays().add(marker);
                }
                map.invalidate(); // redessine la carte avec les nouveaux marqueurs
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

    // --- Callbacks de LocationHelper : recentrage sur ma position ---

    @Override
    public void onLocationReady(double latitude, double longitude, String address) {
        GeoPoint here = new GeoPoint(latitude, longitude);

        if (myPositionMarker == null) {
            myPositionMarker = new Marker(map);
            myPositionMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            myPositionMarker.setTitle(getString(R.string.loc_my_position));
            map.getOverlays().add(myPositionMarker);
        }
        myPositionMarker.setPosition(here);

        map.getController().setZoom(16.0);
        map.getController().animateTo(here);
        map.invalidate();
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

    // osmdroid a son propre cycle de vie à relayer, sinon fuites / carte figée.
    @Override
    public void onResume() {
        super.onResume();
        if (map != null) {
            map.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (map != null) {
            map.onPause();
        }
    }
}
