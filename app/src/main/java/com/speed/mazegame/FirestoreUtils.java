package com.speed.mazegame;

import android.app.Activity;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreUtils {
    private static final String TAG = "firestore";
    private static FirebaseFirestore db;
    private static DocumentReference doc, gameIdDoc;
    private static ListenerRegistration gameListener;
    private static IFireListener fireListener;
    private static String playerNum;
    private static List<Integer> mapCells;
    private static int finishCell;
    
    public interface IFireListener{
        void setGameID(String gameID);
        void showError(String errorMsg);
        void setPlayerNum(int num);
        void moveSecondPlayer(int pos);
        void setMap(List<Integer> cells);
        void gameEnded();
        void gameOverGoAgain();
        void setProgressVisibility(boolean visible);
        void setBlocker(int pos);
        void increaseScore(boolean playerOneWon);
    }

    public static void setupGame(Activity activity, List<Integer> mapCells){
        db = FirebaseFirestore.getInstance();
        fireListener = (IFireListener) activity;

        Map<String,Object> map = new HashMap<String,Object>(){{
            put("onePos",1);
            put("twoPos",1);
        }};
        fireListener.setProgressVisibility(true);
        Log.d(TAG, "setupGame: setting up");
        gameIdDoc = db.collection("games").document("gameID");
        gameIdDoc.get().addOnSuccessListener(documentSnapshot -> {
            doc = db.collection("games").document(documentSnapshot.getString("ID"));
            gameIdDoc.update("ID",""+(Integer.parseInt(doc.getId())+1));
            doc.set(map).addOnSuccessListener(aVoid -> {
                Log.d(TAG, "setupGame: docref:"+doc);
                fireListener.setProgressVisibility(false);
                fireListener.setGameID(doc.getId());
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
        }).addOnFailureListener(e -> fireListener.showError(e.getMessage()));
    }

    public static void joinGame(Activity activity, String gameId){
        db = FirebaseFirestore.getInstance();
        fireListener = (IFireListener) activity;

        fireListener.setProgressVisibility(true);
        db.collection("games").document(gameId).get().addOnSuccessListener(documentSnapshot -> {
            fireListener.setProgressVisibility(false);
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
        gameListener = doc.addSnapshotListener((documentSnapshot, e) -> {
            Mutliplayer game = documentSnapshot.toObject(Mutliplayer.class);
            if(game == null){
                fireListener.gameEnded();
                gameListener.remove();
                return;
            }
            Log.d(TAG, "startGame: snapshot: p1 pos: "+documentSnapshot.get("onePos"));
            if(game.getOnePos()!=1 && game.getTwoPos()!=1){
                fireListener.moveSecondPlayer(playerNum.equals("onePos") ? game.getTwoPos():game.getOnePos());
                if(game.getBlocker() != -1){
                    fireListener.setBlocker(game.getBlocker());
                    doc.update("blocker",-1);
                }
            }
            if(game.getOnePos() == 22 && game.getTwoPos() == 22){
                Log.d(TAG, "startGame: A NEW GAME HAS STARTED");
                mapCells = game.getCells();
                finishCell = mapCells.indexOf(2);
                fireListener.setMap(mapCells);
            }else if(finishCell==0 && mapCells!=null){
                finishCell = mapCells.indexOf(2);
            }
            if(game.getTwoPos() == finishCell || game.getOnePos() == finishCell){
                fireListener.increaseScore(game.getOnePos()==finishCell);
                doc.update("onePos",22,"twoPos",22);
                fireListener.gameOverGoAgain();
            }
            Log.d("finishCell", "cell: "+finishCell);
        });
    }

    public static void endGame(){
        gameListener.remove();
        doc.delete();
        fireListener.gameEnded();
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

    public static void submitBlocker(int pos){
        doc.update("blocker",pos);
    }
}
