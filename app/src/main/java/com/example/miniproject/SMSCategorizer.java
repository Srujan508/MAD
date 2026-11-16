package com.example.miniproject;

import android.content.Context;
import androidx.core.content.ContextCompat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SMSCategorizer is the classification engine for the application.
 * It uses a prioritized keyword and pattern matching algorithm to determine
 * which category an SMS message belongs to:
 * 1. OTP (Highest Priority - Authentication & security codes)
 * 2. Banking (Debits, credits, account updates, transactions)
 * 3. Promotions (Commercial offers, discounts, coupons, sales)
 * 4. Information (Educational alerts, delivery tracking, appointments, official notices)
 * 5. Personal (Default / Direct mobile contacts)
 */
public class SMSCategorizer {

    // Category Name Constants
    public static final String CATEGORY_BANKING = "Banking";
    public static final String CATEGORY_OTP = "OTP";
    public static final String CATEGORY_INFORMATION = "Information";
    public static final String CATEGORY_PROMOTIONS = "Promotions";
    public static final String CATEGORY_PERSONAL = "Personal";

    // Regex to detect standard 10 to 12 digit personal phone numbers
    private static final Pattern PERSONAL_PHONE_PATTERN =
            Pattern.compile("^(\\+?\\d{1,3})?[6-9]\\d{9}$");

    // 1. OTP Keywords & Patterns
    private static final String[] OTP_KEYWORDS = {
            "otp", "one time password", "verification code", "security code",
            "login code", "auth code", "secret code", "passcode",
            "is your code", "use code", "do not share", "do not disclose",
            "valid for", "verification pin", "temporary pin", "confirm your login",
            "is your verification", "authentication code", "confirmation code"
    };

    private static final Pattern OTP_PATTERN = Pattern.compile(
            "(?i)\\b(otp|code|pin|password|secret)\\b.*\\b\\d{4,8}\\b|\\b\\d{4,8}\\b.*\\b(otp|code|pin|verification)\\b"
    );

    // 3. Promotions Keywords (Commercial offers, discounts, coupons)
    private static final String[] PROMO_KEYWORDS = {
            "offer", "discount", "sale", "cashback", "win", "winner", "coupon",
            "promo", "off on", "deal", "hurry", "limited period", "limited time",
            "flat 50%", "flat 40%", "flat 30%", "flat 20%", "subscribe", "shopping",
            "voucher", "claim", "bonus", "shop now", "buy 1 get 1", "bogo",
            "save up to", "mega sale", "exclusive offer", "apply code", "festive sale",
            "scratch card", "reward points", "special offer", "avail now", "free delivery"
    };

    // 4. Information Keywords (College notices, tracking, appointments, official updates)
    private static final String[] INFO_KEYWORDS = {
            "college", "exam", "hall ticket", "timetable", "university", "result",
            "grades", "appointment", "doctor", "clinic", "hospital", "delivery",
            "courier", "shipped", "out for delivery", "dispatched", "arriving",
            "tracking", "track your", "order id", "alert", "reminder", "notice",
            "scheduled meeting", "class cancelled", "attendance", "electricity bill",
            "water bill", "due date", "flight", "boarding", "train", "pnr",
            "interview", "webinar", "seminar", "ticket confirmed", "service request"
    };

    /**
     * Categorizes a message based on its sender and body.
     *
     * @param sender The sender's phone number or header (e.g. "HDFCBK", "+919876543210")
     * @param body   The text content of the SMS
     * @return One of: "OTP", "Banking", "Information", "Promotions", "Personal"
     */
    public static String categorize(String sender, String body) {
        if (body == null || body.trim().isEmpty()) {
            return CATEGORY_PERSONAL;
        }

        String lowerBody = body.toLowerCase(Locale.ROOT);
        String cleanSender = sender != null ? sender.trim() : "";
        String lowerSender = cleanSender.toLowerCase(Locale.ROOT);

        // PRIORITY 1: OTP (Highest priority - time-critical verification codes)
        if (isOtp(lowerBody)) {
            return CATEGORY_OTP;
        }

        // PRIORITY 2: Banking (Real account debits, credits, balances)
        if (isBanking(lowerSender, lowerBody)) {
            return CATEGORY_BANKING;
        }

        // Check if message is from a personal mobile phone number without promotional triggers
        boolean isPersonalNumber = isPersonalSenderNumber(cleanSender);

        // PRIORITY 3: Promotions
        if (isPromotions(lowerBody) && !isPersonalNumber) {
            return CATEGORY_PROMOTIONS;
        }

        // PRIORITY 4: Information
        if (isInformation(lowerBody) && !isPersonalNumber) {
            return CATEGORY_INFORMATION;
        }

        // PRIORITY 5: Personal (Default or direct contacts)
        return CATEGORY_PERSONAL;
    }

