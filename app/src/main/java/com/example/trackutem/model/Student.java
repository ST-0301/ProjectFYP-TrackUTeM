package com.example.trackutem.model;

public class Student {
    private final String studentId;
    private final String name;
    private final String email;
    private final String role;

    public Student(String studentId, String name, String email, String role) {
        this.studentId = studentId;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    public String getStudentId() { return studentId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
}