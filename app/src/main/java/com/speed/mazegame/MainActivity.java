package com.speed.mazegame;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.google.android.material.navigation.NavigationView;
import com.tapadoo.alerter.Alerter;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_UP;

public class MainActivity extends AppCompatActivity implements PlayerGridAdapter.IEndOfGame, NavigationView.OnNavigationItemSelectedListener, FirestoreUtils.IFireListener, SensorEventListener {

    private static final String TAG = "kesD";
    private static final String QUICKEST_TIME = "quickest_time";

    private GridView mapGrid, playerGrid;
    private Button upBtn,rightBtn,downBtn,leftBtn, menuBtn;
    private ProgressBar progressBar;
    private TextView playerOneScoreTxt, playerTwoScoreTxt, gameIdTxt, countDownTxt, quickestTimeTxt;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private View countdownShadow;

    private Map map;
    private GridAdapter adapter;
    private PlayerGridAdapter playerAdapter;
    private Thread loggingThread;

    private Resources res;
    private AtomicBoolean actionDownFlag = new AtomicBoolean(false);
    public boolean inMultiplayer, tiltMode, landScapeMode;
    private int playerNum, speed, playerOneScore, playerTwoScore, topViewPosition;
    private long time;
    private int width = 20;
    private int height = 30;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeValues();
        findViews();
        setupBtns();
        makeFullScreen();
        createPlayerGrid(createMap().getCells());
        checkForQuickestTime();
    }

    private void findViews() {
        mapGrid = findViewById(R.id.grid);
        playerGrid = findViewById(R.id.player_grid);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.drawer_nav);
        gameIdTxt = findViewById(R.id.gameId);
        progressBar = findViewById(R.id.progressBar);
        upBtn = findViewById(R.id.top_btn);
        rightBtn = findViewById(R.id.right_btn);
        downBtn = findViewById(R.id.down_btn);
        leftBtn = findViewById(R.id.left_btn);
        playerOneScoreTxt = findViewById(R.id.player_one_score);
        playerTwoScoreTxt = findViewById(R.id.player_two_score);
        countDownTxt = findViewById(R.id.countdown_txt);
        countdownShadow = findViewById(R.id.countdown_shadow);

        navigationView.setNavigationItemSelectedListener(this);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.GONE);
        quickestTimeTxt = navigationView.getHeaderView(0).findViewById(R.id.quickest_time);
        YoYo.with(Techniques.FadeOut).duration(1).playOn(countDownTxt);
    }

    private void initializeValues(){
        inMultiplayer = false;
        playerOneScore = 0;
        playerTwoScore = 0;
        speed = 3;
        playerNum = CellViewSpace.PLAYER1;
        time = System.currentTimeMillis();
        topViewPosition = 0;
        tiltMode = false;
        res = getResources();
    }

    private void checkForQuickestTime(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int seconds = prefs.getInt(QUICKEST_TIME,999999);
        if(seconds == 999999){
            quickestTimeTxt.setVisibility(View.GONE);
        }else {
            setQuickestTimeTxt(seconds);
        }
    }

    private boolean checkNewHighScore(){
        int newScore = (int)((System.currentTimeMillis() - time) / 1000);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int oldScore = prefs.getInt(QUICKEST_TIME,999999);
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
        quickestTimeTxt.setText(res.getString(R.string.quickest_time,mins,secs));
    }

    private boolean movement(MotionEvent event, int direction){
        if(event.getAction() == ACTION_DOWN){
            if(loggingThread != null){
                return true;
            }
            actionDownFlag.set(true);
            loggingThread = new Thread(() -> {
                while(actionDownFlag.get()){
                    runOnUiThread(() -> movePlayer(direction));
                    delay();
                }
            });
            loggingThread.start();
        }
        if(event.getAction() == ACTION_UP){
            actionDownFlag.set(false);
            loggingThread = null;
        }
        return true;
    }

    private void delay(){
        try {
            Thread.sleep(100 * speed);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void movePlayer(int direction){
        int playerPos = playerAdapter.move(direction);
        if(inMultiplayer){
            FirestoreUtils.submitPos(playerPos);
        }
        if(direction == Map.UP){
            maybeScrollUp(playerPos);
        }else if(direction == Map.DOWN){
            maybeScrollDown(playerPos);
        }
    }

    private void maybeScrollUp(int playerPos){
        if(((playerPos - topViewPosition) % 600) < 150){
            if(topViewPosition != 0){
                topViewPosition -= 150;
            }
            playerGrid.smoothScrollToPosition(playerPos - 300);
            mapGrid.smoothScrollToPosition(playerPos - 300);
        }
    }

    private void maybeScrollDown(int playerPos){
        if(((playerPos - topViewPosition) % 600) > 450){
            topViewPosition += 150;
            playerGrid.smoothScrollToPosition(topViewPosition + 600);
            mapGrid.smoothScrollToPosition(topViewPosition + 600);
        }
    }

    private void setupBtns(){
        upBtn.setOnTouchListener((v, event) -> movement(event,Map.UP));
        rightBtn.setOnTouchListener((v, event) -> movement(event,Map.RIGHT));
        downBtn.setOnTouchListener((v, event) -> movement(event,Map.DOWN));
        leftBtn.setOnTouchListener((v, event) -> movement(event,Map.LEFT));
    }

    private void newGame(){
        width = landScapeMode?30:20;
        mapGrid.setNumColumns(width);
        playerGrid.setNumColumns(width);
        YoYo.with(Techniques.ZoomOutUp).duration(2500).onEnd(animator -> {
            YoYo.with(Techniques.ZoomInUp).duration(2500).playOn(mapGrid);
            adapter.submitList(map.generateNewCells(height,width));
            adapter.notifyDataSetChanged();
            createPlayerGrid(map.getCells());
        }).playOn(mapGrid);
        YoYo.with(Techniques.FlipOutX).duration(100).onEnd(animator -> YoYo.with(Techniques.RotateIn).duration(4500).playOn(playerGrid)).playOn(playerGrid);
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

    private Map createMap(){
        map = new Map(height,width);
        getQuickestRoute();
        adapter = new GridAdapter(map.getCells(),this);
        mapGrid.setAdapter(adapter);
        playerGrid.setOnItemClickListener((parent, view, position, id) -> {
            if(inMultiplayer){
                FirestoreUtils.submitBlocker(position);
                adapter.placeWall(position);
            }
        });
        return map;
    }

    private void createPlayerGrid(int[] mapCells) {
        playerAdapter = new PlayerGridAdapter(map.getFinish(),mapCells,this,width, playerNum);
        playerGrid.setAdapter(playerAdapter);
    }

    private void makeFullScreen() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS | WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(Color.parseColor("#A7A7A7"));
    }

    @Override
    public void finishReached() {
        if(!inMultiplayer){
            sayWellDone();
            newGame();
            setValuesForNewGame();
            if(checkNewHighScore()){
                Alerter.create(this).setTitle(res.getString(R.string.new_high_score)).setText(res.getString(R.string.see_high_score_msg))
                        .setDuration(3500).setBackgroundColorInt(Color.parseColor("#00ff00")).show();
            }
        }
    }

    TextToSpeech tts;

    private void sayWellDone() {
        tts = new TextToSpeech(this,status -> {
            if(status == TextToSpeech.SUCCESS){
                tts.setLanguage(Locale.UK);
                tts.speak("Well done mate!",TextToSpeech.QUEUE_FLUSH, null);
            }else{
                Log.d(TAG, "sayWellDone: ERROR");
            }
        });

    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        switch (menuItem.getItemId()){
            case R.id.new_game_item:
                setHeight();
                break;
            case R.id.multiplayer_menu_item:
                multiplayerOption();
                break;
            case R.id.set_speed_item:
                speedAlert();
                break;
            case R.id.tilt_mode:
                tiltMode();
                break;
            case R.id.landscape_mode:
                forceLandscape();
                break;
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void forceLandscape(){
        if(landScapeMode){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }else{
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    private void multiplayerOption(){
        if (inMultiplayer) {
            endMultiplayer();
        } else {
            hostOrJoinDialog();
        }
    }

    private void speedAlert(){
        new AlertDialog.Builder(this)
                .setTitle(res.getString(R.string.select_speed))
                .setItems(res.getStringArray(R.array.speeds),(dialog, which) -> speed = which+1)
                .show();
    }

    private void endMultiplayer(){
        gameIdTxt.setText("");
        FirestoreUtils.endGame();
        inMultiplayer = false;
        navigationView.getMenu().findItem(R.id.multiplayer_menu_item).setTitle(res.getString(R.string.multiplayer));
        enableNewGame(true);
        playerTwoScoreTxt.setText("");
        playerTwoScoreTxt.setVisibility(View.GONE);
        playerOneScoreTxt.setText("");
        playerOneScoreTxt.setVisibility(View.GONE);
    }

    private void hostOrJoinDialog(){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(res.getString(R.string.host_or_join));
        alert.setPositiveButton(res.getString(R.string.join),(dialog, which) -> joinOption());
        alert.setNegativeButton(res.getString(R.string.host),(dialog, which) -> hostOption());
        alert.show();
    }

    private void setHeight(){
        final EditText editText = new EditText(this);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        InputFilter[] filters = new InputFilter[1];
        filters[0] = new InputFilter.LengthFilter(9);
        editText.setFilters(filters);
        new AlertDialog.Builder(this)
                .setTitle(res.getString(R.string.enter_maze_length))
                .setView(editText)
                .setPositiveButton(res.getString(R.string.submit),(dialog, which) -> submitHeight(editText))
                .setNegativeButton(res.getString(R.string.cancel),null)
                .show();
    }

    private void submitHeight(EditText editText){
        if(editText.getText() == null || editText.getText().length() == 0){
            showError(res.getString(R.string.enter_maze_length));
            return;
        }
        height = Integer.parseInt(editText.getText().toString());
        if(height > 500000 || height < 3) {
            showError(res.getString(R.string.invalid_maze_length));
            return;
        }
        newGame();
        setValuesForNewGame();
    }

    private void joinOption(){
        final EditText editText = new EditText(this);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        new AlertDialog.Builder(this)
                .setTitle(res.getString(R.string.enter_game_id))
                .setView(editText)
                .setPositiveButton(res.getString(R.string.submit),(dialog, which) -> submitGameID(editText))
                .setNegativeButton(res.getString(R.string.cancel),null)
                .show();
        enableNewGame(false);
    }

    private void enableNewGame(boolean enable){
        navigationView.getMenu().findItem(R.id.new_game_item).setEnabled(enable);
    }

    private void submitGameID(EditText editText){
        String gameID = editText.getText().toString();
        if(gameID.length() == 0){
            showError(res.getString(R.string.game_id_invalid));
            return;
        }
        FirestoreUtils.joinGame(this,gameID);
    }

    private void hostOption(){
        FirestoreUtils.setupGame(this,adapter.getCells());
        enableNewGame(false);
    }

    private void countDown(final int count){
        countdownShadow.setVisibility(View.VISIBLE);
        if(count==0){
            countDownTxt.setText(res.getString(R.string.go));
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
        gameIdTxt.setText(res.getString(R.string.game_id,gameID));
    }

    @Override
    public void showError(String errorMsg) {
        Alerter.create(this).setText(errorMsg).setDuration(10000).setBackgroundColorInt(Color.parseColor("#ff0000")).show();
    }

    @Override
    public void setPlayerNum(int num) {
        playerAdapter.setLocalPlayerNum(num);
        playerNum = num;
        navigationView.getMenu().findItem(R.id.multiplayer_menu_item).setTitle(res.getString(R.string.end_multiplayer));
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
        if(cells == null){
            Alerter.create(this).setText(res.getString(R.string.set_map_error)).setDuration(7500).show();
            return;
        }
        int[] cellArr = new int[cells.size()];
        for(int i = 0; i < cells.size(); i++){
            cellArr[i]= ((Number) cells.get(i)).intValue();
        }
        adapter.submitList(cellArr);
        createPlayerGrid(cellArr);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void gameEnded() {
        Alerter.create(this)
                .setTitle(res.getString(R.string.game_over_winner,playerOneScore == playerTwoScore ? "No one":playerOneScore > playerTwoScore ? "Player one":"Player two"))
                .setBackgroundColorInt(Color.parseColor("#00ff00")).setDuration(3500).show();
        inMultiplayer = false;
        navigationView.getMenu().findItem(R.id.multiplayer_menu_item).setTitle(res.getString(R.string.multiplayer));
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

    public List<Integer> getQuickestRoute(){
        return map.getQuickestRoute();
    }

    @Override
    protected void onResume() {
        super.onResume();

        int orientation = getResources().getConfiguration().orientation;
        Log.d(TAG, "onConfigurationChanged");
        if(orientation == Configuration.ORIENTATION_LANDSCAPE){
            Log.d(TAG, "onConfigurationChanged: landscape");
            Button topBtn2 = findViewById(R.id.top_btn2);
            Button downBtn2 = findViewById(R.id.down_btn2);
            menuBtn = findViewById(R.id.menu_button);
            menuBtn.setOnClickListener(v -> drawerLayout.openDrawer(Gravity.LEFT));
            navigationView.getMenu().findItem(R.id.tilt_mode).setEnabled(false);

            topBtn2.setOnTouchListener((v, event) -> movement(event,Map.UP));
            downBtn2.setOnTouchListener((v, event) -> movement(event,Map.DOWN));
            landScapeMode = true;
            navigationView.getMenu().findItem(R.id.landscape_mode).setTitle("End Landscape Mode");
            newGame();
        } else if(orientation == Configuration.ORIENTATION_PORTRAIT){
            Log.d(TAG, "onConfigurationChanged: portrait");
            landScapeMode = false;
            navigationView.getMenu().findItem(R.id.tilt_mode).setEnabled(true);
            navigationView.getMenu().findItem(R.id.landscape_mode).setTitle("Landscape Mode");
            newGame();
        }
    }

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Button[] buttons;
    private Boolean[] buttonDown = {false,false,false,false};

    private void tiltMode(){
        if(tiltMode){
            registerSensor(false);
            tiltMode = false;
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            navigationView.getMenu().findItem(R.id.landscape_mode).setEnabled(true);
            navigationView.getMenu().findItem(R.id.tilt_mode).setTitle("Tilt Mode");
            return;
        }
        navigationView.getMenu().findItem(R.id.landscape_mode).setEnabled(false);
        navigationView.getMenu().findItem(R.id.tilt_mode).setTitle("End Tilt Mode");
        tiltMode = true;
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        sensorManager = ((SensorManager) getSystemService(Context.SENSOR_SERVICE));
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        buttons = new Button[]{upBtn,rightBtn,downBtn,leftBtn};
        registerSensor(true);
    }

    private void registerSensor(boolean register){
        if(register && tiltMode){
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }else{
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.values == null){
            return;
        }

        float x = event.values[0];
        float y = event.values[1];

        Log.d(TAG, "onSensorChanged: x: "+x+" | y: "+y);

        if( x > 1.5){
            simulatePress(true, Map.LEFT);
        }else if(x < -1.5){
            simulatePress(true,Map.RIGHT);
        }else{
            if (buttonDown[1]){
                simulatePress(false,Map.RIGHT);
            } else if(buttonDown[3]){
                simulatePress(false,Map.LEFT);
            }
        }

        if( y > 1.5){
            simulatePress(true,Map.DOWN);
        }else if( y < -1.5){
            simulatePress(true,Map.UP);
        }else {
            if (buttonDown[0]){
                simulatePress(false,Map.UP);
            } else if(buttonDown[2]){
                simulatePress(false,Map.DOWN);
            }
        }
    }

    private void simulatePress(boolean down, int dir){
        MotionEvent motionEvent = MotionEvent.obtain(SystemClock.uptimeMillis(),SystemClock.uptimeMillis(),down?ACTION_DOWN:ACTION_UP,1,1,0);
        buttons[dir].dispatchTouchEvent(motionEvent);
        buttonDown[dir] = down;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

}
