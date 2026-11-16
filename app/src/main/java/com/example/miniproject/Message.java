package com.example.miniproject;

import java.io.Serializable;

/**
 * Model class representing an SMS message in the application.
 * Implements Serializable so it can be passed across Activities via Intent.
 */
public class Message implements Serializable {

    private int id;                 // Primary key in SQLite
    private String smsId;           // System SMS _id (to avoid duplicate imports)
    private String sender;          // Sender address or phone number
    private String body;            // Text content of the SMS
    private long dateMillis;        // Timestamp in milliseconds
    private String formattedDate;   // Human-readable date string
    private String category;        // Banking, OTP, Information, Promotions, Personal

    // Default constructor
    public Message() {
    }

    // Constructor with all fields
    public Message(int id, String smsId, String sender, String body, long dateMillis, String formattedDate, String category) {
        this.id = id;
        this.smsId = smsId;
        this.sender = sender;
        this.body = body;
        this.dateMillis = dateMillis;
        this.formattedDate = formattedDate;
        this.category = category;
    }

    // Constructor without SQLite autoincrement id
    public Message(String smsId, String sender, String body, long dateMillis, String formattedDate, String category) {
        this.smsId = smsId;
        this.sender = sender;
        this.body = body;
        this.dateMillis = dateMillis;
        this.formattedDate = formattedDate;
        this.category = category;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSmsId() {
        return smsId;
    }

    public void setSmsId(String smsId) {
        this.smsId = smsId;
    }

    public String getSender() {
        return sender != null ? sender : "Unknown";
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getBody() {
        return body != null ? body : "";
    }

    public void setBody(String body) {
        this.body = body;
    }

    public long getDateMillis() {
        return dateMillis;
    }

    public void setDateMillis(long dateMillis) {
        this.dateMillis = dateMillis;
    }

    public String getFormattedDate() {
        return formattedDate != null ? formattedDate : "";
    }

    public void setFormattedDate(String formattedDate) {
        this.formattedDate = formattedDate;
    }

    public String getCategory() {
        return category != null ? category : SMSCategorizer.CATEGORY_PERSONAL;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
