package com.speed.mazegame;

import android.os.SystemClock;

import androidx.test.espresso.contrib.DrawerActions;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static android.text.InputType.TYPE_CLASS_NUMBER;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withInputType;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class GeneralTester {

    @Rule
    public ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<>(MainActivity.class);

    @Test
    public void movePlayer(){
        for (int i = 0;i<5;i++){
            onView(withId(R.id.right_btn))
                    .perform(click());
        }
        for (int i = 0;i<5;i++){
            onView(withId(R.id.left_btn))
                    .perform(click());
        }
        for (int i = 0;i<5;i++){
            onView(withId(R.id.down_btn))
                    .perform(click());
        }
        for (int i = 0;i<5;i++)
            onView(withId(R.id.top_btn))
                    .perform(click());
    }

    @Test
    public void newGame() {
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
        onView(withText("Set Speed")).perform(click());
        onView(withText("super fast")).perform(click());
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
        onView(withText("New Game")).perform(click());
        onView(withInputType(TYPE_CLASS_NUMBER)).perform(click(),typeText("50"));
        onView(withText("SUBMIT")).perform(click());
        onView(withId(R.id.right_btn)).perform(click());
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
        onView(withText("Set Speed")).perform(click());
        onView(withText("super slow")).perform(click());
        onView(withId(R.id.left_btn)).perform(click());
    }

    @Test
    public void solveMaze(){
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
        onView(withText("Set Speed")).perform(click());
        onView(withText("fast")).perform(click());
        doMaze(activityRule.getActivity().getQuickestRoute());
    }

    @Test
    public void solveBigMaze(){
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
        onView(withText("New Game")).perform(click());
        onView(withInputType(TYPE_CLASS_NUMBER)).perform(click(),typeText("100"));
        onView(withText("SUBMIT")).perform(click());
        SystemClock.sleep(5000);
        doMaze(activityRule.getActivity().getQuickestRoute());
    }

    private void doMaze(List<Integer> moves){
        int[] btns = {R.id.top_btn,R.id.right_btn,R.id.down_btn,R.id.left_btn};
        for (Integer i:moves){
            onView(withId(btns[i])).perform(click());
        }
    }

    @Test
    public void landscapeMode(){
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
        onView(withText("Set Speed")).perform(click());
        onView(withText("Slow")).perform(click());
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
        onView(withText("Landscape Mode")).perform(click());
        SystemClock.sleep(6000);
        doMaze(activityRule.getActivity().getQuickestRoute());
    }

}
