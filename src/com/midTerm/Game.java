package com.midTerm;

import java.util.ArrayList;
import java.util.Stack;

//----------------------------------------
// this class holds game related status and method
public class Game {

    // players is the list of all players which game started with
    // notReservedPlayers is the players which are not give to a client or reserved for it
    // deadPlayers is the list of dead players
    // deadPlayers is the list of dead players
    // isDay check if its night or day
    // result is the final result of the game
    // sortedList is sorted list of client handlers(it is sorted in awake hierarchy order)
    private ArrayList<Player> players;
    private Stack<GameCharacter> characters;
    private ArrayList<Player> deadPlayers;
    private ArrayList<Player> alivePlayers;
    private boolean isDay;
    private boolean isIntroduction = true;
    private final String result = null;
    private ArrayList<Server.ClientHandler> sortedHandlerList;
    private int timer;

    /**
     * this is a constructor
     */
    public Game() {
        characters = new Stack<>();
        setupCharactersStack();
    }

    public static String getProperMessage(String s) {
        return "THE NARRATOR: " + s + "\n";
    }

    /**
     * this method prepare characters stack
     */
    private void setupCharactersStack() {
        characters.add(GameCharacter.GODFATHER);
        characters.add(GameCharacter.DETECTOR);
        characters.add(GameCharacter.DOCTOR);
        characters.add(GameCharacter.MAYOR);
        characters.add(GameCharacter.DIEHARD);
        characters.add(GameCharacter.MENTALIST);
        characters.add(GameCharacter.PROFESSIONAL);
        characters.add(GameCharacter.DOCTORLECTOR);
        characters.add(GameCharacter.CITIZEN);
        characters.add(GameCharacter.CITIZEN);
        characters.add(GameCharacter.CITIZEN);
        characters.add(GameCharacter.MAFIA);
    }

    /**
     * this method starts the next stage of the game.
     */
    public void next() {
        if (isGameFinished()) {
            showResultOfTheGame();
            return;
        }
        if (isDay && isIntroduction) {
            switchDay();
            startNightProgress();
        } else if (isDay) {
            switchDay();
            startNightProgress();
        } else {
            switchDay();
            startTimer(1000 * 5 * 60);
            startDayProgress();
        }
        next();
    }

    private void startTimer(int totalPeriod) {
        timer = totalPeriod;
        Thread clock = new Thread(() -> {
            while (timer != 0) {
                try {
                    timer--;
                    wait(1000);
                } catch (InterruptedException e) {
                    System.out.println("Clock Stopped Working");
                }
            }
        });
    }

    /**
     * switch from day to night or visa versa
     */
    public void switchDay() {
        if (isDay)
            sortedHandlerList.forEach(Server.ClientHandler::asleep);
        else
            sortedHandlerList.forEach(Server.ClientHandler::awake);
        isDay = !isDay;
    }


    /**
     * this method returns a Random Player Instance
     * @return a Player Instance
     */
    public GameCharacter getCharacterInstance() {
        return characters.pop();
    }

    /**
     * this is a constructor
     * @param player is the player which will be removed
     */
    public void removePlayer(Player player) {

    }

    /**
     * this method add a player to the list of players
     * @param player the new player which is going to be added
     */
    public void addPlayer(Player player) {
        players.add(player);
    }

    /**
     * this methods checks if a game is finished
     * @return
     */
    public boolean isGameFinished() {

        return false;
    }

    /**
     * this method sort the client handlers in order which will be called during the night
     * @param clientHandlers is the initial list of client handlers
     * @return a sorted clientHandler ArrayList
     */
    public ArrayList<Server.ClientHandler> sortHandlers(ArrayList<Server.ClientHandler> clientHandlers) {
        while (sortedHandlerList.size() != clientHandlers.size()) {
            for (var clientHandler : clientHandlers) {
                if (clientHandler.getClient().getCharacter() == GameCharacter.GODFATHER)
                    sortedHandlerList.add(clientHandler);
                if (clientHandler.getClient().getCharacter() == GameCharacter.DOCTOR)
                    sortedHandlerList.add(clientHandler);
                if (clientHandler.getClient().getCharacter() == GameCharacter.MAFIA)
                    sortedHandlerList.add(clientHandler);
                if (clientHandler.getClient().getCharacter() == GameCharacter.DETECTOR)
                    sortedHandlerList.add(clientHandler);
                if (clientHandler.getClient().getCharacter() == GameCharacter.PROFESSIONAL)
                    sortedHandlerList.add(clientHandler);
                if (clientHandler.getClient().getCharacter() == GameCharacter.MENTALIST)
                    sortedHandlerList.add(clientHandler);
                if (clientHandler.getClient().getCharacter() == GameCharacter.DIEHARD)
                    sortedHandlerList.add(clientHandler);
            }
        }
        return sortedHandlerList;
    }
}