package fr.android.carnetvoyage.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fr.android.carnetvoyage.R;
import fr.android.carnetvoyage.data.Repository;
import fr.android.carnetvoyage.data.StubRepository;
import fr.android.carnetvoyage.model.Entry;

public class ListFragment extends Fragment {

    private EntryAdapter adapter;
    private TextView emptyView;

    // Lecture des données sur un thread de fond (la vraie source = SQLite).
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // On ne connaît QUE l'interface. TODO : remplacer StubRepository par le
    // LocalRepository (SQLite) de B dès qu'il est livré -> rien d'autre à changer ici.
    private final Repository repository = new StubRepository();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        emptyView = view.findViewById(R.id.tv_empty);

        RecyclerView recyclerView = view.findViewById(R.id.rv_entries);
        // 1 colonne en portrait, 2 en paysage : la valeur vient de values(-land)/integers.xml.
        int spanCount = getResources().getInteger(R.integer.list_span_count);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), spanCount));
        adapter = new EntryAdapter();
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Rechargé à chaque retour sur l'écran -> les nouvelles entrées apparaissent.
        loadEntries();
    }

    private void loadEntries() {
        executor.execute(() -> {
            List<Entry> entries = repository.getAll();

            Activity activity = getActivity();
            if (activity == null) {
                return; // fragment détaché entre-temps
            }
            activity.runOnUiThread(() -> {
                if (!isAdded()) {
                    return;
                }
                adapter.setData(entries);
                emptyView.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }
}