    /**
     * Checks if sender is a personal mobile number format (e.g. +919876543210, 9876543210).
     */
    private static boolean isPersonalSenderNumber(String sender) {
        if (sender == null || sender.isEmpty()) return false;
        String digitsOnly = sender.replaceAll("[^0-9+]", "");
        return PERSONAL_PHONE_PATTERN.matcher(digitsOnly).matches();
    }

    /**
     * Checks if the message content indicates an OTP.
     */
    private static boolean isOtp(String lowerBody) {
        for (String keyword : OTP_KEYWORDS) {
            if (lowerBody.contains(keyword)) {
                return true;
            }
        }

        // Regex check for numeric code alongside keywords
        Matcher matcher = OTP_PATTERN.matcher(lowerBody);
        return matcher.find();
    }

    /**
     * Checks if the message indicates a real banking transaction or balance update.
     */
    private static boolean isBanking(String lowerSender, String lowerBody) {
        // Strong banking action indicators
        boolean hasBankAction = lowerBody.contains("debited") || lowerBody.contains("credited") ||
                lowerBody.contains("withdrawn") || lowerBody.contains("transferred") ||
                lowerBody.contains("deposited") || lowerBody.contains("spent on") ||
                lowerBody.contains("payment received") || lowerBody.contains("payment of rs") ||
                lowerBody.contains("avail bal") || lowerBody.contains("available bal") ||
                lowerBody.contains("card ending") || lowerBody.contains("ac no") ||
                lowerBody.contains("a/c") || lowerBody.contains("upi ref") ||
                lowerBody.contains("salary credited");

        // Known bank sender headers (e.g. HDFCBK, SBIINB, ICICIB, AXISBK, etc.)
        if (lowerSender.contains("bank") || lowerSender.contains("hdfc") ||
                lowerSender.contains("sbi") || lowerSender.contains("icici") ||
                lowerSender.contains("axis") || lowerSender.contains("pnb") ||
                lowerSender.contains("kotak")) {
            return hasBankAction || lowerBody.contains("bal:") || lowerBody.contains("balance");
        }

        return hasBankAction;
    }

    /**
     * Checks if the message indicates a promotional or marketing message.
     */
    private static boolean isPromotions(String lowerBody) {
        for (String keyword : PROMO_KEYWORDS) {
            if (lowerBody.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the message indicates an informational alert or reminder.
     */
    private static boolean isInformation(String lowerBody) {
        for (String keyword : INFO_KEYWORDS) {
            if (lowerBody.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Helper to get category-specific primary color.
     */
    public static int getCategoryColor(Context context, String category) {
        if (category == null) return ContextCompat.getColor(context, R.color.category_personal);
        switch (category) {
            case CATEGORY_BANKING:
                return ContextCompat.getColor(context, R.color.category_banking);
            case CATEGORY_OTP:
                return ContextCompat.getColor(context, R.color.category_otp);
            case CATEGORY_INFORMATION:
                return ContextCompat.getColor(context, R.color.category_info);
            case CATEGORY_PROMOTIONS:
                return ContextCompat.getColor(context, R.color.category_promo);
            case CATEGORY_PERSONAL:
            default:
                return ContextCompat.getColor(context, R.color.category_personal);
        }
    }

    /**
     * Helper to get category-specific background tint color.
     */
    public static int getCategoryBgColor(Context context, String category) {
        if (category == null) return ContextCompat.getColor(context, R.color.category_personal_bg);
        switch (category) {
            case CATEGORY_BANKING:
                return ContextCompat.getColor(context, R.color.category_banking_bg);
            case CATEGORY_OTP:
                return ContextCompat.getColor(context, R.color.category_otp_bg);
            case CATEGORY_INFORMATION:
                return ContextCompat.getColor(context, R.color.category_info_bg);
            case CATEGORY_PROMOTIONS:
                return ContextCompat.getColor(context, R.color.category_promo_bg);
            case CATEGORY_PERSONAL:
            default:
                return ContextCompat.getColor(context, R.color.category_personal_bg);
        }
    }

    /**
     * Helper to get category icon drawable resource ID.
     */
    public static int getCategoryIconRes(String category) {
        if (category == null) return R.drawable.ic_personal;
        switch (category) {
            case CATEGORY_BANKING:
                return R.drawable.ic_bank;
            case CATEGORY_OTP:
                return R.drawable.ic_otp;
            case CATEGORY_INFORMATION:
                return R.drawable.ic_info;
            case CATEGORY_PROMOTIONS:
                return R.drawable.ic_promo;
            case CATEGORY_PERSONAL:
            default:
                return R.drawable.ic_personal;
        }
    }
}
