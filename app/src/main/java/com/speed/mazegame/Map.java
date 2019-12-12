package com.speed.mazegame;

import android.graphics.Point;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Stack;

import static com.speed.mazegame.CellViewSpace.FINISH;
import static com.speed.mazegame.CellViewSpace.SPACE;
import static com.speed.mazegame.CellViewSpace.WALL;

public class Map {

    private static final String TAG = "KesD";

    private int height, width;
    public static final int UP = 0, RIGHT = 1, DOWN = 2, LEFT = 3, BACK = 4;

    public int finishPos;
    private Point currentPoint;
    private boolean end, fastestFound;

    private Stack<Integer> lastMoves;
    private List<Integer> quickestRoute;
    private int[] cells;
    private int[][] cells2d;

    public Map(int height, int width) {
        this.height = height;
        this.width = width;

        generateNewCells(height,width);
    }

    // assigns a new space cell to the specific position in the 2d array
    public void setSpace(int y, int x) {
        cells2d[y][x] = SPACE;
    }

    // finds the bottom-right most space cell, places finish point there.
    public void setFinish() {
        int countDown = (height * width) - 1;
        do {
            if (cells[countDown] == SPACE) {
                cells[countDown] = FINISH;
                finishPos = countDown;
                break;
            }
            countDown--;
        } while (countDown > 0);
    }

    public int getFinish() {
        return finishPos;
    }

    public int[] getCells() {
        return cells;
    }

    public int[] generateNewCells(int aHeight, int aWidth) {
        this.height = aHeight;
        this.width = aWidth;
        cells2d = new int[height][width];
        for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w++) {
                cells2d[h][w] = WALL;
            }
        }
        //start
        setSpace(1, 1);
        currentPoint = new Point(1, 1);
        lastMoves = new Stack<>();
        end = false;
        fastestFound = false;

        long time = System.currentTimeMillis();
        spaceCreation();
        Log.d(TAG, "generateNewCells: timeTaken: "+(System.currentTimeMillis()-time));

        //create space at the start to produce two clear paths
        if (height > 16) {
            for (int i = 2; i < 8; i++) {
                setSpace(1, i);
                setSpace(i, 1);
            }
        }

        this.cells = new int[height * width];
        int cellCount = 0;

        // adds the cells from the 2d array to a 1d array.
        for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w++) {
                this.cells[cellCount] = cells2d[h][w];
                cellCount++;
            }
        }

        setFinish();
        return this.cells;
    }

    private void spaceCreation(){
        do {
            List<Integer> directions = new ArrayList<>();
            for (int i=0;i<4;i++){
                // changes x & y depending on direction (i)
                int x = currentPoint.x;
                x = i==RIGHT? x+1 : i==LEFT? x-1 : x;
                int y = currentPoint.y;
                y = i==UP? y-1 : i==DOWN? y+1 : y;
                // checks if valid move, if so, add direction to list
                if((i%2 == 0?currentPoint.y:currentPoint.x)
                        != (i==1? width-1:i==2? height-1:0)
                        && (cells2d[y][x] == WALL)
                        && nextToThreeWalls(new Point(x,y),i))
                    directions.add(i);
            }
            if(directions.isEmpty()){
                if (lastMoves.empty()) end = true;
                else go(BACK);
            }else{
                go(directions.get(new Random().nextInt(directions.size())));
                setSpace(currentPoint.y, currentPoint.x);
            }
            //loops ends once every single possible route has been taken.
        } while (!end);
    }

    private void go(int dir){
        if(dir==BACK) dir = (lastMoves.pop() + 2) % 4;
        else lastMoves.push(dir);
        currentPoint.y += (dir % 2 != 0 ? 0 : dir == 0 ? -1 : 1);
        currentPoint.x += (dir % 2 == 0 ? 0 : dir == 1 ? 1 : -1);
        if(currentPoint.y == (height - 2) && currentPoint.x > (width -4) && !fastestFound){
            quickestRoute = new ArrayList<>(lastMoves);
            fastestFound = true;
        }
    }

    // checks if the specific cell is beside 3 wall cells.
    private boolean nextToThreeWalls(Point nextPos, int direction) {
        boolean[] booleans = {
                nextPos.y != height - 1 && cells2d[nextPos.y + 1][nextPos.x] == WALL,
                nextPos.x != 0 && cells2d[nextPos.y][nextPos.x - 1] == WALL,
                nextPos.y != 0 && cells2d[nextPos.y - 1][nextPos.x] == WALL,
                nextPos.x != width - 1 && cells2d[nextPos.y][nextPos.x + 1] == WALL};
        for (int i = 0; i < 4; i++) {
            if (i == direction) continue;
            if (!booleans[i]) return false;
        }
        return true;
    }

    public List<Integer> getQuickestRoute(){
        return quickestRoute;
    }
}
