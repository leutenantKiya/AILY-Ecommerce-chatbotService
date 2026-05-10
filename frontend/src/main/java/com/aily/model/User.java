package com.aily.model;

public class User {
    private String id;        // hashed password, dipakai sebagai session token
    private String username;
    private String email;
    private String phone;
    private String address;
    private String role;
    private String gender;

    public User(String id, String username, String email,
                String phone, String address, String role) {
        this(id, username, email, phone, address, role, "L");
    }

    public User(String id, String username, String email,
                String phone, String address, String role, String gender) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.role = role;
        this.gender = gender;
    }

    public String getId()       { return id; }
    public String getUsername() { return username; }
    public String getEmail()    { return email; }
    public String getPhone()    { return phone; }
    public String getAddress()  { return address; }
    public String getRole()     { return role; }
    public String getGender()   { return gender; }

    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email)       { this.email = email; }
    public void setPhone(String phone)       { this.phone = phone; }
    public void setAddress(String address)   { this.address = address; }
    public void setGender(String gender)     { this.gender = gender; }

    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(role);
    }
}
