package com.speed.mazegame;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.internal.NavigationMenu;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseApp;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Thread.sleep;

public class MainActivity extends AppCompatActivity implements PlayerGridAdapter.IEndOfGame, NavigationView.OnNavigationItemSelectedListener, FirestoreUtils.IFireListener {

    private GridView mapGrid, playerGrid;
    private Button menuBtn, newMapBtn;
    private Button upBtn,rightBtn,downBtn,leftBtn;
    private Map map;
    private final int height = 30,width = 20;
    private GridAdapter adapter;
    private PlayerGridAdapter playerAdapter;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView gameIdTxt;
    public boolean inMultiplayer;
    private AtomicBoolean actionDownFlag = new AtomicBoolean(true);
    private Thread loggingThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //FirebaseApp.initializeApp(this);
        inMultiplayer = false;

        findViews();
        setupBtns();
        makeFullScreen();
        createMap();
        createPlayerGrid(map.getCells());
        //FirestoreUtils.setupGame();
    }

    private void findViews() {
        mapGrid = findViewById(R.id.grid);
        playerGrid = findViewById(R.id.player_grid);
        newMapBtn = findViewById(R.id.new_map_btn);
        newMapBtn.setOnClickListener(v -> newGame());
        menuBtn = findViewById(R.id.menu_btn);
        menuBtn.setOnClickListener(v -> openMenu());
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.drawer_nav);
        navigationView.setNavigationItemSelectedListener(this);
        gameIdTxt = findViewById(R.id.gameId);

        upBtn = findViewById(R.id.top_btn);
        rightBtn = findViewById(R.id.right_btn);
        downBtn = findViewById(R.id.down_btn);
        leftBtn = findViewById(R.id.left_btn);
    }

    private void openMenu(){
        drawerLayout.openDrawer(Gravity.LEFT);
    }

    private boolean movement(MotionEvent event, int direction){
        if(event.getAction()==MotionEvent.ACTION_DOWN){
            actionDownFlag.set(true);
            loggingThread = new Thread(() -> {
                while(actionDownFlag.get()){
                    runOnUiThread(() -> {
                        if(inMultiplayer){
                            FirestoreUtils.submitPos(playerAdapter.move(direction));
                        }else{
                            playerAdapter.move(direction);
                        }
                    });
                    try {
                        Thread.sleep(150);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            loggingThread.start();
        }
        if(event.getAction()==MotionEvent.ACTION_UP){
            actionDownFlag.set(false);
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
        adapter.submitList(map.generateNewCells());
        adapter.notifyDataSetChanged();
        createPlayerGrid(map.getCells());
        if(inMultiplayer){
            FirestoreUtils.newMap(adapter.getCells());
        }
    }

    private void createMap(){
        map = new Map(height,width);
        adapter = new GridAdapter(map.getCells(),this);
        mapGrid.setAdapter(adapter);
    }

    private void createPlayerGrid(int[] mapCells) {
        int cellsLength = height*width;
        int[] cells = new int[cellsLength];

        for (int i=0;i<cellsLength;i++){
            cells[i] = CellViewSpace.TRANSPARENT;
        }

        playerAdapter = new PlayerGridAdapter(cells,mapCells,this,width);
        playerGrid.setAdapter(playerAdapter);
    }

    private void makeFullScreen(){
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    public void finishReached() {
        Toast.makeText(this, "end of game", Toast.LENGTH_SHORT).show();
        newGame();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        switch (menuItem.getItemId()){
            case R.id.new_game_item:
                newGame();
                break;
            case R.id.multiplayer_menu_item:
                if(inMultiplayer){
                    gameIdTxt.setText("");
                    //todo exit multiplayer here.
                    newGame();
                    inMultiplayer = false;
                    navigationView.getMenu().findItem(R.id.multiplayer_menu_item).setTitle("Multiplayer");
                }else{
                    hostOrJoinDialog();
                }
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

    private void joinOption(){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        final EditText editText = new EditText(this);
        alert.setView(editText);
        alert.setPositiveButton("Submit",(dialog, which) -> FirestoreUtils.joinGame(this,editText.getText().toString()));
        alert.setNegativeButton("Cancel",null);
        alert.show();
    }

    private void hostOption(){
        FirestoreUtils.setupGame(this,adapter.getCells());
    }

    @Override
    public void setGameID(String gameID) {
        gameIdTxt.setText("Game ID: "+gameID);
        navigationView.getMenu().findItem(R.id.multiplayer_menu_item).setTitle("End Multiplayer");
        inMultiplayer = true;
    }

    @Override
    public void showError(String errorMsg) {
        Snackbar.make(findViewById(android.R.id.content),errorMsg,7000).show();
    }

    @Override
    public void setPlayerNum(int num) {
        playerAdapter.setLocalPlayerNum(num);
        inMultiplayer = true;
    }

    @Override
    public void moveSecondPlayer(int pos) {
        playerAdapter.moveSecondPlayer(pos);
    }

    @Override
    public void setMap(List<Integer> cells) {
        int[] cellArr = new int[cells.size()];
        for(int i=0;i<cells.size();i++){
            cellArr[i]= ((Number) cells.get(i)).intValue();
        }
        adapter.submitList(cellArr);
        createPlayerGrid(cellArr);
        adapter.notifyDataSetChanged();
    }
}
