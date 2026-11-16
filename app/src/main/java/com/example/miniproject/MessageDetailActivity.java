package com.example.miniproject;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class MessageDetailActivity extends AppCompatActivity {

    private Message message;
    private DatabaseHelper dbHelper;

    private TextView tvSenderDetail;
    private TextView tvCategoryBadgeDetail;
    private TextView tvDateDetail;
    private TextView tvBodyDetail;
    private TextView tvClassificationReason;
    private ImageView imgCategoryIconDetail;
    private FrameLayout avatarFrameDetail;

    private ImageView btnBackDetail;
    private ImageView btnShareDetail;
    private ImageView btnDeleteDetail;
    private MaterialButton btnCopyText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_detail);

        dbHelper = new DatabaseHelper(this);

        message = (Message) getIntent().getSerializableExtra("EXTRA_MESSAGE");
        if (message == null) {
            Toast.makeText(this, "Message details unavailable", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        displayMessageDetails();
        setupListeners();
    }

    private void initViews() {
        tvSenderDetail = findViewById(R.id.tvSenderDetail);
        tvCategoryBadgeDetail = findViewById(R.id.tvCategoryBadgeDetail);
        tvDateDetail = findViewById(R.id.tvDateDetail);
        tvBodyDetail = findViewById(R.id.tvBodyDetail);
        tvClassificationReason = findViewById(R.id.tvClassificationReason);
        imgCategoryIconDetail = findViewById(R.id.imgCategoryIconDetail);
        avatarFrameDetail = findViewById(R.id.avatarFrameDetail);

        btnBackDetail = findViewById(R.id.btnBackDetail);
        btnShareDetail = findViewById(R.id.btnShareDetail);
        btnDeleteDetail = findViewById(R.id.btnDeleteDetail);
        btnCopyText = findViewById(R.id.btnCopyText);
    }

    private void displayMessageDetails() {
        tvSenderDetail.setText(message.getSender());
        tvDateDetail.setText(message.getFormattedDate());
        tvBodyDetail.setText(message.getBody());

        String category = message.getCategory();
        tvCategoryBadgeDetail.setText(category);

        int catColor = SMSCategorizer.getCategoryColor(this, category);
        int catBgColor = SMSCategorizer.getCategoryBgColor(this, category);

        // Customize Category Badge
        tvCategoryBadgeDetail.setTextColor(catColor);
        GradientDrawable chipBg = new GradientDrawable();
        chipBg.setColor(catBgColor);
        chipBg.setCornerRadius(18f);
        chipBg.setStroke(2, catColor);
        tvCategoryBadgeDetail.setBackground(chipBg);

        // Customize Avatar & Icon
        imgCategoryIconDetail.setImageResource(SMSCategorizer.getCategoryIconRes(category));
        imgCategoryIconDetail.setColorFilter(catColor);

        GradientDrawable avatarBg = new GradientDrawable();
        avatarBg.setShape(GradientDrawable.OVAL);
        avatarBg.setColor(catBgColor);
        avatarFrameDetail.setBackground(avatarBg);

        // Set Educational Classification Reason
        tvClassificationReason.setText(getClassificationExplanation(category));
    }

    private String getClassificationExplanation(String category) {
        switch (category) {
            case SMSCategorizer.CATEGORY_OTP:
                return "Classified as OTP because the message body contains high-priority authentication keywords (e.g. OTP, verification code, secret pin) or numerical verification tokens.";
            case SMSCategorizer.CATEGORY_BANKING:
                return "Classified as Banking because financial transaction identifiers (debited, credited, UPI, account number, available balance, or bank names) were identified.";
            case SMSCategorizer.CATEGORY_INFORMATION:
                return "Classified as Information because it contains event alerts, college notifications, exam schedules, appointments, or courier delivery tracking updates.";
            case SMSCategorizer.CATEGORY_PROMOTIONS:
                return "Classified as Promotions because marketing triggers, discount offers, coupons, cashback vouchers, or shopping deals were detected.";
            case SMSCategorizer.CATEGORY_PERSONAL:
            default:
                return "Classified as Personal because no transactional or corporate marketing patterns were detected, or the message is a direct personal conversation.";
        }
    }

    private void setupListeners() {
        btnBackDetail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnCopyText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyMessageToClipboard();
            }
        });

        btnShareDetail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareMessage();
            }
        });

        btnDeleteDetail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDelete();
            }
        });
    }

    private void copyMessageToClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("SMS Body", message.getBody());
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
        }
    }

    private void shareMessage() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "SMS from " + message.getSender());
        shareIntent.putExtra(Intent.EXTRA_TEXT, message.getBody());
        startActivity(Intent.createChooser(shareIntent, "Share SMS via"));
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Saved Record")
                .setMessage("Are you sure you want to remove this message from Smart SMS Categorizer?\n\nNote: Original SMS on your phone will NOT be deleted.")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dbHelper.deleteMessage(message.getId());
                        Toast.makeText(MessageDetailActivity.this, "Message removed from app database", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
