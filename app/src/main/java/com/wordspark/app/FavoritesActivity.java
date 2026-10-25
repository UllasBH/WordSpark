package com.wordspark.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.wordspark.app.adapter.FavoritesAdapter;
import com.wordspark.app.db.FavoriteDao;
import com.wordspark.app.model.FavoriteEntry;
import com.wordspark.app.util.LifecycleUtil;
import com.wordspark.app.util.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FavoritesActivity extends AppCompatActivity implements FavoritesAdapter.Listener {

    private RecyclerView recyclerView;
    private TextView tvEmpty, btnSortFavorites;
    private EditText etSearchFavorites;
    private FavoriteDao favoriteDao;
    private FavoritesAdapter adapter;
    private final List<FavoriteEntry> masterList = new ArrayList<>();
    private final List<FavoriteEntry> items = new ArrayList<>();
    private boolean isSortAZ = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        favoriteDao = new FavoriteDao(this);

        recyclerView = findViewById(R.id.recyclerFavorites);
        tvEmpty = findViewById(R.id.tvEmptyFavorites);
        etSearchFavorites = findViewById(R.id.etSearchFavorites);
        btnSortFavorites = findViewById(R.id.btnSortFavorites);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FavoritesAdapter(items, this);
        recyclerView.setAdapter(adapter);

        if (btnSortFavorites != null) {
            btnSortFavorites.setOnClickListener(v -> {
                isSortAZ = !isSortAZ;
                btnSortFavorites.setText(isSortAZ ? R.string.sort_az : R.string.sort_recent);
                applyFilterAndSort();
            });
        }

        if (etSearchFavorites != null) {
            etSearchFavorites.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    applyFilterAndSort();
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

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
        loadFavorites();
    }

    private void loadFavorites() {
        int userId = SessionManager.getCurrentUserId(this);
        new Thread(() -> {
            List<FavoriteEntry> favorites = favoriteDao.getFavorites(userId);
            runOnUiThread(() -> {
                if (LifecycleUtil.isGone(this)) return;
                masterList.clear();
                masterList.addAll(favorites);
                applyFilterAndSort();
            });
        }).start();
    }

    private void applyFilterAndSort() {
        String filter = etSearchFavorites != null ? etSearchFavorites.getText().toString().trim().toLowerCase(Locale.ROOT) : "";
        List<FavoriteEntry> filtered = new ArrayList<>();

        for (FavoriteEntry entry : masterList) {
            if (filter.isEmpty() || entry.getWord().toLowerCase(Locale.ROOT).contains(filter)) {
                filtered.add(entry);
            }
        }

        if (isSortAZ) {
            filtered.sort((a, b) -> a.getWord().compareToIgnoreCase(b.getWord()));
        }

        items.clear();
        items.addAll(filtered);
        adapter.notifyDataSetChanged();

        boolean empty = items.isEmpty();
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onItemClick(FavoriteEntry entry) {
        Intent intent = new Intent(this, WordDetailActivity.class);
        intent.putExtra(WordDetailActivity.EXTRA_WORD, entry.getWord());
        startActivity(intent);
    }

    @Override
    public void onRemoveClick(FavoriteEntry entry) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Word?")
                .setMessage("Are you sure you want to remove this word from Saved Words?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    int userId = SessionManager.getCurrentUserId(this);
                    new Thread(() -> {
                        favoriteDao.removeFavoriteById(userId, entry.getId());
                        runOnUiThread(this::loadFavorites);
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupBottomNav() {
        findViewById(R.id.navHome).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
        findViewById(R.id.navHistory).setOnClickListener(v -> {
            startActivity(new Intent(this, HistoryActivity.class));
            finish();
        });
        View navQuiz = findViewById(R.id.navQuiz);
        navQuiz.setOnClickListener(v -> {
            startActivity(new Intent(this, QuizActivity.class));
            finish();
        });

        highlightNavItem(R.id.ivNavHome, R.id.tvNavHome, false);
        highlightNavItem(R.id.ivNavHistory, R.id.tvNavHistory, false);
        highlightNavItem(R.id.ivNavSaved, R.id.tvNavSaved, true);
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
