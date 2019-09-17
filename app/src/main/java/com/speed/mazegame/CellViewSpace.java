package com.speed.mazegame;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

public class CellViewSpace extends View {

    public static final int WALL=0, SPACE=1, FINISH=2;
    public static final int PLAYER1 = 101, PLAYER2 = 102, BOTHPLAYERS = 103, TRANSPARENT = 999;

    public CellViewSpace(Context context, int cellType) {
        super(context);
        Drawable background;
        switch (cellType) {
            case WALL:
                background = context.getDrawable(R.drawable.wall_cell_drawable);
                break;
            case SPACE:
                background = context.getDrawable(R.drawable.space_cell_drawable);
                break;
            case FINISH:
                background = context.getDrawable(R.drawable.finish_cell_drawable);
                break;
            case TRANSPARENT:
                background = context.getDrawable(R.drawable.transparent);
                break;
            case PLAYER1:
                background = context.getDrawable(R.drawable.player_1_cell);
                break;
            case PLAYER2:
                background = context.getDrawable(R.drawable.player_2_cell);
                break;
            case BOTHPLAYERS:
                background = context.getDrawable(R.drawable.both_players_cell);
                break;
            default:
                background = null;
        }

        setBackground(background);
    }
}
