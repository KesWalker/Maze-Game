package com.speed.mazegame;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import static com.speed.mazegame.CellViewSpace.WALL;

public class PlayerGridAdapter extends BaseAdapter {

    private int[] cells;
    private int[] mapCells;
    private Context context;
    private int playerOnePos,playerTwoPos;
    private IEndOfGame iEndOfGameListener;
    private int localPlayerNum;

    public interface IEndOfGame{
        void finishReached();
    }

    public void setLocalPlayerNum(int num){
        localPlayerNum = num;
    }

    public PlayerGridAdapter(int[] cells, int[] mapCells, Activity activity, int width) {
        this.cells = cells;
        this.context = activity;
        this.mapCells = mapCells;
        this.iEndOfGameListener = (IEndOfGame) activity;
        this.localPlayerNum = CellViewSpace.PLAYER1;
        playerOnePos = width+1;
        cells[playerOnePos] = localPlayerNum;
    }

    @Override
    public int getCount() {
        return cells.length;
    }

    public int move(int direction){
        cells[playerOnePos] = CellViewSpace.TRANSPARENT;
        int newPos;
        switch (direction){
            case Map.UP:
                newPos = playerOnePos - 20;
                playerOnePos = mapCells[newPos]==WALL?playerOnePos:newPos;
                break;
            case Map.RIGHT:
                newPos = playerOnePos + 1;
                playerOnePos = mapCells[newPos]==WALL?playerOnePos:newPos;
                break;
            case Map.DOWN:
                newPos = playerOnePos+20;
                playerOnePos = mapCells[newPos]==WALL?playerOnePos:newPos;
                break;
            case Map.LEFT:
                newPos = playerOnePos -1;
                playerOnePos = mapCells[newPos]==WALL?playerOnePos:newPos;
                break;
        }

        Log.d("KesD", "move: playerPos: "+playerOnePos);

        cells[playerOnePos] = localPlayerNum;
        notifyDataSetChanged();
        if(Map.finishPos == playerOnePos){
            iEndOfGameListener.finishReached();
        }
        return playerOnePos;
    }

    public int moveSecondPlayer(int pos){
        cells[playerTwoPos] = CellViewSpace.TRANSPARENT;
        playerTwoPos = pos;
        if(playerTwoPos==playerOnePos){
            cells[playerTwoPos] = CellViewSpace.BOTHPLAYERS;
        }else{
            if(localPlayerNum == CellViewSpace.PLAYER1){
                cells[playerTwoPos] = CellViewSpace.PLAYER2;
                cells[playerOnePos] = CellViewSpace.PLAYER1;
            }else{
                cells[playerTwoPos] = CellViewSpace.PLAYER1;
                cells[playerOnePos] = CellViewSpace.PLAYER2;
            }
        }
        notifyDataSetChanged();
        if(Map.finishPos == pos){
            iEndOfGameListener.finishReached();
        }
        return pos;
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
        return new CellViewSpace(context,cells[position]);
    }
}
