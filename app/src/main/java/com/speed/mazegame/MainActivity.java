package com.speed.mazegame;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.google.android.material.internal.NavigationMenu;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseApp;
import com.tapadoo.alerter.Alert;
import com.tapadoo.alerter.Alerter;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Thread.sleep;

public class MainActivity extends AppCompatActivity implements PlayerGridAdapter.IEndOfGame, NavigationView.OnNavigationItemSelectedListener, FirestoreUtils.IFireListener {

    private static final String QUICKEST_TIME = "quickest_time";

    private GridView mapGrid, playerGrid;
    private Button upBtn,rightBtn,downBtn,leftBtn;
    private ProgressBar progressBar;
    private TextView playerOneScoreTxt, playerTwoScoreTxt, gameIdTxt, countDownTxt, quickestTimeTxt;
    private Map map;
    private final int width = 20;
    private int height = 30;
    private GridAdapter adapter;
    private PlayerGridAdapter playerAdapter;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    public boolean inMultiplayer;
    private AtomicBoolean actionDownFlag = new AtomicBoolean(false);
    private Thread loggingThread;
    private int playerNum, speed, playerOneScore, playerTwoScore, numBtnsBeingPressed, topViewPosition;
    private long time;
    private View countdownShadow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //FirebaseApp.initializeApp(this);
        initializeValues();
        findViews();
        setupBtns();
        makeFullScreen();
        createMap();
        createPlayerGrid(map.getCells());
        checkForQuickestTime();
    }

    private void findViews() {
        mapGrid = findViewById(R.id.grid);
        playerGrid = findViewById(R.id.player_grid);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.drawer_nav);
        navigationView.setNavigationItemSelectedListener(this);
        gameIdTxt = findViewById(R.id.gameId);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.GONE);
        upBtn = findViewById(R.id.top_btn);
        rightBtn = findViewById(R.id.right_btn);
        downBtn = findViewById(R.id.down_btn);
        leftBtn = findViewById(R.id.left_btn);
        playerOneScoreTxt = findViewById(R.id.player_one_score);
        playerTwoScoreTxt = findViewById(R.id.player_two_score);
        countDownTxt = findViewById(R.id.countdown_txt);
        countdownShadow = findViewById(R.id.countdown_shadow);
        quickestTimeTxt = navigationView.getHeaderView(0).findViewById(R.id.quickest_time);
        YoYo.with(Techniques.FadeOut).duration(1).playOn(countDownTxt);
    }

    private void initializeValues(){
        inMultiplayer = false;
        numBtnsBeingPressed = 0;
        playerOneScore = 0;
        playerTwoScore = 0;
        speed = 3;
        playerNum = CellViewSpace.PLAYER1;
        time = System.currentTimeMillis();
        topViewPosition = 0;
    }

    private void checkForQuickestTime(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int seconds = prefs.getInt(QUICKEST_TIME,999999);
        if(seconds==999999){
            quickestTimeTxt.setVisibility(View.GONE);
        }else {
            setQuickestTimeTxt(seconds);
        }
    }

    private boolean checkNewHighScore(){
        int newScore = (int)((System.currentTimeMillis() - time) / 1000);
        Log.d("kesD", "checkNewHighScore: new score: "+newScore+" seconds");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int oldScore = prefs.getInt(QUICKEST_TIME,999999);
        Log.d("kesD", "checkNewHighScore: oldScore: "+oldScore+" | newScore: "+newScore);
        if(newScore < oldScore){
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(QUICKEST_TIME,newScore);
            editor.apply();
            setQuickestTimeTxt(newScore);
        }
        return newScore < oldScore;
    }

    private void setQuickestTimeTxt(int seconds){
        quickestTimeTxt.setVisibility(View.VISIBLE);
        int mins = seconds / 60;
        int secs = seconds % 60;
        quickestTimeTxt.setText(String.format("Quickest time: %02d:%02d",mins,secs));
    }

    private boolean movement(MotionEvent event, int direction){
        if(event.getAction()==MotionEvent.ACTION_DOWN){
            numBtnsBeingPressed++;
            actionDownFlag.set(true);
            loggingThread = new Thread(() -> {
                while(actionDownFlag.get() && numBtnsBeingPressed < 1){
                    runOnUiThread(() -> {
                        int playerPos = playerAdapter.move(direction);
                        if(inMultiplayer){
                            FirestoreUtils.submitPos(playerPos);
                        }
                        int length = height*width;
                        if(direction==Map.UP){
                            if(((playerPos-topViewPosition) % 600) < 150){
                                if(topViewPosition!=0){
                                    topViewPosition-=150;
                                }
                                playerGrid.smoothScrollToPosition(playerPos-300);
                                mapGrid.smoothScrollToPosition(playerPos-300);
                            }
                        }else if(direction==Map.DOWN){
                            if(((playerPos-topViewPosition) % 600) > 450){
                                topViewPosition+=150;
                                playerGrid.smoothScrollToPosition(topViewPosition+600);
                                mapGrid.smoothScrollToPosition(topViewPosition+600);
                            }
                        }

                        Log.d("kesD", "\n topViewPos: "+topViewPosition+" playerPos: "+playerPos+"\n "+(playerPos-topViewPosition)+" % 600 = "+((playerPos-topViewPosition)%600));
                    });
                    try {
                        Thread.sleep(100 * speed);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            loggingThread.start();
        }
        if(event.getAction()==MotionEvent.ACTION_UP){
            actionDownFlag.set(false);
            numBtnsBeingPressed--;
        }else{
            numBtnsBeingPressed--;
        }
        return true;
    }

    private void setupBtns(){
        upBtn.setOnTouchListener((v, event) -> movement(event,Map.UP));
        rightBtn.setOnTouchListener((v, event) -> movement(event,Map.RIGHT));
        downBtn.setOnTouchListener((v, event) -> movement(event,Map.DOWN));
        leftBtn.setOnTouchListener((v, event) -> movement(event,Map.LEFT));
    }

    private void newGame(){
        adapter.submitList(map.generateNewCells(height));
        adapter.notifyDataSetChanged();
        createPlayerGrid(map.getCells());
        if(inMultiplayer){
            FirestoreUtils.newMap(adapter.getCells());
        }
    }

    private void setValuesForNewGame(){
        playerGrid.smoothScrollToPosition(0);
        mapGrid.smoothScrollToPosition(0);
        topViewPosition = 0;
        countDown(3);
        time = System.currentTimeMillis() - 3000;
    }

    int mLastFirstVisibleItem = 0;

    private void createMap(){
        map = new Map(height,width);
        adapter = new GridAdapter(map.getCells(),this);
        mapGrid.setAdapter(adapter);
        playerGrid.setOnItemClickListener((parent, view, position, id) -> {
            //Toast.makeText(this, "click", Toast.LENGTH_SHORT).show();
            if(inMultiplayer){
                FirestoreUtils.submitBlocker(position);
            }
            adapter.placeWall(position);
        });
        playerGrid.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if(mLastFirstVisibleItem<firstVisibleItem)
                {
                    mapGrid.smoothScrollToPosition(firstVisibleItem+visibleItemCount);
                }
                if(mLastFirstVisibleItem>firstVisibleItem)
                {
                    mapGrid.smoothScrollToPosition(firstVisibleItem);
                }
                mLastFirstVisibleItem=firstVisibleItem;
            }
        });
    }

    private void createPlayerGrid(int[] mapCells) {
        int cellsLength = height*width;
        int[] cells = new int[cellsLength];

        for (int i=0;i<cellsLength;i++){
            cells[i] = CellViewSpace.TRANSPARENT;
        }

        playerAdapter = new PlayerGridAdapter(cells,mapCells,this,width, playerNum);
        playerGrid.setAdapter(playerAdapter);
    }

    private void makeFullScreen() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    public void finishReached() {
        Toast.makeText(this, "end of game", Toast.LENGTH_SHORT).show();
        if(!inMultiplayer){
            newGame();
            setValuesForNewGame();
            if(checkNewHighScore()){
                Alerter.create(this).setTitle("New High Score!").setText("Look in the menu to see your new fastest time!")
                        .setDuration(3500).setBackgroundColorInt(Color.parseColor("#00ff00")).show();
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        switch (menuItem.getItemId()){
            case R.id.new_game_item:
                setHeight();
                break;
            case R.id.multiplayer_menu_item:
                if(inMultiplayer){
                    gameIdTxt.setText("");
                    FirestoreUtils.endGame();
                    inMultiplayer = false;
                    navigationView.getMenu().findItem(R.id.multiplayer_menu_item).setTitle("Multiplayer");
                }else{
                    hostOrJoinDialog();
                }
                break;
            case R.id.set_speed_item:
                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setTitle("Select Speed");
                String[] speedChoices = {"super fast","fast","normal","slow","super slow"};
                alert.setSingleChoiceItems(speedChoices,-1,(dialog, which) -> {
                    switch (speedChoices[which]){
                        case "super fast":
                            speed = 1;
                            break;
                        case "fast":
                            speed = 2;
                            break;
                        case "normal":
                            speed = 3;
                            break;
                        case "slow":
                            speed = 5;
                            break;
                        case "super slow":
                            speed = 10;
                            break;
                    }
                    dialog.dismiss();
                });
                alert.create().show();
                break;
        }

        drawerLayout.closeDrawer(Gravity.LEFT);
        return true;
    }

    private void hostOrJoinDialog(){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Host or Join a game?");
        alert.setPositiveButton("Join",(dialog, which) -> joinOption());
        alert.setNegativeButton("Host",(dialog, which) -> hostOption());
        alert.show();
    }

    private void setHeight(){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        final EditText editText = new EditText(this);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        alert.setTitle("Enter the maze length");
        alert.setView(editText);
        alert.setPositiveButton("Submit",(dialog, which) ->{
            if(editText.getText() == null || editText.getText().length() == 0){
                showError("Please enter a length");
                return;
            }
            height = Integer.parseInt(editText.getText().toString());
            if(height > 9999999 || height < 3){
                showError("Invalid length, minimum length is 3");
                return;
            }
            newGame();
            setValuesForNewGame();
        });
        alert.setNegativeButton("Cancel",null);
        alert.show();
    }

    private void joinOption(){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        final EditText editText = new EditText(this);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        alert.setTitle("Enter the Game ID");
        alert.setView(editText);
        alert.setPositiveButton("Submit",(dialog, which) -> {
            String gameID = editText.getText().toString();
            if(gameID==null||gameID.length()==0){
                showError("Game ID invalid.");
                return;
            }
            FirestoreUtils.joinGame(this,gameID);
        });
        alert.setNegativeButton("Cancel",null);
        alert.show();
    }

    private void hostOption(){
        FirestoreUtils.setupGame(this,adapter.getCells());
    }

    private void countDown(final int count){
        countdownShadow.setVisibility(View.VISIBLE);
        if(count==0){
            countDownTxt.setText("GO!");
            YoYo.with(Techniques.Landing).duration(500).onEnd(animator -> {
                YoYo.with(Techniques.FadeOut).duration(500).playOn(countDownTxt);
                countdownShadow.setVisibility(View.GONE);
            }).playOn(countDownTxt);
            return;
        }
        countDownTxt.setText(""+count);
        YoYo.with(Techniques.Landing).duration(1000).onEnd(animator ->
                YoYo.with(count==3?Techniques.SlideOutRight:count==2?Techniques.SlideOutLeft:Techniques.SlideOutDown).duration(500).onEnd(animator1 ->
                        countDown(count-1)).playOn(countDownTxt)).playOn(countDownTxt);
    }

    @Override
    public void setGameID(String gameID) {
        gameIdTxt.setText("Game ID: "+gameID);
    }

    @Override
    public void showError(String errorMsg) {
        Alerter.create(this).setText(errorMsg).setDuration(10000).setBackgroundColorInt(Color.parseColor("#ff0000")).show();
    }

    @Override
    public void setPlayerNum(int num) {
        playerAdapter.setLocalPlayerNum(num);
        playerNum = num;
        navigationView.getMenu().findItem(R.id.multiplayer_menu_item).setTitle("End Multiplayer");
        inMultiplayer = true;
        playerOneScoreTxt.setText(""+playerOneScore);
        playerTwoScoreTxt.setText(""+playerTwoScore);
        playerOneScoreTxt.setVisibility(View.VISIBLE);
        playerTwoScoreTxt.setVisibility(View.VISIBLE);
    }

    @Override
    public void moveSecondPlayer(int pos) {
        playerAdapter.moveSecondPlayer(pos);
    }

    @Override
    public void setMap(List<Integer> cells) {
        if(cells==null){
            Alerter.create(this).setText("Tried setting map but provided map was empty :(").setDuration(7500).show();
            return;
        }
        int[] cellArr = new int[cells.size()];
        for(int i=0;i<cells.size();i++){
            cellArr[i]= ((Number) cells.get(i)).intValue();
        }
        adapter.submitList(cellArr);
        createPlayerGrid(cellArr);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void gameEnded() {
        String endMessage = "Game Over. No one wins";
        if(playerOneScore > playerTwoScore){
            endMessage = "Game Over. Player one wins!";
        }else if(playerOneScore < playerTwoScore){
            endMessage = "Game Over. Player two wins!";
        }
        Alerter.create(this).setTitle(endMessage).setBackgroundColorInt(Color.parseColor("#00ff00")).setDuration(3500).show();
        inMultiplayer = false;
        navigationView.getMenu().findItem(R.id.multiplayer_menu_item).setTitle("Multiplayer");
    }

    @Override
    public void gameOverGoAgain() {
        if(playerNum == CellViewSpace.PLAYER1){
            newGame();
        }
        setValuesForNewGame();
        playerAdapter.resetPos();
    }

    @Override
    public void setProgressVisibility(boolean visible) {
        progressBar.setVisibility(visible?View.VISIBLE:View.GONE);
    }

    @Override
    public void setBlocker(int pos) {
        adapter.placeWall(pos);
    }

    @Override
    public void increaseScore(boolean playerOneWon) {
        if(playerOneWon){
            playerOneScore++;
            playerOneScoreTxt.setText(""+playerOneScore);
        }else{
            playerTwoScore++;
            playerTwoScoreTxt.setText(""+playerTwoScore);
        }
    }
}
