package com.aily.model;

public class Product {
    private String id;
    private String name;
    private String code;
    private long price;
    private int stock;
    private String description;
    private String image;

    public Product(String id, String name, String code, long price, int stock, String description, String image) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.price = price;
        this.stock = stock;
        this.description = description;
        this.image = image;
    }

    public String getId()          { return id; }
    public String getName()        { return name; }
    public String getCode()        { return code; }
    public long   getPrice()       { return price; }
    public int    getStock()       { return stock; }
    public String getDescription() { return description; }
    public String getImage()       { return image; }

    public void setName(String name)               { this.name = name; }
    public void setCode(String code)               { this.code = code; }
    public void setPrice(long price)               { this.price = price; }
    public void setStock(int stock)                { this.stock = stock; }
    public void setDescription(String description) { this.description = description; }
    public void setImage(String image)             { this.image = image; }

    public String formattedPrice() {
        return String.format("Rp %,d", price).replace(',', '.');
    }
}
