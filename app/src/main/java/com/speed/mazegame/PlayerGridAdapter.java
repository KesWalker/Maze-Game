package com.speed.mazegame;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import static com.speed.mazegame.CellViewSpace.BOTHPLAYERS;
import static com.speed.mazegame.CellViewSpace.PLAYER1;
import static com.speed.mazegame.CellViewSpace.PLAYER2;
import static com.speed.mazegame.CellViewSpace.TRANSPARENT;
import static com.speed.mazegame.CellViewSpace.WALL;
import static com.speed.mazegame.Map.DOWN;
import static com.speed.mazegame.Map.RIGHT;
import static com.speed.mazegame.Map.UP;

public class PlayerGridAdapter extends BaseAdapter {

    private int[] cells;
    private int[] mapCells;
    private Context context;
    private int playerOnePos,playerTwoPos;
    private IEndOfGame iEndOfGameListener;
    private int localPlayerNum, finishPos;
    private int width;

    public interface IEndOfGame{
        void finishReached();
    }

    public void setLocalPlayerNum(int num){
        localPlayerNum = num;
    }

    public void resetPos(){
        playerOnePos = width+1;
        playerTwoPos = width+1;
        move(-1);
    }

    public PlayerGridAdapter(int finishPos, int[] mapCells, Activity activity, int width, int playerNum) {
        this.cells = new int[mapCells.length];
        for (int i=0;i<mapCells.length;i++){
            this.cells[i] = TRANSPARENT;
        }
        this.finishPos = finishPos;
        this.context = activity;
        this.mapCells = mapCells;
        this.iEndOfGameListener = (IEndOfGame) activity;
        this.localPlayerNum = playerNum;
        this.width = width;
        playerOnePos = width+1;
        cells[playerOnePos] = localPlayerNum;
    }

    @Override
    public int getCount() {
        return cells.length;
    }

    public int move(int direction){
        cells[playerOnePos] = TRANSPARENT;
        int newPos = playerOnePos + ((direction == UP)? -width : (direction == RIGHT)? 1 : (direction == DOWN)? width : -1);
        playerOnePos = mapCells[newPos] == WALL? playerOnePos:newPos;
        cells[playerOnePos] = localPlayerNum;
        notifyDataSetChanged();
        if(finishPos == playerOnePos){
            iEndOfGameListener.finishReached();
        }
        return playerOnePos;
    }

    public int moveSecondPlayer(int pos){
        cells[playerTwoPos] = TRANSPARENT;
        playerTwoPos = pos;
        if(playerTwoPos==playerOnePos){
            cells[playerTwoPos] = BOTHPLAYERS;
        }else{
            boolean localIsPlayerOne = localPlayerNum == PLAYER1;
            cells[playerTwoPos] = localIsPlayerOne? PLAYER2 : PLAYER1;
            cells[playerOnePos] = localIsPlayerOne? PLAYER1 : PLAYER2;
        }
        notifyDataSetChanged();
        if(finishPos == pos){
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
