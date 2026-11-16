package com.example.miniproject;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests to verify the classification accuracy and priority ordering of SMSCategorizer.
 */
public class SMSCategorizerTest {

    @Test
    public void testOtpClassification() {
        // High priority OTP testing
        String body1 = "584920 is your Google verification code. Do not share your code with anyone.";
        assertEquals(SMSCategorizer.CATEGORY_OTP, SMSCategorizer.categorize("VK-GOOGLE", body1));

        String body2 = "Your OTP for login to netbanking is 849201. Valid for 5 minutes.";
        assertEquals(SMSCategorizer.CATEGORY_OTP, SMSCategorizer.categorize("HDFCBK", body2));

        String body3 = "Use authentication code 382910 to confirm your transaction.";
        assertEquals(SMSCategorizer.CATEGORY_OTP, SMSCategorizer.categorize("PAYTM", body3));
    }

    @Test
    public void testBankingClassification() {
        String body1 = "Dear Customer, INR 2,450.00 debited from A/C XX4921 on 16-Nov-26 via UPI Ref 6291038491. Avail Bal: INR 18,340.50.";
        assertEquals(SMSCategorizer.CATEGORY_BANKING, SMSCategorizer.categorize("HDFCBK", body1));

        String body2 = "Your A/C ending 3829 is credited with Rs 25,000.00 by Salary NEFT transfer. Available Bal: Rs 42,150.00.";
        assertEquals(SMSCategorizer.CATEGORY_BANKING, SMSCategorizer.categorize("SBIINB", body2));

        String body3 = "Txn of INR 850.00 spent on ICICI Bank Card ending 1092 at STARBUCKS. Avail Limit: INR 85,000.";
        assertEquals(SMSCategorizer.CATEGORY_BANKING, SMSCategorizer.categorize("ICICIB", body3));
    }

    @Test
    public void testInformationClassification() {
        String body1 = "Reminder: Semester end examinations commence from 24th Nov. Download your hall ticket from the student portal.";
        assertEquals(SMSCategorizer.CATEGORY_INFORMATION, SMSCategorizer.categorize("VK-COLLEGE", body1));

        String body2 = "Out for Delivery: Your package with tracking ID FMPC009182 will be delivered today by courier agent Rajesh.";
        assertEquals(SMSCategorizer.CATEGORY_INFORMATION, SMSCategorizer.categorize("EKART", body2));

        String body3 = "Appointment confirmed with Dr. Sharma for tomorrow at 10:30 AM at Apollo Clinic.";
        assertEquals(SMSCategorizer.CATEGORY_INFORMATION, SMSCategorizer.categorize("APLMED", body3));
    }

    @Test
    public void testPromotionsClassification() {
        String body1 = "Flash Deal! Flat 50% discount on orders above Rs. 299. Use promo coupon TREAT50. Hurry, offer valid today only!";
        assertEquals(SMSCategorizer.CATEGORY_PROMOTIONS, SMSCategorizer.categorize("DOMINOS", body1));

        String body2 = "Mega Fashion Sale is LIVE! Flat 40% to 70% off on top brands + 10% cashback. Shop now and save big!";
        assertEquals(SMSCategorizer.CATEGORY_PROMOTIONS, SMSCategorizer.categorize("MYNTRA", body2));

        String body3 = "Craving biryani? Enjoy up to Rs 120 off + free delivery on top rated restaurants today!";
        assertEquals(SMSCategorizer.CATEGORY_PROMOTIONS, SMSCategorizer.categorize("ZOMATO", body3));
    }

    @Test
    public void testPersonalClassification() {
        String body1 = "Hey, are we still meeting at the library at 4 PM to work on our MAD project?";
        assertEquals(SMSCategorizer.CATEGORY_PERSONAL, SMSCategorizer.categorize("+919876543210", body1));

        String body2 = "Mom called, please pick up milk and bread on your way back home.";
        assertEquals(SMSCategorizer.CATEGORY_PERSONAL, SMSCategorizer.categorize("+919123456789", body2));

        String body3 = "Thanks for sharing the notes! Let's discuss module 3 tomorrow morning.";
        assertEquals(SMSCategorizer.CATEGORY_PERSONAL, SMSCategorizer.categorize("+918889990001", body3));
    }

    @Test
    public void testOtpPriorityOverBanking() {
        // When a bank sends an OTP, OTP should win because it is an authentication message
        String body = "Dear Customer, 492019 is your OTP for transaction of INR 1,500.00 at Amazon. Do not share with anyone.";
        assertEquals(SMSCategorizer.CATEGORY_OTP, SMSCategorizer.categorize("HDFCBK", body));
    }
}
