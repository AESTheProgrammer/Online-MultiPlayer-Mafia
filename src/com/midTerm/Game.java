package com.midTerm;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;

//----------------------------------------
// this class holds game related status and method
public class Game {
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
    private ArrayList<GameCharacter> deadCharacters;
    private ArrayList<String> repliesFromClientsDuringTheNight;
    private HashMap<String, Server.ClientHandler> handlersClientUsername;
    private String poleResult = null;
    private Server server;
    private boolean isIntroductionDay;
    private boolean isDay;
    private int timer;

    /**
     * this is a constructor
     */
    public Game(Server server) {
        this.server = server;
        isDay = true;
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
        characters.add(GameCharacter.MAFIA);
        characters.add(GameCharacter.CITIZEN);
        characters.add(GameCharacter.CITIZEN);
        characters.add(GameCharacter.CITIZEN);
        characters.add(GameCharacter.DOCTORLECTOR);
        characters.add(GameCharacter.PROFESSIONAL);
        characters.add(GameCharacter.MENTALIST);
        characters.add(GameCharacter.DIEHARD);
        characters.add(GameCharacter.MAYOR);
        characters.add(GameCharacter.DOCTOR);
        characters.add(GameCharacter.DETECTOR);
        characters.add(GameCharacter.GODFATHER);
    }

    /**
     * this method starts the next stage of the game.
     */
    public void next() {
        switchDay();
        startIntroductionNightProgress();
        switchDay();
        startDayProgress();
//        if (isGameFinished()) {
//            showResultOfTheGame();
//            return;
//        } else if (isIntroductionDay) {
//            switchDay();
//            startIntroductionNightProgress();
//        } else if (isDay) {
//            switchDay();
//            startNightProgress();
//        } else {
//            switchDay();
//            startDayProgress();
//        }
//        next();
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

        sortedHandlerList.get(8).getConfirmation(Game.getProperMessage("Do you want me to reveal dead characters?"));
        startTimer(20);
        sortedHandlerList.get(8).silent();
    }

    /**
     * this method gets a string as a reply from the client
     * @param reply is the reply from the client  during the night in regard of his character
     */
    public void addReply(String reply) {
        repliesFromClientsDuringTheNight.add(reply);
    }

    /**
     * handle actions of the player's during the night
     */
    public void handleRepliesInNight() {
        handlersClientUsername.get(repliesFromClientsDuringTheNight.get(0)).kill();
        handlersClientUsername.get(repliesFromClientsDuringTheNight.get(4)).kill();
        handlersClientUsername.get(repliesFromClientsDuringTheNight.get(1)).cure();
        handlersClientUsername.get(repliesFromClientsDuringTheNight.get(2)).cure();
        handlersClientUsername.get(repliesFromClientsDuringTheNight.get(5)).silent();
        if (repliesFromClientsDuringTheNight.get(5).equalsIgnoreCase("yes"))
            revealDeadCharacters();
        repliesFromClientsDuringTheNight.clear();
    }

    /**
     * reveals all dead characters for players
     */
    private void revealDeadCharacters() {
        var message = "Dead characters are as follows:\n";
        Collections.shuffle(deadCharacters);
        for (var character : deadCharacters)
            message += character.toString() + "\n";
        server.updateChatroom(message);
    }

    /**
     * add to dead characters
     * @param character which is dead and is will be added to dead characters list.
     */
    public void updateDeadCharacters(GameCharacter character) {
        deadCharacters.add(character);
    }

    /**
     * this method assign a hash map with handler key and name value
     */
    private void getHandlerUsernameHashmap() {
        sortedHandlerList.forEach(
                handler -> handlersClientUsername.put(handler.getClient().getUsername(), handler));
    }

    /**
     * this method control the daytime progress
     */
    private void startDayProgress() {
        startTimer(30);
        makePole();
    }

    /**
     * this method makes a pole for voting to suspicious person
     */
    private void makePole() {
        sendListOfAllALivePlayers();
        for (var clientHandler : sortedHandlerList)
            clientHandler.getVote(Game.getProperMessage("Who To Be Executed?"));
        startTimer(30);
        getPoleResult(countTheVotes());
        announcePoleResult();
        sortedHandlerList.forEach(Server.ClientHandler::silent);
        callTheMayor();
        announceFinalPoleResult();
        //TODO you might want to do it in the end
    }

