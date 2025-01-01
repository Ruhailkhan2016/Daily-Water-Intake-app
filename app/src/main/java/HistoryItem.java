package com.example.dailywaterintake;

public class HistoryItem {
    private String date;
    private int totalWater;

    public HistoryItem(String date, int totalWater) {
        this.date = date;
        this.totalWater = totalWater;
    }

    public String getDate() {
        return date;
    }

    public int getTotalWater() {
        return totalWater;
    }
}
