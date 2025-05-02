package com.example.trackutem.model;

public class Driver {
    private String driverId;
    private String name;
    private String email;
    private String password;
    private String phone;
    private String licenseNumber;

    // Required no-argument constructor for Firestore
    public Driver() {}
    public Driver(String driverId, String name, String email, String password, String phone, String licenseNumber) {
        this.driverId = driverId;
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.licenseNumber = licenseNumber;
    }

    // Getters
    public String getDriverId() { return driverId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getPhone() { return phone; }
    public String getLicenseNumber() { return licenseNumber; }

    // Setters
    public void setDriverId(String driverId) { this.driverId = driverId; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }
}