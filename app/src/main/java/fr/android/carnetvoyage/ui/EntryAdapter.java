package fr.android.carnetvoyage.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import fr.android.carnetvoyage.R;
import fr.android.carnetvoyage.model.Entry;

/**
 * Fait le lien entre la liste d'Entry et le RecyclerView.
 * Le RecyclerView ne crée qu'une poignée de "lignes" (ViewHolder) et les
 * recycle au défilement -> c'est ça qui le rend fluide même avec beaucoup d'items.
 */
public class EntryAdapter extends RecyclerView.Adapter<EntryAdapter.EntryViewHolder> {

    private final List<Entry> entries = new ArrayList<>();

    // Format de date dépendant de la langue du téléphone (i18n gratuite).
    private final DateFormat dateFormat =
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault());

    /** Remplace les données affichées et redessine la liste. */
    public void setData(List<Entry> newEntries) {
        entries.clear();
        if (newEntries != null) {
            entries.addAll(newEntries);
        }
        notifyDataSetChanged();
    }

    // Appelé quand le RecyclerView a besoin d'une NOUVELLE ligne vide.
    @NonNull
    @Override
    public EntryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_entry, parent, false);
        return new EntryViewHolder(view);
    }

    // Appelé pour REMPLIR une ligne (neuve ou recyclée) avec l'entrée n° position.
    @Override
    public void onBindViewHolder(@NonNull EntryViewHolder holder, int position) {
        Entry entry = entries.get(position);
        holder.title.setText(entry.getTitle());

        String address = entry.getAddress();
        holder.address.setText(address != null
                ? address
                : holder.itemView.getContext().getString(R.string.loc_address_unknown));

        holder.date.setText(dateFormat.format(new Date(entry.getTimestamp())));

        // TODO : afficher la vraie vignette depuis entry.getPhotoPath() quand B
        //        aura la capture photo (décodage à faire hors thread principal).
        holder.photo.setImageResource(android.R.drawable.ic_menu_gallery);
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    /** Garde les références des vues d'une ligne pour éviter de re-chercher à chaque bind. */
    static class EntryViewHolder extends RecyclerView.ViewHolder {
        final ImageView photo;
        final TextView title;
        final TextView address;
        final TextView date;

        EntryViewHolder(@NonNull View itemView) {
            super(itemView);
            photo = itemView.findViewById(R.id.img_photo);
            title = itemView.findViewById(R.id.tv_title);
            address = itemView.findViewById(R.id.tv_address);
            date = itemView.findViewById(R.id.tv_date);
        }
    }
}
