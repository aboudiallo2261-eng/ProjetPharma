package com.pharmacie.models.dto;

public class HistoriqueCADTO {
    private String date;
    private long ca;

    public HistoriqueCADTO() {}

    public HistoriqueCADTO(String date, long ca) {
        this.date = date;
        this.ca = ca;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public long getCa() { return ca; }
    public void setCa(long ca) { this.ca = ca; }
}
