package com.midTerm;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class Player implements Observer {
    private Socket socket;
    private Scanner scanner;
    private Character character;
    private Socket ConnectionSocket;
    private String name;

    /**
     * this methods calls the players' special skill
     */
    public void call() {

    }

    /**
     * vote a suspicious player
     * @return name of the suspicious player
     */
    public String vote() {
        return getTargetName();
    }

    /**
     * get a name from user for targeting a player
     * @return a String of characters which is a name
     */
    private String getTargetName() {
        return scanner.nextLine();
    }

    @Override
    public void update(ArrayList<Player> removedPlayer) {

    }

    private enum Character {
        //TODO functionality of the characters must be added
        GODFATHER,
        MAFIA,
        DOCTORLECTOR,
        CITIZEN,
        MAYOR,
        DOCTOR,
        DIEHARD,
        MENTALIST,
        PROFESSIONAL,
        DETECTOR;

        Character() {
        }

    }
}
