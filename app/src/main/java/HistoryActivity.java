package com.example.dailywaterintake;

import android.database.Cursor;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private ArrayList<HistoryItem> historyList;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        recyclerView = findViewById(R.id.recyclerViewHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        databaseHelper = new DatabaseHelper(this);
        historyList = new ArrayList<>();

        // Load data from database
        Cursor cursor = databaseHelper.getWaterHistory();
        if (cursor.moveToFirst()) {
            do {
                String date = cursor.getString(cursor.getColumnIndex("date"));
                int totalWater = cursor.getInt(cursor.getColumnIndex("totalWater"));
                historyList.add(new HistoryItem(date, totalWater));
            } while (cursor.moveToNext());
        }
        cursor.close();

        // Set up RecyclerView
        adapter = new HistoryAdapter(historyList);
        recyclerView.setAdapter(adapter);
    }
}
