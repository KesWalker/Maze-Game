package com.speed.mazegame;

import org.junit.Before;
import org.junit.Test;

import static com.speed.mazegame.CellViewSpace.FINISH;
import static com.speed.mazegame.CellViewSpace.SPACE;
import static org.junit.Assert.assertEquals;

public class GeneralUnitTester {

    Map map;

    @Before
    public void initValues(){
        map = new Map(10,20);
    }

    @Test
    public void mapSize(){
        assertEquals(200,map.getCells().length);
        map.generateNewCells(84,20);
        assertEquals(1680,map.getCells().length);
    }

    @Test
    public void startPosition(){
        assertEquals(SPACE,map.getCells()[21]);
    }

    @Test
    public void endPosition(){
        map.generateNewCells(3,20);
        assertEquals(FINISH,map.getCells()[39]);
    }

}
