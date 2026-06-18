package fr.android.carnetvoyage;

import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import fr.android.carnetvoyage.data.Settings;
import fr.android.carnetvoyage.data.SyncManager;
import fr.android.carnetvoyage.ui.AddFragment;
import fr.android.carnetvoyage.ui.ListFragment;
import fr.android.carnetvoyage.ui.MapFragment;
import fr.android.carnetvoyage.ui.SettingsFragment;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private SyncManager syncManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Settings.applySavedNightMode(this);
        Settings.applySavedLanguage(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        syncManager = new SyncManager(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.navigation_view);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_list) {
                loadFragment(new ListFragment(), getString(R.string.menu_list));
            } else if (id == R.id.nav_map) {
                loadFragment(new MapFragment(), getString(R.string.menu_map));
            } else if (id == R.id.nav_add) {
                loadFragment(new AddFragment(), getString(R.string.menu_add));
            } else if (id == R.id.nav_settings) {
                loadFragment(new SettingsFragment(), getString(R.string.menu_settings));
            } else if (id == R.id.nav_sync) {
                handleSync();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        if (savedInstanceState == null) {
            loadFragment(new ListFragment(), getString(R.string.menu_list));
            navigationView.setCheckedItem(R.id.nav_list);
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void handleSync() {
        if (!SyncManager.isNetworkAvailable(this)) {
            Snackbar.make(drawerLayout, R.string.no_network, Snackbar.LENGTH_SHORT).show();
            return;
        }

        Snackbar.make(drawerLayout, R.string.sync_started, Snackbar.LENGTH_SHORT).show();
        syncManager.sync(count -> runOnUiThread(() ->
                Snackbar.make(drawerLayout,
                        getString(R.string.sync_finished, count),
                        Snackbar.LENGTH_LONG).show()
        ));
    }

    public void showFragment(Fragment fragment, String title) {
        loadFragment(fragment, title);
    }

    private void loadFragment(Fragment fragment, String title) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
        setTitle(title);
    }
}
