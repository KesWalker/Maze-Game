package com.speed.mazegame;

import android.app.Activity;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreUtils {
    private static final String TAG = "firestore";
    private static FirebaseFirestore db;
    private static DocumentReference doc;
    private static IFireListener fireListener;
    private static String playerNum;
    private static List<Integer> mapCells;

    public interface IFireListener{
        void setGameID(String gameID);
        void showError(String errorMsg);
        void setPlayerNum(int num);
        void moveSecondPlayer(int pos);
        void setMap(List<Integer> cells);
    }

    public static void setupGame(Activity activity, List<Integer> mapCells){
        db = FirebaseFirestore.getInstance();
        fireListener = (IFireListener) activity;

        Map<String,Integer> map = new HashMap<String,Integer>(){{
            put("onePos",1);
            put("twoPos",1);
        }};

        Log.d(TAG, "setupGame: setting up");
        db.collection("games").add(map).addOnSuccessListener(documentReference -> {
            Log.d(TAG, "setupGame: docref:"+documentReference.getId());
            fireListener.setGameID(documentReference.getId());
            doc = documentReference;
            doc.update("onePos",22)
                    .addOnSuccessListener(command -> Log.d(TAG, "player 1 position updated"))
                    .addOnFailureListener(e -> Log.d(TAG, "player 1 position failed to update: "+e.getMessage()));
            doc.update("cells",mapCells);
            playerNum = "onePos";
            fireListener.setPlayerNum(CellViewSpace.PLAYER1);
            startGame();
        }).addOnFailureListener(e -> {
            fireListener.showError(e.getMessage());
            Log.d(TAG, "setupGame: failure listen");
        }).addOnCompleteListener(task -> {
            Log.d(TAG, "setupGame: complete listen");
        });
    }

    public static void joinGame(Activity activity, String gameId){
        db = FirebaseFirestore.getInstance();
        fireListener = (IFireListener) activity;

        db.collection("games").document(gameId).get().addOnSuccessListener(documentSnapshot -> {
            if(!documentSnapshot.exists()){
                fireListener.showError("Wrong Game ID");
                return;
            }
            Log.d(TAG, "joinGame: success");
            doc = db.collection("games").document(gameId);
            doc.update("twoPos",22)
                    .addOnSuccessListener(command -> Log.d(TAG, "player 2 position updated"))
                    .addOnFailureListener(e -> Log.d(TAG, "player 2 position failed to update: "+e.getMessage()));
            playerNum = "twoPos";
            fireListener.setPlayerNum(CellViewSpace.PLAYER2);
            fireListener.setMap((List<Integer>) documentSnapshot.get("cells"));
            startGame();
        }).addOnFailureListener(e -> fireListener.showError(e.getMessage()));
    }

    public static void startGame(){
        doc.addSnapshotListener((documentSnapshot, e) -> {
            Mutliplayer game = documentSnapshot.toObject(Mutliplayer.class);
            Log.d(TAG, "startGame: snapshot: p1 pos: "+documentSnapshot.get("onePos"));
            if(game.getOnePos()!=1 && game.getTwoPos()!=1){
                Log.d(TAG, "BOTH PLAYERS READY, p1 pos: "+game.getOnePos()+" | p2 pos: "+game.getTwoPos());
                fireListener.moveSecondPlayer(playerNum.equals("onePos") ? game.getTwoPos():game.getOnePos());
            }
            if(game.getCells()!=mapCells){
                Log.d(TAG, "startGame: A NEW GAME HAS STARTED");
                mapCells = game.getCells();
                fireListener.setMap(mapCells);
            }
        });
    }

    public static void newMap(List<Integer> cells){
        doc.update("cells",cells);
        mapCells = cells;
    }

    public static void submitPos(int pos){
        doc.update(playerNum,pos)
                .addOnSuccessListener(command -> Log.d(TAG, playerNum + " updated"))
                .addOnFailureListener(e -> Log.d(TAG, playerNum + " failed to update: "+e.getMessage()));;
    }
}
