package com.speed.mazegame;

import java.util.List;

public class Multiplayer {
    private int onePos;
    private int twoPos;
    private List<Integer> cells;
    private int blocker;

    public Multiplayer() { }

    public int getOnePos() {
        return onePos;
    }

    public void setOnePos(int onePos) {
        this.onePos = onePos;
    }

    public int getTwoPos() {
        return twoPos;
    }

    public void setTwoPos(int twoPos) {
        this.twoPos = twoPos;
    }

    public List<Integer> getCells() {
        return cells;
    }

    public void setCells(List<Integer> cells) {
        this.cells = cells;
    }

    public int getBlocker() {
        return blocker;
    }

    public void setBlocker(int blocker) {
        this.blocker = blocker;
    }
}
