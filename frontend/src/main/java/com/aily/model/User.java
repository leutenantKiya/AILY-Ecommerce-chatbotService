package com.aily.model;

public class User {
    private String id;        // hashed password, dipakai sebagai session token
    private String username;
    private String email;
    private String phone;
    private String address;
    private String role;

    public User(String id, String username, String email,
                String phone, String address, String role) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.role = role;
    }

    public String getId()       { return id; }
    public String getUsername() { return username; }
    public String getEmail()    { return email; }
    public String getPhone()    { return phone; }
    public String getAddress()  { return address; }
    public String getRole()     { return role; }

    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(role);
    }
}
