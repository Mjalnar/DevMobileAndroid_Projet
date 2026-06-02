package fr.android.carnetvoyage.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import fr.android.carnetvoyage.R;
import fr.android.carnetvoyage.data.LocalRepository;
import fr.android.carnetvoyage.model.Entry;

public class ListFragment extends Fragment {

    private TextView textCount;
    private ListView listView;
    private LocalRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);

        textCount = view.findViewById(R.id.text_count);
        listView = view.findViewById(R.id.list_view_simple);
        repository = new LocalRepository(requireContext());

        refreshList();

        return view;
    }

    private void refreshList() {
        List<Entry> entries = repository.getAll();
        textCount.setText("Nombre d'entrées : " + entries.size());

        List<String> displayList = new ArrayList<>();
        for (Entry e : entries) {
            displayList.add(e.getTitle() + "\n" + e.getAddress());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                displayList
        );
        listView.setAdapter(adapter);
    }
}
