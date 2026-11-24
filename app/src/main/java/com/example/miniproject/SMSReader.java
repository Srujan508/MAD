package com.example.miniproject;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Telephony;
import androidx.core.content.ContextCompat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SMSReader handles reading SMS messages from the Android Telephony Content Provider
 * in a background worker thread. It categorizes each message and inserts it into
 * the SQLite database without freezing the UI.
 */
public class SMSReader {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final Random RANDOM = new Random();

    private static String formatDate(long dateMillis) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        return dateFormat.format(new Date(dateMillis));
    }

    public interface SMSReadCallback {
        void onSuccess(int totalCount, int addedOrUpdatedCount);
        void onError(String errorMessage);
    }

    /**
     * Checks if the app has READ_SMS permission.
     */
    public static boolean hasSmsPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Reads all SMS from inbox, classifies each, and persists to DatabaseHelper.
     * Executes asynchronously on a background thread.
     */
    public static void readDeviceSMS(final Context context,
                                     final DatabaseHelper dbHelper,
                                     final SMSReadCallback callback) {

        if (!hasSmsPermission(context)) {
            if (callback != null) {
                callback.onError("READ_SMS permission is not granted.");
            }
            return;
        }

        EXECUTOR.execute(() -> {
            int totalCount = 0;
            int addedOrUpdated = 0;

            try {
                ContentResolver contentResolver = context.getContentResolver();
                Uri inboxUri = Telephony.Sms.Inbox.CONTENT_URI;

                // Projection of required columns
                String[] projection = new String[]{
                        Telephony.Sms._ID,
                        Telephony.Sms.ADDRESS,
                        Telephony.Sms.BODY,
                        Telephony.Sms.DATE
                };

                Cursor cursor = contentResolver.query(
                        inboxUri,
                        projection,
                        null,
                        null,
                        Telephony.Sms.DATE + " DESC"
                );

                if (cursor != null) {
                    int idIndex = cursor.getColumnIndex(Telephony.Sms._ID);
                    int addressIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS);
                    int bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY);
                    int dateIndex = cursor.getColumnIndex(Telephony.Sms.DATE);

                    while (cursor.moveToNext()) {
                        totalCount++;
                        String smsId = idIndex != -1 ? cursor.getString(idIndex) : ("" + totalCount);
                        String sender = addressIndex != -1 ? cursor.getString(addressIndex) : "Unknown";
                        String body = bodyIndex != -1 ? cursor.getString(bodyIndex) : "";
                        long dateMillis = dateIndex != -1 ? cursor.getLong(dateIndex) : System.currentTimeMillis();

                        if (sender == null || sender.trim().isEmpty()) {
                            sender = "Unknown";
                        }
                        if (body == null) {
                            body = "";
                        }

                        String formattedDate = formatDate(dateMillis);

                        // 1. Run Classification Algorithm
                        String category = SMSCategorizer.categorize(sender, body);

                        // 2. Create Message model
                        Message message = new Message(smsId, sender, body, dateMillis, formattedDate, category);

                        // 3. Save to local SQLite database
                        boolean inserted = dbHelper.insertOrUpdateMessage(message);
                        if (inserted) {
                            addedOrUpdated++;
                        }
                    }
                    cursor.close();
                }

                final int finalTotal = totalCount;
                final int finalAdded = addedOrUpdated;
                MAIN_HANDLER.post(() -> {
                    if (callback != null) {
                        callback.onSuccess(finalTotal, finalAdded);
                    }
                });

            } catch (final Exception e) {
                MAIN_HANDLER.post(() -> {
                    if (callback != null) {
                        callback.onError("Failed to read messages: " + e.getMessage());
                    }
                });
            }
        });
    }

    /**
     * Inserts realistic, dynamically generated sample data across all five categories.
     * Each invocation generates fresh, unique demo messages so every click yields new messages.
     */
    @SuppressWarnings("unused")
    public static void insertSampleData(final Context context,
                                        final DatabaseHelper dbHelper,
                                        final SMSReadCallback callback) {

        EXECUTOR.execute(() -> {
            long now = System.currentTimeMillis();

            // Dynamic demo message templates per category
            String[][] generatedSamples = generateRandomDemoBatch(now);

            int count = 0;
            for (String[] sample : generatedSamples) {
                String sender = sample[0];
                String body = sample[1];
                long time = Long.parseLong(sample[2]);
                String uniqueId = "demo_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 6);
                String formattedDate = formatDate(time);
                String category = SMSCategorizer.categorize(sender, body);

                Message message = new Message(uniqueId, sender, body, time, formattedDate, category);
                if (dbHelper.insertOrUpdateMessage(message)) {
                    count++;
                }
            }

            final int finalCount = count;
            final int batchSize = generatedSamples.length;
            MAIN_HANDLER.post(() -> {
                if (callback != null) {
                    callback.onSuccess(batchSize, finalCount);
                }
            });
        });
    }

    /**
     * Generates a randomized batch of 5 distinct demo messages across all categories.
     */
    private static String[][] generateRandomDemoBatch(long baseTime) {
        // 1. OTP Sample
        String[] otpSenders = {"VK-GOOGLE", "AD-AMAZON", "VM-SWIGGY", "AX-PAYTM", "JM-JIO", "UB-UBER"};
        String selectedOtpSender = otpSenders[RANDOM.nextInt(otpSenders.length)];
        int otpCode = 100000 + RANDOM.nextInt(900000);
        String otpBody;
        switch (RANDOM.nextInt(4)) {
            case 0:
                otpBody = otpCode + " is your " + selectedOtpSender + " verification code. Do not share your code with anyone.";
                break;
            case 1:
                otpBody = "Your " + selectedOtpSender + " OTP is " + otpCode + ". Use code to verify your account login. Valid for 10 minutes.";
                break;
            case 2:
                otpBody = "Your verification code for " + selectedOtpSender + " login is " + otpCode + ". Do not disclose this secret code.";
                break;
            default:
                otpBody = otpCode + " is the secret PIN to authorize your transaction on " + selectedOtpSender + ".";
                break;
        }

        // 2. Banking Sample
        String[] bankSenders = {"HDFCBK", "SBIINB", "ICICIB", "AXISBK", "KOTAKB", "PNBANK"};
        String selectedBankSender = bankSenders[RANDOM.nextInt(bankSenders.length)];
        int accountEnding = 1000 + RANDOM.nextInt(9000);
        double debitAmount = 100.0 + (RANDOM.nextInt(450) * 50.0);
        double balance = 5000.0 + (RANDOM.nextInt(500) * 100.0);
        long refNum = 6000000000L + RANDOM.nextInt(900000000);
        String bankBody;
        if (RANDOM.nextBoolean()) {
            bankBody = "Dear Customer, INR " + String.format(Locale.US, "%.2f", debitAmount) +
                    " debited from A/C XX" + accountEnding + " via UPI Ref " + refNum + ". Avail Bal: INR " +
                    String.format(Locale.US, "%.2f", balance) + ".";
        } else {
            bankBody = "Your A/C ending " + accountEnding + " is credited with Rs " +
                    String.format(Locale.US, "%.2f", debitAmount * 2) + " by Salary NEFT transfer. Available Bal: Rs " +
                    String.format(Locale.US, "%.2f", balance + debitAmount * 2) + ".";
        }

        // 3. Information Sample
        String[][] infoTemplates = {
                {"VK-COLLEGE", "Reminder: Semester end examinations commence from " + (15 + RANDOM.nextInt(10)) + "th Nov. Download your hall ticket from student portal."},
                {"EKART", "Out for Delivery: Your package with tracking ID FMPC" + (100000 + RANDOM.nextInt(900000)) + " will be delivered today by courier agent."},
                {"APLMED", "Appointment confirmed with Dr. Sharma for tomorrow at " + (9 + RANDOM.nextInt(5)) + ":30 AM at Apollo Clinic."},
                {"IRCTC", "PNR " + (8000000000L + RANDOM.nextInt(900000000)) + " Train 12626: Your booking status is CONFIRMED, Coach B" + (1 + RANDOM.nextInt(5)) + "."},
                {"BESCOM", "Electricity Bill Alert: Rs " + (450 + RANDOM.nextInt(1500)) + " for Consumer ID " + (70000 + RANDOM.nextInt(20000)) + " is due next week."}
        };
        String[] selectedInfo = infoTemplates[RANDOM.nextInt(infoTemplates.length)];

        // 4. Promotions Sample
        int discountPct = 10 * (2 + RANDOM.nextInt(6)); // 20%, 30%, ... 70%
        String[][] promoTemplates = {
                {"DOMINOS", "Flash Deal! Flat " + discountPct + "% discount on orders above Rs. 299. Use promo coupon TREAT" + discountPct + ". Hurry!"},
                {"MYNTRA", "Mega Fashion Sale is LIVE! Flat " + discountPct + "% off on top brands + 10% cashback. Shop now and save big!"},
                {"ZOMATO", "Craving biryani? Enjoy up to Rs " + (50 + discountPct * 2) + " off + free delivery on top rated restaurants today!"},
                {"FLIPKART", "Big Sale Days sneak peek! Extra " + discountPct + "% off on electronics with code FLASH" + discountPct + "."},
                {"BLINKIT", "Get 10 min grocery delivery! Flat Rs " + ( discountPct * 2) + " cashback on orders above Rs 499."}
        };
        String[] selectedPromo = promoTemplates[RANDOM.nextInt(promoTemplates.length)];

        // 5. Personal Sample
        String[] personalSenders = {"+919876543210", "+919123456789", "+918889990001", "+919711223344", "+919445566778"};
        String selectedPersonalSender = personalSenders[RANDOM.nextInt(personalSenders.length)];
        String[] personalTexts = {
                "Hey, are we still meeting at the library at " + (2 + RANDOM.nextInt(4)) + " PM to work on our MAD project?",
                "Mom called, please pick up milk and snacks on your way back home.",
                "Thanks for sharing the notes for module " + (1 + RANDOM.nextInt(4)) + "! Let's discuss tomorrow.",
                "Can you send me the location link for today's venue?",
                "Happy Birthday! Hope you have a wonderful day ahead!"
        };
        String selectedPersonalBody = personalTexts[RANDOM.nextInt(personalTexts.length)];

        // Random offsets so timestamps are spaced out realistically
        long time1 = baseTime - (5L + RANDOM.nextInt(15)) * 60 * 1000;
        long time2 = baseTime - (30L + RANDOM.nextInt(60)) * 60 * 1000;
        long time3 = baseTime - (2L + RANDOM.nextInt(5)) * 3600 * 1000;
        long time4 = baseTime - (6L + RANDOM.nextInt(10)) * 3600 * 1000;
        long time5 = baseTime - (12L + RANDOM.nextInt(24)) * 3600 * 1000;

        return new String[][]{
                {selectedOtpSender, otpBody, String.valueOf(time1)},
                {selectedBankSender, bankBody, String.valueOf(time2)},
                {selectedInfo[0], selectedInfo[1], String.valueOf(time3)},
                {selectedPromo[0], selectedPromo[1], String.valueOf(time4)},
                {selectedPersonalSender, selectedPersonalBody, String.valueOf(time5)}
        };
    }
}
