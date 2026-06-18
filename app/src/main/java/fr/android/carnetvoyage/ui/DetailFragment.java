package fr.android.carnetvoyage.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fr.android.carnetvoyage.R;
import fr.android.carnetvoyage.data.DatabaseManager;
import fr.android.carnetvoyage.model.Entry;

public class DetailFragment extends Fragment {

    private static final String ARG_ID = "entry_id";

    public static DetailFragment newInstance(long entryId) {
        DetailFragment fragment = new DetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_ID, entryId);
        fragment.setArguments(args);
        return fragment;
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private DatabaseManager databaseManager;
    private Entry entry;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        databaseManager = new DatabaseManager(requireContext());
        long entryId = getArguments() != null ? getArguments().getLong(ARG_ID, -1) : -1;

        executor.execute(() -> {
            Entry loaded = databaseManager.getById(entryId);
            if (getActivity() == null) return;
            requireActivity().runOnUiThread(() -> {
                if (!isAdded() || loaded == null) return;
                entry = loaded;
                bind(view);
            });
        });
    }

    private void bind(View view) {
        ImageView photo = view.findViewById(R.id.detail_photo);
        TextView title = view.findViewById(R.id.detail_title);
        TextView sync = view.findViewById(R.id.detail_sync);
        TextView date = view.findViewById(R.id.detail_date);
        TextView address = view.findViewById(R.id.detail_address);
        TextView coords = view.findViewById(R.id.detail_coords);
        TextView note = view.findViewById(R.id.detail_note);

        title.setText(entry.getTitle());

        boolean synced = entry.getRemoteId() != -1;
        sync.setText(synced ? R.string.sync_state_done : R.string.sync_state_pending);

        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT, Locale.getDefault());
        date.setText(df.format(new Date(entry.getTimestamp())));

        address.setText(entry.getAddress() != null
                ? entry.getAddress()
                : getString(R.string.loc_address_unknown));
        coords.setText(String.format(Locale.getDefault(),
                getString(R.string.location_format), entry.getLatitude(), entry.getLongitude()));
        note.setText(entry.getNote());

        if (entry.getPhotoPath() != null) {
            File file = new File(entry.getPhotoPath());
            if (file.exists()) {
                photo.setImageURI(Uri.fromFile(file));
            }
        }

        view.findViewById(R.id.btn_open_map).setOnClickListener(v -> openInMap());
        view.findViewById(R.id.btn_share).setOnClickListener(v -> shareEntry());
        view.findViewById(R.id.btn_delete).setOnClickListener(v -> confirmDelete());
    }

    private void openInMap() {
        Uri geo = Uri.parse(String.format(Locale.US, "geo:%f,%f?q=%f,%f(%s)",
                entry.getLatitude(), entry.getLongitude(),
                entry.getLatitude(), entry.getLongitude(),
                Uri.encode(entry.getTitle())));
        Intent intent = new Intent(Intent.ACTION_VIEW, geo);
        if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Snackbar.make(requireView(), R.string.detail_no_map_app, Snackbar.LENGTH_SHORT).show();
        }
    }

    private void shareEntry() {
        String text = getString(R.string.share_text,
                entry.getTitle(),
                entry.getAddress() != null ? entry.getAddress() : "",
                entry.getLatitude(), entry.getLongitude());
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(intent, getString(R.string.detail_share)));
    }

    private void confirmDelete() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.detail_delete)
                .setMessage(getString(R.string.delete_confirm, entry.getTitle()))
                .setPositiveButton(R.string.detail_delete, (d, w) -> deleteEntry())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void deleteEntry() {
        long id = entry.getId();
        executor.execute(() -> {
            databaseManager.deleteEntry(id);
            if (getActivity() == null) return;
            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new ListFragment())
                        .commit();
            });
        });
    }
}
