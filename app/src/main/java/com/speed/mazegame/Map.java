package com.speed.mazegame;

import android.graphics.Point;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
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

    public static int finishPos;
    private Point currentPoint;
    private int lastMove = -1;
    private boolean end;

    private Stack<Integer> lastMoves;
    private int[] cells;
    private int[][] cells2d;

    public Map(int height, int width) {
        this.height = height;
        this.width = width;

        generateNewCells();
    }

    // assigns a new space cell to the specific position in the 2d array
    public void setSpace(int y, int x) {
        cells2d[y][x] = SPACE;
    }

    // finds the bottom-right most space cell, places finish point there.
    public void setFinish(){
        int countDown = (height*width)-1;
        do {
            if(cells[countDown]==SPACE){
                cells[countDown] = FINISH;
                finishPos = countDown;
                break;
            }
            countDown--;
        }while (countDown>0);
    }

    public int[] getCells() {
        return cells;
    }

    public int[] generateNewCells() {

        cells2d = new int[height][width];
        for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w++) {
                cells2d[h][w] = WALL;
            }
        }
        //start
        setSpace(1, 1);

        currentPoint = new Point(1, 1);
        int count = 0;
        lastMoves = new Stack<>();
        end = false;

        do {
            count++;

            // checks for borders around current point
            boolean borderUp = currentPoint.y == 0;
            boolean borderRight = currentPoint.x == this.width - 1;
            boolean borderDown = currentPoint.y == this.height - 1;
            boolean borderLeft = currentPoint.x == 0;

            // checks for walls around current point
            boolean wallUp = borderUp ? false : cells2d[currentPoint.y - 1][currentPoint.x]==WALL;
            boolean wallRight = borderRight ? false : cells2d[currentPoint.y][currentPoint.x + 1]==WALL;
            boolean wallDown = borderDown ? false : cells2d[currentPoint.y + 1][currentPoint.x]==WALL;
            boolean wallLeft = borderLeft ? false : cells2d[currentPoint.y][currentPoint.x - 1]==WALL;

            // checks that the next possible move has three walls either side of it
            // it excludes the cell of the current point, hence it checks 3 walls and not 4
            boolean canGoUp = wallUp ? nextToThreeWalls(new Point(currentPoint.x, currentPoint.y - 1), UP) : false;
            boolean canGoRight = wallRight ? nextToThreeWalls(new Point(currentPoint.x + 1, currentPoint.y), RIGHT) : false;
            boolean canGoDown = wallDown ? nextToThreeWalls(new Point(currentPoint.x, currentPoint.y + 1), DOWN) : false;
            boolean canGoLeft = wallLeft ? nextToThreeWalls(new Point(currentPoint.x - 1, currentPoint.y), LEFT) : false;

            // checks if there is a valid direction to go
            boolean canGo = canGoUp || canGoRight || canGoLeft || canGoDown;

            List<Integer> directions = new ArrayList<>();

            // adds all possible directions to a list
            if (canGoUp) {
                directions.add(UP);
            }
            if (canGoRight) {
                directions.add(RIGHT);
            }
            if (canGoDown) {
                directions.add(DOWN);
            }
            if (canGoLeft) {
                directions.add(LEFT);
            }
            // if it cant go anywhere new, it will just add this single direction to the list
            if (!canGo) {
                directions.add(BACK);
            }
            // randomly selects a direction from the list
            int direction = directions.get(new Random().nextInt(directions.size()));

            // direction is taken
            switch (direction) {
                case UP:
                    if (canGoUp) {
                        goUp();
                    }
                    break;
                case RIGHT:
                    if (canGoRight) {
                        goRight();
                    }
                    break;
                case DOWN:
                    if (canGoDown) {
                        goDown();
                    }
                    break;
                case LEFT:
                    if (canGoLeft) {
                        goLeft();
                    }
                    break;
                case BACK:
                    if(lastMoves.empty()){
                        end = true;
                        break;
                    }
                    // retrives last move, and goes the opposite way.
                    lastMove = lastMoves.pop();
                    if (lastMove == UP) {
                        currentPoint.y++;
                    } else if (lastMove == RIGHT) {
                        currentPoint.x--;
                    } else if (lastMove == DOWN) {
                        currentPoint.y--;
                    } else if (lastMove == LEFT) {
                        currentPoint.x++;
                    }
                    break;
            }

            //doesnt set space if moving back through spaces
            if(canGo){
                //creates a space cell at the current position.
                setSpace(currentPoint.y, currentPoint.x);
            }

            //loops ends once every single possible route has been taken.
        } while (!end);

        //create space at the start to produce two clear paths
        for (int x=2; x<8; x++){
            setSpace(1,x);
            setSpace(x,1);
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

    private void goUp() {
        currentPoint.y--;
        lastMoves.push(UP);
    }

    private void goRight() {
        currentPoint.x++;
        lastMoves.push(RIGHT);
    }

    private void goDown() {
        currentPoint.y++;
        lastMoves.push(DOWN);
    }

    private void goLeft() {
        currentPoint.x--;
        lastMoves.push(LEFT);
    }

    // checks if the specific cell is beside 3 wall cells.
    private boolean nextToThreeWalls(Point nextPos, int direction) {

        boolean upIsWall = nextPos.y == 0 ? false : cells2d[nextPos.y - 1][nextPos.x]==WALL;
        boolean rightIsWall = nextPos.x == width - 1 ? false : cells2d[nextPos.y][nextPos.x + 1]==WALL;
        boolean downIsWall = nextPos.y == height - 1 ? false : cells2d[nextPos.y + 1][nextPos.x]==WALL;
        boolean leftIsWall = nextPos.x == 0 ? false : cells2d[nextPos.y][nextPos.x - 1]==WALL;

        if (direction == UP && leftIsWall && rightIsWall && upIsWall) {
            return true;
        } else if (direction == RIGHT && upIsWall && downIsWall && rightIsWall) {
            return true;
        } else if (direction == DOWN && rightIsWall && downIsWall && leftIsWall) {
            return true;
        } else if (direction == LEFT && upIsWall && downIsWall && leftIsWall) {
            return true;
        } else {
            return false;
        }
    }
}
