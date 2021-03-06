package com.speed.mazegame;

import java.util.List;

public class Mutliplayer {
    private int onePos;
    private int twoPos;
    private List<Integer> cells;
    private int blocker;

    public Mutliplayer() {
    }

    public Mutliplayer(int onePos, int twoPos, List<Integer> cells, int blocker) {
        this.onePos = onePos;
        this.twoPos = twoPos;
        this.cells = cells;
        this.blocker = blocker;
    }

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
