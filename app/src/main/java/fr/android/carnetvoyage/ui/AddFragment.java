package fr.android.carnetvoyage.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
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

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fr.android.carnetvoyage.R;
import fr.android.carnetvoyage.data.DatabaseManager;
import fr.android.carnetvoyage.location.LocationHelper;
import fr.android.carnetvoyage.model.Entry;

public class AddFragment extends Fragment implements LocationHelper.Callback {

    private EditText editTitle, editNote;
    private ImageView imagePreview;
    private TextView textLocation;
    private Button btnTakePhoto, btnSave;

    private String currentPhotoPath;
    private double currentLat, currentLng;
    private String currentAddress = "";

    private DatabaseManager databaseManager;
    private LocationHelper locationHelper;
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

        databaseManager = new DatabaseManager(requireContext());
        locationHelper = new LocationHelper(this, this);

        btnTakePhoto.setOnClickListener(v -> checkCameraPermission());
        btnSave.setOnClickListener(v -> saveEntry());

        locationHelper.requestLocation();

        return view;
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            dispatchTakePictureIntent();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        locationHelper.onPermissionResult(requestCode, grantResults);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (locationHelper != null) {
            locationHelper.stopLocation();
        }
    }

    @Override
    public void onLocationReady(double latitude, double longitude, String address) {
        currentLat = latitude;
        currentLng = longitude;
        currentAddress = address;
        if (isAdded()) {
            textLocation.setText(String.format(Locale.getDefault(), getString(R.string.location_format), currentLat, currentLng));
        }
    }

    @Override
    public void onLocationError(String message) {
        if (isAdded()) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPermissionDenied() {
        if (isAdded()) {
            Toast.makeText(requireContext(), R.string.loc_permission_denied, Toast.LENGTH_SHORT).show();
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
            long id = databaseManager.addEntry(entry);
            requireActivity().runOnUiThread(() -> {
                if (id != -1) {
                    Toast.makeText(requireContext(), R.string.success_saving, Toast.LENGTH_SHORT).show();
                    // Retourner à la liste après sauvegarde
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, new ListFragment())
                            .commit();
                } else {
                    Toast.makeText(requireContext(), R.string.error_saving, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
