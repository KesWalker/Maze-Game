package com.speed.mazegame;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Toast;

import static com.speed.mazegame.CellViewSpace.SPACE;
import static com.speed.mazegame.CellViewSpace.WALL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Delayed;

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

    public boolean placeWall(int position){
        if(cells[position] == SPACE){
            cells[position] = WALL;
            notifyDataSetChanged();
            new Handler().postDelayed(() -> {
                cells[position]=SPACE;
                notifyDataSetChanged();
            },5000);
            return true;
        }
        return false;
    }
}
