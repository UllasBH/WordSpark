package com.wordspark.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.wordspark.app.R;
import com.wordspark.app.model.FavoriteEntry;

import java.util.List;
import java.util.Locale;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.ViewHolder> {

    public interface Listener {
        void onItemClick(FavoriteEntry entry);
        void onRemoveClick(FavoriteEntry entry);
    }

    private final List<FavoriteEntry> items;
    private final Listener listener;

    public FavoritesAdapter(List<FavoriteEntry> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_favorite, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FavoriteEntry entry = items.get(position);
        String word = entry.getWord();
        holder.tvWord.setText(capitalize(word));

        String pos = entry.getPartOfSpeech();
        String meaning = entry.getMeaning();
        String meta = (pos != null && !pos.isEmpty() ? pos + " · " : "") + (meaning != null ? meaning : "");
        holder.tvMeta.setText(meta);

        holder.itemView.setOnClickListener(v -> listener.onItemClick(entry));
        holder.btnRemove.setOnClickListener(v -> listener.onRemoveClick(entry));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase(Locale.getDefault()) + s.substring(1);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvWord, tvMeta;
        ImageView btnRemove;

        ViewHolder(View itemView) {
            super(itemView);
            tvWord = itemView.findViewById(R.id.tvWord);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }
    }
}
