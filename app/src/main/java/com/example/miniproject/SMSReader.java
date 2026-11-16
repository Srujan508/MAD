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
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

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

        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                int totalProcessed = 0;
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
                            totalProcessed++;
                            String smsId = idIndex != -1 ? cursor.getString(idIndex) : String.valueOf(totalProcessed);
                            String sender = addressIndex != -1 ? cursor.getString(addressIndex) : "Unknown";
                            String body = bodyIndex != -1 ? cursor.getString(bodyIndex) : "";
                            long dateMillis = dateIndex != -1 ? cursor.getLong(dateIndex) : System.currentTimeMillis();

                            if (sender == null || sender.trim().isEmpty()) {
                                sender = "Unknown";
                            }
                            if (body == null) {
                                body = "";
                            }

                            String formattedDate = DATE_FORMAT.format(new Date(dateMillis));

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

                    final int finalTotal = totalProcessed;
                    final int finalAdded = addedOrUpdated;
                    MAIN_HANDLER.post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) {
                                callback.onSuccess(finalTotal, finalAdded);
                            }
                        }
                    });

                } catch (final Exception e) {
                    MAIN_HANDLER.post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) {
                                callback.onError("Failed to read messages: " + e.getMessage());
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * Inserts realistic sample data across all five categories.
     * Useful for testing in Android Studio Emulator or devices with empty inboxes.
     */
    public static void insertSampleData(final Context context,
                                        final DatabaseHelper dbHelper,
                                        final SMSReadCallback callback) {

        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                long hour = 3600 * 1000;
                long day = 24 * hour;

                // Sample messages designed specifically to demonstrate all 5 categories
                String[][] sampleMessages = {
                        // 1. OTP Messages
                        {"VK-GOOGLE", "584920 is your Google verification code. Do not share your code with anyone.", String.valueOf(now - 10 * 60 * 1000)},
                        {"AD-AMAZON", "Your Amazon OTP is 739201. Use code to verify your account login. Valid for 10 minutes.", String.valueOf(now - 45 * 60 * 1000)},
                        {"VM-SWIGGY", "Your verification code for Swiggy login is 4291. Do not disclose this secret code.", String.valueOf(now - 3 * hour)},

                        // 2. Banking Messages
                        {"HDFCBK", "Dear Customer, INR 2,450.00 debited from A/C XX4921 on 16-Nov-26 via UPI Ref 6291038491. Avail Bal: INR 18,340.50.", String.valueOf(now - 1 * hour)},
                        {"SBIINB", "Your A/C ending 3829 is credited with Rs 25,000.00 on 15-Nov-26 by Salary NEFT transfer. Available Bal: Rs 42,150.00.", String.valueOf(now - 1 * day)},
                        {"ICICIB", "Txn of INR 850.00 spent on ICICI Bank Card ending 1092 at STARBUCKS. Avail Limit: INR 85,000.", String.valueOf(now - 2 * day)},

                        // 3. Information Messages
                        {"VK-COLLEGE", "Reminder: Semester end examinations commence from 24th Nov. Download your hall ticket from the student portal.", String.valueOf(now - 4 * hour)},
                        {"EKART", "Out for Delivery: Your package with tracking ID FMPC009182 will be delivered today by courier agent Rajesh.", String.valueOf(now - 6 * hour)},
                        {"APLMED", "Appointment confirmed with Dr. Sharma for tomorrow at 10:30 AM at Apollo Clinic.", String.valueOf(now - 1 * day - 2 * hour)},

                        // 4. Promotions Messages
                        {"DOMINOS", "Flash Deal! Flat 50% discount on orders above Rs. 299. Use promo coupon TREAT50. Hurry, offer valid today only!", String.valueOf(now - 5 * hour)},
                        {"MYNTRA", "Mega Fashion Sale is LIVE! Flat 40% to 70% off on top brands + 10% cashback. Shop now and save big!", String.valueOf(now - 8 * hour)},
                        {"ZOMATO", "Craving biryani? Enjoy up to Rs 120 off + free delivery on top rated restaurants today!", String.valueOf(now - 2 * day)},

                        // 5. Personal Messages
                        {"+919876543210", "Hey, are we still meeting at the library at 4 PM to work on our MAD project?", String.valueOf(now - 2 * hour)},
                        {"+919123456789", "Mom called, please pick up milk and bread on your way back home.", String.valueOf(now - 5 * hour)},
                        {"+918889990001", "Thanks for sharing the notes! Let's discuss module 3 tomorrow morning.", String.valueOf(now - 1 * day)}
                };

                int count = 0;
                for (int i = 0; i < sampleMessages.length; i++) {
                    String sender = sampleMessages[i][0];
                    String body = sampleMessages[i][1];
                    long time = Long.parseLong(sampleMessages[i][2]);
                    String formattedDate = DATE_FORMAT.format(new Date(time));
                    String category = SMSCategorizer.categorize(sender, body);

                    Message message = new Message("sample_" + (i + 1), sender, body, time, formattedDate, category);
                    if (dbHelper.insertOrUpdateMessage(message)) {
                        count++;
                    }
                }

                final int finalCount = count;
                MAIN_HANDLER.post(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.onSuccess(sampleMessages.length, finalCount);
                        }
                    }
                });
            }
        });
    }
}
