package com.example.findmyworker;

import static com.example.findmyworker.FBRef.refCustomer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Customer {

    private String userId;
    private String userType;
    private String specialties; //for workers
    private String fixedLocation; //for workers
    private String email;
    private String name;
    private String phone;
    private String profilePictureUrl; // ← URL string from Firebase Storage
    private ArrayList<String> portfolioUrls; // ← Array of URL strings

    public Customer() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public Customer(String userId, String userType, String email) {
        this.userId = userId;
        this.userType = userType;
        this.email = email;
        this.name = null; //not relevant for now
        this.fixedLocation = null;
        this.name = null;
        this.phone = null;
    }

    public Customer(String userId, String userType, String email, String specialties, String name, String phone) {
        this.userId = userId;
        this.userType = userType;
        this.specialties = specialties;
        this.email = email;
        this.fixedLocation = null;
        this.name = name;
        this.phone = phone;
    } //used for workers they must have all took down fixed location for now

    public ArrayList<String> getPortfolioUrls() {
        return portfolioUrls;
    }

    public void setPortfolioUrls(ArrayList<String> portfolioUrls) {
        this.portfolioUrls = portfolioUrls;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getSpecialties() {
        return specialties;
    }

    public void setSpecialties(String specialties) {
        this.specialties = specialties;
    }

    public String getFixedLocation() {
        return fixedLocation;
    }

    public void setFixedLocation(String fixedLocation) {
        this.fixedLocation = fixedLocation;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }
}
