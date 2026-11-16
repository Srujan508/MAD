package com.example.miniproject;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_READ_SMS = 101;

    // Database & Adapter
    private DatabaseHelper dbHelper;
    private MessageAdapter messageAdapter;

    // Views - Counts & Headers
    private TextView tvTotalCount;
    private TextView tvSyncStatus;
    private TextView tvCountBanking;
    private TextView tvCountOtp;
    private TextView tvCountInfo;
    private TextView tvCountPromo;
    private TextView tvCountPersonal;
    private TextView tvListHeader;
    private ProgressBar progressBarSync;

    // Views - Category Cards
    private CardView cardBanking;
    private CardView cardOtp;
    private CardView cardInfo;
    private CardView cardPromo;
    private CardView cardPersonal;

    // Views - Search & Controls
    private EditText etSearch;
    private ImageView btnClearSearch;
    private ImageView btnRefresh;
    private MaterialButton btnLoadSample;

    // Permission Banner
    private CardView cardPermission;
    private MaterialButton btnGrantPermission;
    private MaterialButton btnLoadDemoDirect;

    // Empty state & RecyclerView
    private LinearLayout layoutEmptyState;
    private TextView tvEmptyMessage;
    private RecyclerView recyclerViewMessages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        initViews();
        setupRecyclerView();
        setupCategoryClicks();
        setupSearch();
        setupButtons();

        // Check SMS permission status
        checkAndRequestSmsPermission();

        // Load cached messages from SQLite
        loadDashboardData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh counts and messages (e.g. if user deleted or modified records)
        loadDashboardData();
    }

    private void initViews() {
        tvTotalCount = findViewById(R.id.tvTotalCount);
        tvSyncStatus = findViewById(R.id.tvSyncStatus);
        tvCountBanking = findViewById(R.id.tvCountBanking);
        tvCountOtp = findViewById(R.id.tvCountOtp);
        tvCountInfo = findViewById(R.id.tvCountInfo);
        tvCountPromo = findViewById(R.id.tvCountPromo);
        tvCountPersonal = findViewById(R.id.tvCountPersonal);
        tvListHeader = findViewById(R.id.tvListHeader);
        progressBarSync = findViewById(R.id.progressBarSync);

        cardBanking = findViewById(R.id.cardBanking);
        cardOtp = findViewById(R.id.cardOtp);
        cardInfo = findViewById(R.id.cardInfo);
        cardPromo = findViewById(R.id.cardPromo);
        cardPersonal = findViewById(R.id.cardPersonal);

        etSearch = findViewById(R.id.etSearch);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnLoadSample = findViewById(R.id.btnLoadSample);

        cardPermission = findViewById(R.id.cardPermission);
        btnGrantPermission = findViewById(R.id.btnGrantPermission);
        btnLoadDemoDirect = findViewById(R.id.btnLoadDemoDirect);

        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);
        recyclerViewMessages = findViewById(R.id.recyclerViewMessages);
    }

    private void setupRecyclerView() {
        recyclerViewMessages.setLayoutManager(new LinearLayoutManager(this));
        messageAdapter = new MessageAdapter(this, null, new MessageAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Message message) {
                Intent intent = new Intent(MainActivity.this, MessageDetailActivity.class);
                intent.putExtra("EXTRA_MESSAGE", message);
                startActivity(intent);
            }
        });
        recyclerViewMessages.setAdapter(messageAdapter);
    }

    private void setupCategoryClicks() {
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String category;
                int id = v.getId();
                if (id == R.id.cardBanking) {
                    category = SMSCategorizer.CATEGORY_BANKING;
                } else if (id == R.id.cardOtp) {
                    category = SMSCategorizer.CATEGORY_OTP;
                } else if (id == R.id.cardInfo) {
                    category = SMSCategorizer.CATEGORY_INFORMATION;
                } else if (id == R.id.cardPromo) {
                    category = SMSCategorizer.CATEGORY_PROMOTIONS;
                } else {
                    category = SMSCategorizer.CATEGORY_PERSONAL;
                }

                Intent intent = new Intent(MainActivity.this, CategoryActivity.class);
                intent.putExtra("EXTRA_CATEGORY", category);
                startActivity(intent);
            }
        };

        cardBanking.setOnClickListener(listener);
        cardOtp.setOnClickListener(listener);
        cardInfo.setOnClickListener(listener);
        cardPromo.setOnClickListener(listener);
        cardPersonal.setOnClickListener(listener);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    btnClearSearch.setVisibility(View.GONE);
                    tvListHeader.setText("Recent Messages");
                    loadRecentMessages();
                } else {
                    btnClearSearch.setVisibility(View.VISIBLE);
                    tvListHeader.setText("Search Results for \"" + query + "\"");
                    performSearch(query);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etSearch.setText("");
            }
        });
    }

    private void setupButtons() {
        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (SMSReader.hasSmsPermission(MainActivity.this)) {
                    readAndSyncDeviceSms();
                } else {
                    showPermissionRationaleDialog();
                }
            }
        });

        btnLoadSample.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadDemoData();
            }
        });

        btnGrantPermission.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestSmsPermission();
            }
        });

        btnLoadDemoDirect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadDemoData();
            }
        });
    }

    private void checkAndRequestSmsPermission() {
        if (!SMSReader.hasSmsPermission(this)) {
            cardPermission.setVisibility(View.VISIBLE);
        } else {
            cardPermission.setVisibility(View.GONE);
            // If database is empty, automatically scan inbox on first grant
            if (dbHelper.getTotalCount() == 0) {
                readAndSyncDeviceSms();
            }
        }
    }

    private void showPermissionRationaleDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.permission_title)
                .setMessage(R.string.permission_rationale)
                .setPositiveButton(R.string.grant_permission, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        requestSmsPermission();
                    }
                })
                .setNegativeButton("Load Demo Instead", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        loadDemoData();
                    }
                })
                .show();
    }

    private void requestSmsPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.READ_SMS},
                PERMISSION_REQUEST_READ_SMS
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_READ_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                cardPermission.setVisibility(View.GONE);
                Toast.makeText(this, "Permission granted! Reading messages…", Toast.LENGTH_SHORT).show();
                readAndSyncDeviceSms();
            } else {
                cardPermission.setVisibility(View.VISIBLE);
                Toast.makeText(this, R.string.permission_denied_warning, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void readAndSyncDeviceSms() {
        progressBarSync.setVisibility(View.VISIBLE);
        tvSyncStatus.setText(R.string.syncing_messages);

        SMSReader.readDeviceSMS(this, dbHelper, new SMSReader.SMSReadCallback() {
            @Override
            public void onSuccess(int totalCount, int addedOrUpdatedCount) {
                progressBarSync.setVisibility(View.GONE);
                tvSyncStatus.setText("Synced " + totalCount + " messages from inbox");
                Toast.makeText(MainActivity.this, "Scan complete! Found " + totalCount + " messages.", Toast.LENGTH_SHORT).show();
                loadDashboardData();
            }

            @Override
            public void onError(String errorMessage) {
                progressBarSync.setVisibility(View.GONE);
                tvSyncStatus.setText("Scan failed");
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadDemoData() {
        progressBarSync.setVisibility(View.VISIBLE);
        tvSyncStatus.setText("Loading demo SMS…");

        SMSReader.insertSampleData(this, dbHelper, new SMSReader.SMSReadCallback() {
            @Override
            public void onSuccess(int totalCount, int addedOrUpdatedCount) {
                progressBarSync.setVisibility(View.GONE);
                tvSyncStatus.setText("Demo messages loaded successfully");
                Toast.makeText(MainActivity.this, "Loaded " + totalCount + " realistic demo messages across 5 categories!", Toast.LENGTH_SHORT).show();
                loadDashboardData();
            }

            @Override
            public void onError(String errorMessage) {
                progressBarSync.setVisibility(View.GONE);
                tvSyncStatus.setText("Failed to load demo");
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadDashboardData() {
        // 1. Update Category Counts
        int countBanking = dbHelper.getCategoryCount(SMSCategorizer.CATEGORY_BANKING);
        int countOtp = dbHelper.getCategoryCount(SMSCategorizer.CATEGORY_OTP);
        int countInfo = dbHelper.getCategoryCount(SMSCategorizer.CATEGORY_INFORMATION);
        int countPromo = dbHelper.getCategoryCount(SMSCategorizer.CATEGORY_PROMOTIONS);
        int countPersonal = dbHelper.getCategoryCount(SMSCategorizer.CATEGORY_PERSONAL);
        int total = dbHelper.getTotalCount();

        tvTotalCount.setText(String.valueOf(total));
        tvCountBanking.setText(String.valueOf(countBanking));
        tvCountOtp.setText(String.valueOf(countOtp));
        tvCountInfo.setText(String.valueOf(countInfo));
        tvCountPromo.setText(String.valueOf(countPromo));
        tvCountPersonal.setText(String.valueOf(countPersonal));

        // 2. Load Recent Messages
        loadRecentMessages();
    }

    private void loadRecentMessages() {
        List<Message> allMessages = dbHelper.getAllMessages();
        if (allMessages.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            tvEmptyMessage.setText("No messages yet. Click 'Refresh' to scan phone SMS, or tap 'Load Demo SMS' to test.");
            recyclerViewMessages.setVisibility(View.GONE);
            messageAdapter.updateList(allMessages);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            recyclerViewMessages.setVisibility(View.VISIBLE);
            messageAdapter.updateList(allMessages);
        }
    }

    private void performSearch(String query) {
        List<Message> results = dbHelper.searchMessages(query);
        if (results.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            tvEmptyMessage.setText("No messages matching \"" + query + "\"");
            recyclerViewMessages.setVisibility(View.GONE);
            messageAdapter.updateList(results);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            recyclerViewMessages.setVisibility(View.VISIBLE);
            messageAdapter.updateList(results);
        }
    }
}
