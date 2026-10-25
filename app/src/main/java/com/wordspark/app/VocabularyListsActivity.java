package com.wordspark.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.wordspark.app.vocab.VocabCategory;
import com.wordspark.app.vocab.VocabularyBank;

public class VocabularyListsActivity extends AppCompatActivity {

    private LinearLayout categoriesContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vocabulary_lists);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        categoriesContainer = findViewById(R.id.categoriesContainer);

        buildCategories();
    }

    private void buildCategories() {
        categoriesContainer.removeAllViews();
        for (VocabCategory category : VocabularyBank.getCategories()) {
            View card = LayoutInflater.from(this).inflate(R.layout.item_vocabulary_category, categoriesContainer, false);

            TextView tvCategoryName = card.findViewById(R.id.tvCategoryName);
            TextView tvWordCount = card.findViewById(R.id.tvWordCount);

            tvCategoryName.setText(category.getName());
            tvWordCount.setText(category.getWordCount(this) + " words");

            card.setOnClickListener(v -> {
                Intent intent = new Intent(this, FlashcardActivity.class);
                intent.putExtra(FlashcardActivity.EXTRA_CATEGORY_ID, category.getId());
                startActivity(intent);
            });

            categoriesContainer.addView(card);
        }
    }
}
