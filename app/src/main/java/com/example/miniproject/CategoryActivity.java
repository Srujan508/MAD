package com.example.miniproject;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CategoryActivity extends AppCompatActivity {

    public static final String EXTRA_CATEGORY = "EXTRA_CATEGORY";

    private String categoryName;
    private DatabaseHelper dbHelper;
    private MessageAdapter messageAdapter;

    private TextView tvCategoryTitle;
    private TextView tvCategoryCountBadge;
    private ImageView imgCategoryToolbarIcon;
    private FrameLayout avatarCategoryToolbar;
    private ImageView btnBack;

    private EditText etSearchCategory;
    private ImageView btnClearSearchCategory;

    private LinearLayout layoutCategoryEmpty;
    private ImageView imgCategoryEmptyIcon;
    private TextView tvCategoryEmptyMessage;
    private RecyclerView recyclerViewCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        categoryName = getIntent().getStringExtra(EXTRA_CATEGORY);
        if (categoryName == null) {
            categoryName = SMSCategorizer.CATEGORY_PERSONAL;
        }

        dbHelper = new DatabaseHelper(this);

        initViews();
        setupStyling();
        setupRecyclerView();
        setupSearch();
        loadCategoryMessages();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCategoryMessages();
    }

    private void initViews() {
        tvCategoryTitle = findViewById(R.id.tvCategoryTitle);
        tvCategoryCountBadge = findViewById(R.id.tvCategoryCountBadge);
        imgCategoryToolbarIcon = findViewById(R.id.imgCategoryToolbarIcon);
        avatarCategoryToolbar = findViewById(R.id.avatarCategoryToolbar);
        btnBack = findViewById(R.id.btnBack);

        etSearchCategory = findViewById(R.id.etSearchCategory);
        btnClearSearchCategory = findViewById(R.id.btnClearSearchCategory);

        layoutCategoryEmpty = findViewById(R.id.layoutCategoryEmpty);
        imgCategoryEmptyIcon = findViewById(R.id.imgCategoryEmptyIcon);
        tvCategoryEmptyMessage = findViewById(R.id.tvCategoryEmptyMessage);
        recyclerViewCategory = findViewById(R.id.recyclerViewCategory);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void setupStyling() {
        tvCategoryTitle.setText(categoryName + " Messages");

        int catColor = SMSCategorizer.getCategoryColor(this, categoryName);
        int catBgColor = SMSCategorizer.getCategoryBgColor(this, categoryName);

        imgCategoryToolbarIcon.setImageResource(SMSCategorizer.getCategoryIconRes(categoryName));
        imgCategoryToolbarIcon.setColorFilter(catColor);

        GradientDrawable avatarBg = new GradientDrawable();
        avatarBg.setShape(GradientDrawable.OVAL);
        avatarBg.setColor(catBgColor);
        avatarCategoryToolbar.setBackground(avatarBg);

        // Customize count chip
        GradientDrawable chipBg = new GradientDrawable();
        chipBg.setColor(catBgColor);
        chipBg.setCornerRadius(16f);
        chipBg.setStroke(2, catColor);
        tvCategoryCountBadge.setBackground(chipBg);
        tvCategoryCountBadge.setTextColor(catColor);

        // Set empty icon
        imgCategoryEmptyIcon.setImageResource(SMSCategorizer.getCategoryIconRes(categoryName));
        imgCategoryEmptyIcon.setColorFilter(catColor);
    }

    private void setupRecyclerView() {
        recyclerViewCategory.setLayoutManager(new LinearLayoutManager(this));
        messageAdapter = new MessageAdapter(this, null, new MessageAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Message message) {
                Intent intent = new Intent(CategoryActivity.this, MessageDetailActivity.class);
                intent.putExtra("EXTRA_MESSAGE", message);
                startActivity(intent);
            }
        });
        recyclerViewCategory.setAdapter(messageAdapter);
    }

    private void setupSearch() {
        etSearchCategory.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    btnClearSearchCategory.setVisibility(View.GONE);
                    loadCategoryMessages();
                } else {
                    btnClearSearchCategory.setVisibility(View.VISIBLE);
                    performSearch(query);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClearSearchCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etSearchCategory.setText("");
            }
        });
    }

    private void loadCategoryMessages() {
        List<Message> messages = dbHelper.getMessagesByCategory(categoryName);
        int count = messages.size();
        tvCategoryCountBadge.setText(count + (count == 1 ? " message" : " messages"));

        if (messages.isEmpty()) {
            layoutCategoryEmpty.setVisibility(View.VISIBLE);
            tvCategoryEmptyMessage.setText("No " + categoryName + " messages found.");
            recyclerViewCategory.setVisibility(View.GONE);
        } else {
            layoutCategoryEmpty.setVisibility(View.GONE);
            recyclerViewCategory.setVisibility(View.VISIBLE);
        }

        messageAdapter.updateList(messages);
    }

    private void performSearch(String query) {
        List<Message> results = dbHelper.searchMessagesByCategory(categoryName, query);
        int count = results.size();
        tvCategoryCountBadge.setText(count + (count == 1 ? " match" : " matches"));

        if (results.isEmpty()) {
            layoutCategoryEmpty.setVisibility(View.VISIBLE);
            tvCategoryEmptyMessage.setText("No " + categoryName + " messages match \"" + query + "\"");
            recyclerViewCategory.setVisibility(View.GONE);
        } else {
            layoutCategoryEmpty.setVisibility(View.GONE);
            recyclerViewCategory.setVisibility(View.VISIBLE);
        }

        messageAdapter.updateList(results);
    }
}
