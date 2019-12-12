package com.speed.mazegame;

import android.app.Activity;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreUtils {
    private static final String TAG = "firestore", ONE_POS = "onePos", TWO_POS = "twoPos", CELLS = "cells",
            BLOCKER = "blocker", GAMES = "games";
    private static FirebaseFirestore db;
    private static DocumentReference doc, gameIdDoc;
    private static ListenerRegistration gameListener;
    private static IFireListener fireListener;
    private static String playerNum;
    private static List<Integer> mapCells;
    private static int finishCell;

    public interface IFireListener {
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

    public static void setupGame(Activity activity, List<Integer> aMapCells) {
        mapCells = aMapCells;
        gameIdDoc = getFireInstance("gameID", activity);
        gameIdDoc.get()
                .addOnSuccessListener(documentSnapshot -> initGameWithId(documentSnapshot)
                        .addOnSuccessListener(aVoid -> setNewGameValues())
                        .addOnFailureListener(e -> somethingFailed(e)))
                .addOnFailureListener(e -> fireListener.showError(e.getMessage()));
    }

    public static void joinGame(Activity activity, String gameId) {
        getFireInstance(gameId, activity).get()
                .addOnSuccessListener(documentSnapshot -> gameJoined(documentSnapshot, gameId))
                .addOnFailureListener(e -> somethingFailed(e));
    }

    private static DocumentReference getFireInstance(String docPath, Activity activity) {
        db = FirebaseFirestore.getInstance();
        fireListener = (IFireListener) activity;
        fireListener.setProgressVisibility(true);
        return db.collection(GAMES).document(docPath);
    }

    private static Task<Void> initGameWithId(DocumentSnapshot documentSnapshot) {
        Map<String, Object> map = new HashMap<String, Object>() {{
            put(ONE_POS, 1);
            put(TWO_POS, 1);
        }};
        doc = db.collection(GAMES).document(documentSnapshot.getString("ID"));
        gameIdDoc.update("ID", "" + (Integer.parseInt(doc.getId()) + 1));
        return doc.set(map);
    }

    private static void setNewGameValues() {
        fireListener.setProgressVisibility(false);
        fireListener.setGameID(doc.getId());
        doc.update(ONE_POS, 21);
        doc.update(CELLS, mapCells);
        playerNum = ONE_POS;
        fireListener.setPlayerNum(CellViewSpace.PLAYER1);
        startGame();
    }

    private static void somethingFailed(Exception e) {
        fireListener.setProgressVisibility(false);
        fireListener.showError(e.getMessage());
    }

    private static void gameJoined(DocumentSnapshot documentSnapshot, String gameId) {
        fireListener.setProgressVisibility(false);
        if (!documentSnapshot.exists()) {
            fireListener.showError("Wrong Game ID");
            return;
        }
        doc = db.collection(GAMES).document(gameId);
        doc.update(TWO_POS, 21);
        playerNum = TWO_POS;
        fireListener.setPlayerNum(CellViewSpace.PLAYER2);
        fireListener.setMap((List<Integer>) documentSnapshot.get(CELLS));
        startGame();
    }

    public static void startGame() {
        gameListener = doc.addSnapshotListener((documentSnapshot, e) -> {
            Multiplayer game = documentSnapshot.toObject(Multiplayer.class);
            if (game == null) {
                endGame();
                return;
            }
            if (game.getOnePos() != 1 && game.getTwoPos() != 1) {
                fireListener.moveSecondPlayer(playerNum.equals(ONE_POS) ? game.getTwoPos() : game.getOnePos());
                if (game.getBlocker() != -1) {
                    fireListener.setBlocker(game.getBlocker());
                    doc.update(BLOCKER, -1);
                }
            }
            if (game.getOnePos() == 21 && game.getTwoPos() == 21) {
                mapCells = game.getCells();
                finishCell = mapCells.indexOf(2);
                fireListener.setMap(mapCells);
            } else if (finishCell == 0 && mapCells != null) {
                finishCell = mapCells.indexOf(2);
            }
            if (game.getTwoPos() == finishCell || game.getOnePos() == finishCell) {
                fireListener.increaseScore(game.getOnePos() == finishCell);
                doc.update(ONE_POS, 21, TWO_POS, 21);
                fireListener.gameOverGoAgain();
            }
        });
    }

    public static void endGame() {
        gameListener.remove();
        doc.delete();
        db = null;
        fireListener.gameEnded();
    }

    public static void newMap(List<Integer> cells) {
        doc.update(CELLS, cells);
        mapCells = cells;
    }

    public static void submitPos(int pos) {
        doc.update(playerNum, pos);
    }

    public static void submitBlocker(int pos) {
        doc.update(BLOCKER, pos);
    }
}
