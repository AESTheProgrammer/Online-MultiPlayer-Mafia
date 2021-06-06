package com.midTerm;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

//----------------------------------------
// this class holds game related status and method
public class Game {
    private interface Notifier {
        void notify(Server.ClientHandler handler, String message);
    }

    // list of all characters of the game which are sorted
    // players is the list of all players which game started with
    // sortedHandlerList is sorted list of client handlers(it is sorted in awake hierarchy order)
    // sortedClientList is sorted list of client (it is sorted in awake hierarchy order)
    // <poleResult> is the final result of the game
    // <isIntroduction> indicates if it is introduction day
    // isDay check if its night or day
    // timer is the time left from the section of the game
    private Stack<GameCharacter> characters;
    private ArrayList<Player> players;
    private ArrayList<Server.ClientHandler> sortedHandlerList;
    private ArrayList<Client> sortedClientList;
    private String poleResult = null;
    private boolean isIntroductionDay;
    private boolean isDay;
    private int timer;

    /**
     * this is a constructor
     */
    public Game() {
        characters = new Stack<>();
        sortedClientList = new ArrayList<>();
        sortedHandlerList = new ArrayList<>();
        isIntroductionDay = true;
        setupCharactersStack();
    }

    /**
     * this method returns a prepared String which is desirable for sending a message
     * @param s is the initial String
     * @return an prepared message from the String s
     */
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
        startIntroductionNightProgress();
        if (isGameFinished()) {
            showResultOfTheGame();
            return;
        } else if (isIntroductionDay) {
            switchDay();
            startIntroductionNightProgress();
        } else if (isDay) {
            switchDay();
            startNightProgress();
        } else {
            switchDay();
            startDayProgress();
        }
        next();
    }

    /**
     * this method starts the night progress
     */
    private void startNightProgress() {
        for (int i = 0; i < 3; i++)
            sortedHandlerList.get(i).getVote(Game.getProperMessage("Who To Be Executed?"));
        startTimer(30);
        sortedHandlerList.get(0).silent();
        sortedHandlerList.get(2).silent();

        sortedHandlerList.get(1).getVote(Game.getProperMessage("Who To Be Cured?"));
        startTimer(20);
        sortedHandlerList.get(1).silent();


        sortedHandlerList.get(4).getVote(Game.getProperMessage("Who To Be Cured?"));
        startTimer(20);
        sortedHandlerList.get(4).silent();

        sortedHandlerList.get(5).getVote(Game.getProperMessage("Who To Be inquired?"));
        startTimer(20);
        sortedHandlerList.get(5).silent();

        sortedHandlerList.get(6).getVote(Game.getProperMessage("Who To Be Killed?"));
        startTimer(20);
        sortedHandlerList.get(6).silent();

        sortedHandlerList.get(7).getVote(Game.getProperMessage("Who to be muted?"));
        startTimer(20);
        sortedHandlerList.get(7).silent();

        sortedHandlerList.get(8).getVote(Game.getProperMessage("Do you want me to reveal dead characters?"));
        startTimer(20);
        sortedHandlerList.get(8).silent();
    }

    /**
     * this method control the daytime progress
     */
    private void startDayProgress() {
        startTimer(1000 * 5 * 60);
        makePole();
    }

    /**
     * this method makes a pole for voting to suspicious person
     */
    private void makePole() {
        for (var clientHandler : sortedHandlerList)
            clientHandler.getVote(Game.getProperMessage("Who To Be Executed?"));
        startTimer(30);
        getPoleResult(countTheVotes());
        announcePoleResult();
        sortedHandlerList.forEach(Server.ClientHandler::silent);
        callTheMayor();
        announceFinalPoleResult();
        //TODO forgot to send list of all players for clients
        //TODO you might want to do it in the end
    }

    /**
     * this method report the poles result with character and username
     */
    private void announceFinalPoleResult() {
        if (poleResult.equals(""))
            return;
        Client client = null;
        for (var clnt : sortedClientList)
            if (clnt.getUsername().equalsIgnoreCase(poleResult))
                client = clnt;
        for (var handler : sortedHandlerList)
            handler.reportPoleResult(getProperMessage(
                    client.getUsername() + "(" + client.getCharacter() + ") was executed."));
    }

    /**
     * report initial pole result
     */
    private void announcePoleResult() {
        for (var handler : sortedHandlerList)
            handler.reportPoleResult(
                    getProperMessage(poleResult + " was voted."));
    }

    /**
     * this method awake the mayor for confirming the execution of the voted client
     */
    private void callTheMayor() {
        sortedHandlerList.get(3).getConfirmation(poleResult);
        startTimer(30);
    }

    /**
     * this method gets the final result of pole
     * @param countedVotes is the hashmap of counter number of votes
     */
    private void getPoleResult(HashMap<String, Integer> countedVotes) {
        int max = 0;
        for (var name : countedVotes.keySet()) {
            if (countedVotes.get(name) > max) {
                max = countedVotes.get(name);
                poleResult = name;
            }
        }
    }

    /**
     * this method count all of the votes and assign the result to the <result> field
     */
    private HashMap<String, Integer> countTheVotes() {
        HashMap<String, Integer> countedVotes = new HashMap<>();
        ArrayList<String> votes = (ArrayList<String>) sortedHandlerList.stream()
                .map(Server.ClientHandler::getVoted).collect(Collectors.toList());
        for (var vote : votes)
            countedVotes.put(vote, countedVotes.get(vote) + 1);
        for (var handler : sortedHandlerList)
            if (handler.getVoted().length() == 0)
                handler.incrementWarnings("You've just got a warning.");
        return countedVotes;
    }

    /**
     * this method starts the night progress
     */
    private void startIntroductionNightProgress() {
        Function<Integer, String> identifier = (Integer index) ->
                        sortedHandlerList.get(index).getClient().getUsername() + " AS " +
                        sortedHandlerList.get(index).getClient().getCharacter().toString();
        Function<Integer, String> roleGetter = (Integer index) ->
                sortedHandlerList.get(index).getClient().getCharacter().toString();
        Notifier alarm = Server.ClientHandler::awake;
        Notifier sleeper = Server.ClientHandler::asleep;

        try {
            var message = "You are now awake, mafias are as follows...\n" +
                    identifier.apply(0) + identifier.apply(1) + identifier.apply(2);
            for (int i = 0; i < 3; i++) {
                alarm.notify(sortedHandlerList.get(i), getProperMessage(message));
                sleeper.notify(sortedHandlerList.get(i), getProperMessage("Your now asleep."));
                wait(6000);
            }
            message = getProperMessage("It's time to wakeup, you are the mayor and you should know:\n" + identifier.apply(4));
            sortedHandlerList.get(3).awake(message);
            for (int i = 4; i < sortedHandlerList.size(); i++) {
                message = getProperMessage("It's time to wakeup you are the " + roleGetter.apply(i) + ".");
                sortedHandlerList.get(i).asleep(message);
                wait(6000);
            }
        } catch (InterruptedException e) {
            System.err.println(e);
        }
    }

    /**
     * this methods starts the timer
     * @param totalPeriod total initial time
     */
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
        try {
            clock.start();
            clock.join();
        } catch (InterruptedException e) {
            System.err.println(e);
        }
    }

    /**
     * switch from day to night or visa versa
     */
    public void switchDay() {
        if (isDay)
            sortedHandlerList.forEach(handler -> handler.asleep("It's Night and everyone are asleep."));
        else
            sortedHandlerList.forEach(handler -> handler.awake("It's Day and everyone are awake."));
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
    public ArrayList<Server.ClientHandler> sortHandlersAndClients(ArrayList<Server.ClientHandler> clientHandlers) {
        while (sortedHandlerList.size() != clientHandlers.size()) {
            for (var clientHandler : clientHandlers) {
                if (clientHandler.getClient().getCharacter() == GameCharacter.GODFATHER)
                    sortedHandlerList.add(clientHandler);
                if (clientHandler.getClient().getCharacter() == GameCharacter.DOCTORLECTOR)
                    sortedHandlerList.add(clientHandler);
                if (clientHandler.getClient().getCharacter() == GameCharacter.MAFIA)
                    sortedHandlerList.add(clientHandler);
                if (clientHandler.getClient().getCharacter() == GameCharacter.MAYOR)
                    sortedHandlerList.add(clientHandler);
                if (clientHandler.getClient().getCharacter() == GameCharacter.DOCTOR)
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
        sortedHandlerList.forEach((handler) -> sortedClientList.add(handler.getClient()));
        return sortedHandlerList;
    }

    /**
     * this method is a getter
     * @return sorted list of handlers
     */
    public ArrayList<Server.ClientHandler> getSortedHandlerList() {
        return sortedHandlerList;
    }

    /**
     * this is a getter
     * @return sorted ArrayList of all alive players or clients
     */
    public ArrayList<Client> getSortedClientList() {
        return sortedClientList;
    }
}
//TODO Doctor can't treat him self more than once