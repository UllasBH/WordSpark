package com.wordspark.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.wordspark.app.R;
import com.wordspark.app.model.HistoryEntry;
import com.wordspark.app.util.TimeUtil;

import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    public interface Listener {
        void onItemClick(HistoryEntry entry);
    }

    private final List<HistoryEntry> items;
    private final Listener listener;

    public HistoryAdapter(List<HistoryEntry> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryEntry entry = items.get(position);
        holder.tvWord.setText(capitalize(entry.getWord()));
        holder.tvWhen.setText(TimeUtil.relativeTime(entry.getTimestamp()));
        holder.itemView.setOnClickListener(v -> listener.onItemClick(entry));
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
        TextView tvWord, tvWhen;

        ViewHolder(View itemView) {
            super(itemView);
            tvWord = itemView.findViewById(R.id.tvWord);
            tvWhen = itemView.findViewById(R.id.tvWhen);
        }
    }
}
