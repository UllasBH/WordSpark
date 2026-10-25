package com.wordspark.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.wordspark.app.adapter.HistoryAdapter;
import com.wordspark.app.util.LifecycleUtil;
import com.wordspark.app.db.HistoryDao;
import com.wordspark.app.model.HistoryEntry;
import com.wordspark.app.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity implements HistoryAdapter.Listener {

    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private HistoryDao historyDao;
    private HistoryAdapter adapter;
    private final List<HistoryEntry> items = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        historyDao = new HistoryDao(this);

        recyclerView = findViewById(R.id.recyclerHistory);
        tvEmpty = findViewById(R.id.tvEmptyHistory);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(items, this);
        recyclerView.setAdapter(adapter);

        findViewById(R.id.btnClearHistory).setOnClickListener(v -> confirmClearHistory());

        setupBottomNav();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!SessionManager.isLoggedIn(this)) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHistory();
    }

    private void loadHistory() {
        int userId = SessionManager.getCurrentUserId(this);
        new Thread(() -> {
            List<HistoryEntry> history = historyDao.getHistory(userId);
            runOnUiThread(() -> {
                if (LifecycleUtil.isGone(this)) return;
                items.clear();
                items.addAll(history);
                adapter.notifyDataSetChanged();
                tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                recyclerView.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
            });
        }).start();
    }

    private void confirmClearHistory() {
        if (items.isEmpty()) return;
        new AlertDialog.Builder(this)
                .setTitle(R.string.clear_history)
                .setMessage(R.string.clear_history_confirm)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    int userId = SessionManager.getCurrentUserId(this);
                    new Thread(() -> {
                        boolean success = true;
                        try {
                            historyDao.clearHistory(userId);
                        } catch (Exception e) {
                            success = false;
                        }
                        final boolean cleared = success;
                        runOnUiThread(() -> {
                if (LifecycleUtil.isGone(this)) return;
                            if (cleared) {
                                loadHistory(); // refreshes the list + shows the empty state
                                android.widget.Toast.makeText(this, R.string.history_cleared, android.widget.Toast.LENGTH_SHORT).show();
                            } else {
                                android.widget.Toast.makeText(this, R.string.err_clear_history_failed, android.widget.Toast.LENGTH_SHORT).show();
                            }
                        });
                    }).start();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void onItemClick(HistoryEntry entry) {
        Intent intent = new Intent(this, WordDetailActivity.class);
        intent.putExtra(WordDetailActivity.EXTRA_WORD, entry.getWord());
        startActivity(intent);
    }

    private void setupBottomNav() {
        findViewById(R.id.navHome).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
        findViewById(R.id.navSaved).setOnClickListener(v -> {
            startActivity(new Intent(this, FavoritesActivity.class));
            finish();
        });
        View navQuiz = findViewById(R.id.navQuiz);
        navQuiz.setOnClickListener(v -> {
            startActivity(new Intent(this, QuizActivity.class));
            finish();
        });

        highlightNavItem(R.id.ivNavHome, R.id.tvNavHome, false);
        highlightNavItem(R.id.ivNavHistory, R.id.tvNavHistory, true);
        highlightNavItem(R.id.ivNavSaved, R.id.tvNavSaved, false);
        highlightNavItem(R.id.ivNavQuiz, R.id.tvNavQuiz, false);
    }


    private void highlightNavItem(int iconId, int labelId, boolean active) {
        ImageView icon = findViewById(iconId);
        TextView label = findViewById(labelId);
        int color = ContextCompat.getColor(this, active ? R.color.primary : R.color.text_secondary);
        icon.setColorFilter(color);
        label.setTextColor(color);
    }
}
