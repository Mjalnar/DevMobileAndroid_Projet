package fr.android.carnetvoyage.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;

import fr.android.carnetvoyage.R;
import fr.android.carnetvoyage.data.Settings;

public class SettingsFragment extends Fragment {

    private EditText editServerUrl;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        editServerUrl = view.findViewById(R.id.edit_server_url);
        editServerUrl.setText(Settings.getServerUrl(requireContext()));

        Button btnSaveUrl = view.findViewById(R.id.btn_save_url);
        btnSaveUrl.setOnClickListener(v -> {
            String url = editServerUrl.getText().toString().trim();
            if (url.isEmpty()) {
                url = Settings.DEFAULT_SERVER_URL;
                editServerUrl.setText(url);
            }
            Settings.setServerUrl(requireContext(), url);
            Snackbar.make(view, R.string.settings_saved, Snackbar.LENGTH_SHORT).show();
        });

        RadioGroup group = view.findViewById(R.id.group_theme);
        switch (Settings.getNightMode(requireContext())) {
            case AppCompatDelegate.MODE_NIGHT_NO:
                group.check(R.id.theme_light);
                break;
            case AppCompatDelegate.MODE_NIGHT_YES:
                group.check(R.id.theme_dark);
                break;
            default:
                group.check(R.id.theme_system);
                break;
        }

        group.setOnCheckedChangeListener((g, checkedId) -> {
            int mode;
            if (checkedId == R.id.theme_light) {
                mode = AppCompatDelegate.MODE_NIGHT_NO;
            } else if (checkedId == R.id.theme_dark) {
                mode = AppCompatDelegate.MODE_NIGHT_YES;
            } else {
                mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            }
            Settings.setNightMode(requireContext(), mode);
        });

        RadioGroup langGroup = view.findViewById(R.id.group_language);
        switch (Settings.getLanguage(requireContext())) {
            case "fr":
                langGroup.check(R.id.lang_french);
                break;
            case "en":
                langGroup.check(R.id.lang_english);
                break;
            default:
                langGroup.check(R.id.lang_system);
                break;
        }

        langGroup.setOnCheckedChangeListener((g, checkedId) -> {
            String code;
            if (checkedId == R.id.lang_french) {
                code = "fr";
            } else if (checkedId == R.id.lang_english) {
                code = "en";
            } else {
                code = Settings.LANG_SYSTEM;
            }
            Settings.setLanguage(requireContext(), code);
        });
    }
}
