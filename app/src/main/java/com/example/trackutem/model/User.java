package com.example.trackutem.model;

public class User {
    private final String userId;
    private final String name;
    private final String email;
    private final String role;
//    private final String phone; // Only for driver

    public User(String userId, String name, String email, String role) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.role = role;
//        this.phone = phone;
    }

    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
//    public String getPhone() { return phone; }
}