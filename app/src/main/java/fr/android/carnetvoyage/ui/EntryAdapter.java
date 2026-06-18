package fr.android.carnetvoyage.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.net.Uri;
import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import fr.android.carnetvoyage.R;
import fr.android.carnetvoyage.model.Entry;

public class EntryAdapter extends RecyclerView.Adapter<EntryAdapter.EntryViewHolder> {

    public interface OnEntryClickListener {
        void onEntryClick(Entry entry);
    }

    private final List<Entry> entries = new ArrayList<>();
    private final OnEntryClickListener clickListener;

    private final DateFormat dateFormat =
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault());

    public EntryAdapter(OnEntryClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void setData(List<Entry> newEntries) {
        entries.clear();
        if (newEntries != null) {
            entries.addAll(newEntries);
        }
        notifyDataSetChanged();
    }

    public Entry getEntryAt(int position) {
        return entries.get(position);
    }

    @NonNull
    @Override
    public EntryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_entry, parent, false);
        return new EntryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EntryViewHolder holder, int position) {
        Entry entry = entries.get(position);
        holder.title.setText(entry.getTitle());

        String address = entry.getAddress();
        holder.address.setText(address != null
                ? address
                : holder.itemView.getContext().getString(R.string.loc_address_unknown));

        holder.date.setText(dateFormat.format(new Date(entry.getTimestamp())));

        boolean synced = entry.getRemoteId() != -1;
        holder.sync.setText(synced ? R.string.sync_state_done : R.string.sync_state_pending);

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onEntryClick(entry);
        });

        if (entry.getPhotoPath() != null) {
            File photoFile = new File(entry.getPhotoPath());
            if (photoFile.exists()) {
                holder.photo.setImageURI(Uri.fromFile(photoFile));
            } else {
                holder.photo.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        } else {
            holder.photo.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class EntryViewHolder extends RecyclerView.ViewHolder {
        final ImageView photo;
        final TextView title;
        final TextView address;
        final TextView date;
        final TextView sync;

        EntryViewHolder(@NonNull View itemView) {
            super(itemView);
            photo = itemView.findViewById(R.id.img_photo);
            title = itemView.findViewById(R.id.tv_title);
            address = itemView.findViewById(R.id.tv_address);
            date = itemView.findViewById(R.id.tv_date);
            sync = itemView.findViewById(R.id.tv_sync);
        }
    }
}