    /**
     * this method report the poles result with character and username
     */
    private void announceFinalPoleResult() {
        for (var handler : sortedHandlerList)
            if (handler.getClient().getCharacter() == GameCharacter.MAYOR)
                if (poleResult.equals("") || handler.getVoted().equalsIgnoreCase("no"))
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
        for (var handler : sortedHandlerList)
            if (handler.getClient().getCharacter() == GameCharacter.MAYOR)
                handler.getConfirmation(poleResult);
        startTimer(30);
    }

    /**
     * this method print boarder of each players page
     */
    private void printBoarder() {
        for (var handler : sortedHandlerList) {
            var boarder = "TIME LEFT: " + timer + "        STATUS: " + handler.isAlive() +
                    "        LIVES: " + handler.getClient().getCharacter().getLives() + "\n" +
                    "&for more information about your character enter -info";
            // TODO this is not complete yet
            handler.writeMessage(boarder);
        }
    }

    /**
     * this method list of all alive players to every player
     */
    private void sendListOfAllALivePlayers() {
        var message = "Alive players are as follows:\n";
        for (var handler : sortedHandlerList) {
            if (handler.isAlive())
                message += handler.getClient().getUsername() + "\n";
        }
        server.updateChatroom(message);
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
            if (countedVotes.containsKey(vote))
                countedVotes.put(vote, countedVotes.get(vote) + 1);
            else
                countedVotes.put(vote, 1);
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
                        sortedHandlerList.get(index).getClient().getCharacter().toString()+ "\n";
        Function<Integer, String> roleGetter = (Integer index) ->
                sortedHandlerList.get(index).getClient().getCharacter().toString();

        try {
            var message = "You are now awake, mafias are as follows...\n";
            int count = 0;
            for (var handler : sortedHandlerList) {
                if (!handler.getClient().getCharacter().isCitizen())
                    message += identifier.apply(count);
                count++;
            }

            for (var handler : sortedHandlerList){
                if (!handler.getClient().getCharacter().isCitizen()) {
                    handler.awake(getProperMessage(message));
                    sleep(5000);
                    handler.asleep(getProperMessage("Your now asleep."));
                }
            }

            count = 0;
            for (var handler : sortedHandlerList) {
                if (handler.getClient().getCharacter() == GameCharacter.DOCTOR)
                    break;
                count++;
            }

            message = getProperMessage("It's time to wakeup, you are the mayor and you should know:\n" + identifier.apply(count));
            for (var handler : sortedHandlerList)
                if (handler.getClient().getCharacter() == GameCharacter.MAYOR) {
                    handler.awake(message);
                    sleep(5000);
                    handler.asleep(getProperMessage("Your now asleep."));
                }

            for (int i = 1; i < sortedHandlerList.size(); i++) {
                var character = sortedHandlerList.get(i).getClient().getCharacter();
                if (character.isCitizen() && character != GameCharacter.MAYOR) {
                    message = getProperMessage("It's time to wakeup you are the " + roleGetter.apply(i) + ".");
                    sortedHandlerList.get(i).awake(message);
                    sleep(5000);
                    sortedHandlerList.get(i).asleep(getProperMessage("Your now asleep."));
                }
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
            while (timer > 0) {
                try {
                    timer--;
                    sleep(1000);
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
            sortedHandlerList.forEach(handler -> handler.asleep("It's Night and everyone are asleep.\n"));
        else
            sortedHandlerList.forEach(handler -> handler.awake("It's Day and everyone are awake.\n"));
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
     * @return true if game is finished
     */
    public boolean isGameFinished() {
        return false;
    }

    /**
     * this method get result of the game
     */
    public void showResultOfTheGame() {

    }

    /**
     * this method sort the client handlers in order which will be called during the night
     * @param clientHandlers is the initial list of client handlers
     */
    public void sortHandlersAndClients(ArrayList<Server.ClientHandler> clientHandlers) {
        sortedHandlerList = (ArrayList<Server.ClientHandler>) clientHandlers.stream().
                sorted(Comparator.comparingInt(
                handler -> handler.getClient().getCharacter().getPriority())).
                collect(Collectors.toList());
        sortedHandlerList.forEach((handler) -> sortedClientList.add(handler.getClient()));
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

    /**
     * this method return the status of the game
     * @return true if its day else false
     */
    public boolean isDay() {
       return isDay;
    }
}
//TODO Doctor can't treat him self more than once