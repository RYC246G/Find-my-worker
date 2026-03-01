package com.example.findmyworker;

import java.util.ArrayList;

public class Customer {

    private String userId;
    private String userType;
    private String specialties; //for workers
    private double latitude; // for workers
    private double longitude; // for workers
    private String address; // for workers
    private double ranking; // for workers
    private String email;
    private String name;
    private String phone;
    private String profilePictureUrl;
    private ArrayList<String> portfolioUrls;

    public Customer() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public Customer(String userId, String userType, String email) {
        this.userId = userId;
        this.userType = userType;
        this.email = email;
    }

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

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getRanking() {
        return ranking;
    }

    public void setRanking(double ranking) {
        this.ranking = ranking;
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
