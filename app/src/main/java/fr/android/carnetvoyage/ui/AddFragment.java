package fr.android.carnetvoyage.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fr.android.carnetvoyage.R;
import fr.android.carnetvoyage.data.LocalRepository;
import fr.android.carnetvoyage.data.Repository;
import fr.android.carnetvoyage.model.Entry;

public class AddFragment extends Fragment {

    private EditText editTitle, editNote;
    private ImageView imagePreview;
    private TextView textLocation;
    private Button btnTakePhoto, btnSave;

    private String currentPhotoPath;
    private double currentLat, currentLng;
    private String currentAddress = "";

    private Repository repository;
    private FusedLocationProviderClient fusedLocationClient;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Launcher pour la caméra
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    imagePreview.setVisibility(View.VISIBLE);
                    imagePreview.setImageURI(Uri.fromFile(new File(currentPhotoPath)));
                }
            }
    );

    // Launcher pour la permission Caméra
    private final ActivityResultLauncher<String> cameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    dispatchTakePictureIntent();
                }
            }
    );

    // Launcher pour la permission Localisation
    private final ActivityResultLauncher<String[]> locationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                Boolean fineLocation = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseLocation = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                if (fineLocation != null && fineLocation || coarseLocation != null && coarseLocation) {
                    getLastLocation();
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add, container, false);

        editTitle = view.findViewById(R.id.edit_title);
        editNote = view.findViewById(R.id.edit_note);
        imagePreview = view.findViewById(R.id.image_preview);
        textLocation = view.findViewById(R.id.text_location);
        btnTakePhoto = view.findViewById(R.id.btn_take_photo);
        btnSave = view.findViewById(R.id.btn_save);

        repository = new LocalRepository(requireContext());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        btnTakePhoto.setOnClickListener(v -> checkCameraPermission());
        btnSave.setOnClickListener(v -> saveEntry());

        checkLocationPermission();

        return view;
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            dispatchTakePictureIntent();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getLastLocation();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void getLastLocation() {
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
                if (location != null) {
                    currentLat = location.getLatitude();
                    currentLng = location.getLongitude();
                    textLocation.setText(String.format(Locale.getDefault(), getString(R.string.location_format), currentLat, currentLng));
                    
                    // Géocodage inverse (normalement c'est Lucas (A), mais j'en ai besoin pour l'objet Entry)
                    executor.execute(() -> {
                        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                        try {
                            List<Address> addresses = geocoder.getFromLocation(currentLat, currentLng, 1);
                            if (addresses != null && !addresses.isEmpty()) {
                                currentAddress = addresses.get(0).getAddressLine(0);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                }
            });
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        if (photoFile != null) {
            Uri photoURI = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            cameraLauncher.launch(takePictureIntent);
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void saveEntry() {
        String title = editTitle.getText().toString().trim();
        String note = editNote.getText().toString().trim();

        if (title.isEmpty()) {
            editTitle.setError(getString(R.string.hint_title));
            return;
        }

        Entry entry = new Entry();
        entry.setTitle(title);
        entry.setNote(note);
        entry.setPhotoPath(currentPhotoPath);
        entry.setLatitude(currentLat);
        entry.setLongitude(currentLng);
        entry.setAddress(currentAddress);
        entry.setTimestamp(System.currentTimeMillis());

        executor.execute(() -> {
            long id = repository.add(entry);
            requireActivity().runOnUiThread(() -> {
                if (id != -1) {
                    Toast.makeText(requireContext(), R.string.success_saving, Toast.LENGTH_SHORT).show();
                    // On pourrait naviguer vers la liste ici
                } else {
                    Toast.makeText(requireContext(), R.string.error_saving, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
