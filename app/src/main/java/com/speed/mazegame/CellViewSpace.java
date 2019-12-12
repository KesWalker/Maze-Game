package com.speed.mazegame;

import android.content.Context;
import android.view.View;

import java.util.HashMap;

public class CellViewSpace extends View {

    public static final int WALL=0, SPACE=1, FINISH=2;
    public static final int PLAYER1 = 101, PLAYER2 = 102, BOTHPLAYERS = 103, TRANSPARENT = 999;

    private static final HashMap cellDrawables = new HashMap<Integer,Integer>(){{
        put(WALL,R.drawable.wall_cell_drawable);
        put(SPACE,R.drawable.space_cell_drawable);
        put(FINISH,R.drawable.finish_cell_drawable);
        put(TRANSPARENT,R.drawable.transparent);
        put(PLAYER1,R.drawable.player_1_cell);
        put(PLAYER2,R.drawable.player_2_cell);
        put(BOTHPLAYERS,R.drawable.both_players_cell);
    }};

    public CellViewSpace(Context context, int cellType) {
        super(context);
        setBackground(context.getDrawable((Integer) cellDrawables.get(cellType)));
    }
}
