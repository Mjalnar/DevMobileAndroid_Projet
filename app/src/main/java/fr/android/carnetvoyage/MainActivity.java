package fr.android.carnetvoyage;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;

import fr.android.carnetvoyage.data.SyncManager;
import fr.android.carnetvoyage.ui.AddFragment;
import fr.android.carnetvoyage.ui.ListFragment;
import fr.android.carnetvoyage.ui.MapFragment;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private SyncManager syncManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialisation du gestionnaire de synchronisation (Personne B)
        syncManager = new SyncManager(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.navigation_view);

        // Bouton "hamburger" : lie la toolbar au tiroir
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Clics sur les éléments du menu
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_list) {
                loadFragment(new ListFragment(), getString(R.string.menu_list));
            } else if (id == R.id.nav_map) {
                loadFragment(new MapFragment(), getString(R.string.menu_map));
            } else if (id == R.id.nav_add) {
                loadFragment(new AddFragment(), getString(R.string.menu_add));
            } else if (id == R.id.nav_sync) {
                handleSync();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Fragment par défaut
        if (savedInstanceState == null) {
            loadFragment(new ListFragment(), getString(R.string.menu_list));
            navigationView.setCheckedItem(R.id.nav_list);
        }
    }

    /**
     * Gère la synchronisation manuelle via le menu (Personne B)
     */
    private void handleSync() {
        if (!SyncManager.isNetworkAvailable(this)) {
            Toast.makeText(this, R.string.no_network, Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, R.string.sync_started, Toast.LENGTH_SHORT).show();
        syncManager.syncLocalToRemote(count -> runOnUiThread(() ->
                Toast.makeText(MainActivity.this,
                        getString(R.string.sync_finished, count),
                        Toast.LENGTH_LONG).show()
        ));
    }

    private void loadFragment(Fragment fragment, String title) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
        setTitle(title);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
