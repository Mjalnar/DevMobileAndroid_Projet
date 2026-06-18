package fr.android.carnetvoyage.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fr.android.carnetvoyage.MainActivity;
import fr.android.carnetvoyage.R;
import fr.android.carnetvoyage.data.DatabaseManager;
import fr.android.carnetvoyage.model.Entry;

public class ListFragment extends Fragment implements EntryAdapter.OnEntryClickListener {

    private static final int SORT_DATE_DESC = 0;
    private static final int SORT_DATE_ASC = 1;
    private static final int SORT_TITLE = 2;

    private EntryAdapter adapter;
    private TextView emptyView;
    private SwipeRefreshLayout swipeRefresh;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private DatabaseManager databaseManager;

    private final List<Entry> allEntries = new ArrayList<>();
    private String currentQuery = "";
    private int currentSort = SORT_DATE_DESC;

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

        databaseManager = new DatabaseManager(requireContext());
        emptyView = view.findViewById(R.id.tv_empty);

        RecyclerView recyclerView = view.findViewById(R.id.rv_entries);
        int spanCount = getResources().getInteger(R.integer.list_span_count);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), spanCount));
        adapter = new EntryAdapter(this);
        recyclerView.setAdapter(adapter);

        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        swipeRefresh.setOnRefreshListener(this::loadEntries);

        view.findViewById(R.id.fab_add).setOnClickListener(v ->
                ((MainActivity) requireActivity()).showFragment(new AddFragment(), getString(R.string.menu_add)));

        attachSwipeToDelete(recyclerView);

        setupMenu();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadEntries();
    }

    @Override
    public void onEntryClick(Entry entry) {
        ((MainActivity) requireActivity())
                .showFragment(DetailFragment.newInstance(entry.getId()), entry.getTitle());
    }

    private void loadEntries() {
        executor.execute(() -> {
            List<Entry> entries = databaseManager.getAllEntries();
            Activity activity = getActivity();
            if (activity == null) return;
            activity.runOnUiThread(() -> {
                if (!isAdded()) return;
                allEntries.clear();
                allEntries.addAll(entries);
                swipeRefresh.setRefreshing(false);
                applyFilterAndSort();
            });
        });
    }

    private void applyFilterAndSort() {
        List<Entry> shown = new ArrayList<>();
        String q = currentQuery.toLowerCase(Locale.getDefault()).trim();

        for (Entry e : allEntries) {
            if (q.isEmpty() || matches(e, q)) {
                shown.add(e);
            }
        }
        sort(shown);

        adapter.setData(shown);
        emptyView.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private boolean matches(Entry e, String query) {
        String title = e.getTitle() != null ? e.getTitle().toLowerCase(Locale.getDefault()) : "";
        String addr = e.getAddress() != null ? e.getAddress().toLowerCase(Locale.getDefault()) : "";
        return title.contains(query) || addr.contains(query);
    }

    private void sort(List<Entry> list) {
        switch (currentSort) {
            case SORT_DATE_ASC:
                Collections.sort(list, Comparator.comparingLong(Entry::getTimestamp));
                break;
            case SORT_TITLE:
                Collections.sort(list, Comparator.comparing(
                        e -> e.getTitle() == null ? "" : e.getTitle(),
                        String.CASE_INSENSITIVE_ORDER));
                break;
            case SORT_DATE_DESC:
            default:
                Collections.sort(list, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                break;
        }
    }

    private void setupMenu() {
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
                inflater.inflate(R.menu.list_menu, menu);

                SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
                searchView.setQueryHint(getString(R.string.search_hint));
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        currentQuery = newText;
                        applyFilterAndSort();
                        return true;
                    }
                });
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.sort_date_desc) {
                    currentSort = SORT_DATE_DESC;
                } else if (id == R.id.sort_date_asc) {
                    currentSort = SORT_DATE_ASC;
                } else if (id == R.id.sort_title) {
                    currentSort = SORT_TITLE;
                } else {
                    return false;
                }
                item.setChecked(true);
                applyFilterAndSort();
                return true;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    private void attachSwipeToDelete(RecyclerView recyclerView) {
        ItemTouchHelper helper = new ItemTouchHelper(
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                    @Override
                    public boolean onMove(@NonNull RecyclerView rv,
                                          @NonNull RecyclerView.ViewHolder vh,
                                          @NonNull RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) {
                        Entry entry = adapter.getEntryAt(vh.getBindingAdapterPosition());
                        deleteWithUndo(entry);
                    }
                });
        helper.attachToRecyclerView(recyclerView);
    }

    private void deleteWithUndo(Entry entry) {
        executor.execute(() -> {
            databaseManager.deleteEntry(entry.getId());
            Activity activity = getActivity();
            if (activity == null) return;
            activity.runOnUiThread(() -> {
                if (!isAdded()) return;
                allEntries.remove(entry);
                applyFilterAndSort();
                Snackbar.make(requireView(),
                                getString(R.string.delete_done, entry.getTitle()),
                                Snackbar.LENGTH_LONG)
                        .setAction(R.string.undo, v -> restore(entry))
                        .show();
            });
        });
    }

    private void restore(Entry entry) {
        executor.execute(() -> {
            databaseManager.addEntry(entry);
            Activity activity = getActivity();
            if (activity == null) return;
            activity.runOnUiThread(this::loadEntries);
        });
    }
}
