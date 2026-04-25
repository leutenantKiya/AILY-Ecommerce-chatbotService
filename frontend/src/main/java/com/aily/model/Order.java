package com.aily.model;

public class Order {
    public enum Status { PENDING, DIPROSES, DIKIRIM, SELESAI, DIBATALKAN }

    private String id;
    private Product product;
    private int quantity;
    private long total;
    private Status status;

    public Order(String id, Product product, int quantity, long total, Status status) {
        this.id       = id;
        this.product  = product;
        this.quantity = quantity;
        this.total    = total;
        this.status   = status;
    }

    public String  getId()       { return id; }
    public Product getProduct()  { return product; }
    public int     getQuantity() { return quantity; }
    public long    getTotal()    { return total; }
    public Status  getStatus()   { return status; }

    public void setStatus(Status status) { this.status = status; }

    public String formattedTotal() {
        return String.format("Rp %,d", total).replace(',', '.');
    }

    public String statusLabel() {
        return switch (status) {
            case PENDING  -> "Pending";
            case DIPROSES -> "Diproses";
            case DIKIRIM  -> "Dikirim";
            case SELESAI  -> "Selesai";
            case DIBATALKAN -> "Dibatalkan";
        };
    }

    public String statusCssClass() {
        return switch (status) {
            case PENDING  -> "status-pending";
            case DIPROSES -> "status-diproses";
            case DIKIRIM  -> "status-dikirim";
            case SELESAI  -> "status-selesai";
            case DIBATALKAN -> "status-pending";
        };
    }

    public boolean canCancel() {
        return status == Status.PENDING || status == Status.DIPROSES;
    }
}
