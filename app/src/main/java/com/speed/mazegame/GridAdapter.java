package com.speed.mazegame;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GridAdapter extends BaseAdapter {

    private int[] cells;
    private Context context;

    public GridAdapter(int[] cells, Context context) {
        this.cells = cells;
        this.context = context;
    }

    public void submitList(int[] cells){
        Log.d("kesD", "list submitted");
        this.cells = cells;
    }

    public List<Integer> getCells(){
        List<Integer> cells = new ArrayList<>();
        for (int cell:this.cells){
            cells.add(cell);
        }
        return cells;
    }

    @Override
    public int getCount() {
        return cells.length;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int cell = cells[position];
        return new CellViewSpace(context,cell);
    }
}
