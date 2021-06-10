package com.midTerm;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;

//----------------------------------------
// this class holds game related status and method
public class Game {
    // this is the server which games is running on
    // list of all characters of the game which are sorted
    // players is the list of all players which game started with
    // sortedHandlerList is sorted list of client handlers(it is sorted in awake hierarchy order)
    // sortedClientList is sorted list of client (it is sorted in awake hierarchy order)
    // <poleResult> is the final result of the game
    // <isIntroduction> indicates if it is introduction day
    // isDay check if its night or day
    // timer is the time left from the section of the game
    private final Server server;
    private final Stack<GameCharacter> characters;
    private final ArrayList<Client> sortedClientList;
    private final ArrayList<Server.ClientHandler> deadHandlers;
    private final HashMap<GameCharacter, String> repliesDuringTheNight;
    private final HashMap<String, Server.ClientHandler> handlersClientUsername;
    private HashMap<GameCharacter, Server.ClientHandler> handlerByCharacter;
    private ArrayList<Server.ClientHandler> sortedHandlerList;
    private ArrayList<GameCharacter> deadCharacters;
    private String poleResult;
    private boolean isIntroductionDay;
    private boolean isDay;
    private int timer;

    /**
     * this is a constructor
     */
    public Game(Server server) {
        isDay = true;
        poleResult = "";
        this.server = server;
        isIntroductionDay = true;
        characters = new Stack<>();
        deadHandlers = new ArrayList<>();
        deadCharacters = new ArrayList<>();
        sortedClientList = new ArrayList<>();
        sortedHandlerList = new ArrayList<>();
        repliesDuringTheNight = new HashMap<>();
        handlersClientUsername = new HashMap<>();
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
        mapHandlersToCharacters();
        indicateDeadPlayers();
        if (isGameFinished()) {
            return;
        } else if (isIntroductionDay) {
            switchDay();
            startIntroductionNightProgress();
            isIntroductionDay = false;
        } else if (isDay && !isGameFinished()) {
            switchDay();
            refresh();
            startNightProgress();
        } else if (!isGameFinished()){
            switchDay();
            refresh();
            startDayProgress();
        }
        next();
    }

    /**
     * this method clean old records
     */
    private void refresh() {
        poleResult = "";
        sortedHandlerList.forEach(Server.ClientHandler::setEmptyVoted);
    }

    /**
     * this method starts the night progress
     */
    private void startNightProgress() {
        mapHandlersToCharacters();
        awakeMafiasForExecution();
        awakeDoctorsForTreatment();
        var handler = handlerByCharacter.get(GameCharacter.DETECTOR);
        if (handler != null) {
            handler.getVote(sortedHandlerList, Game.getProperMessage("Who To Be inquired?"));
            sendListOfAllALivePlayers();
            startTimer(20);
            showTheResultOfQuery(handler);
            handler.silent();
        }
        handler = handlerByCharacter.get(GameCharacter.PROFESSIONAL);
        if (handler != null) {
            handler.getVote(sortedHandlerList, Game.getProperMessage("Who To Be Killed?"));
            sendListOfAllALivePlayers();
            startTimer(20);
            handler.silent();
            handler.asleep(getProperMessage("You are now asleep."));
        }
        handler = handlerByCharacter.get(GameCharacter.MENTALIST);
        if (handler != null) {
            handler.getVote(sortedHandlerList, Game.getProperMessage("Who to be muted?"));
            sendListOfAllALivePlayers();
            startTimer(20);
            handler.asleep(getProperMessage("You are now asleep."));
            handler.silent();
        }
        handler = handlerByCharacter.get(GameCharacter.DIEHARD);
        if (handler != null &&
            handler.getClient().getCharacter().getConstraint() != 0) {
            handler.getConfirmation(Game.getProperMessage("Do you want me to reveal dead characters?(yes/no)"));
            startTimer(20);
            handler.asleep(getProperMessage("You are now asleep."));
            handler.silent();
        }
        //TODO die hard can only get dead characters for 2 times
        collectRepliesSentNight();
        handleRepliesInNight();
    }

    /**
     * this method shows the result of query to the detective
     */
    private void showTheResultOfQuery(Server.ClientHandler handler) {
        if (!handler.getVoted().equals("")) {
            var character = handlersClientUsername.get(handler.getVoted()).getClient().getCharacter();
            if (character.isCitizen() || character == GameCharacter.GODFATHER) {
                server.notifyAwakeAndAliveClients(Game.getProperMessage("It's a CITIZEN"));
            } else {
                server.notifyAwakeAndAliveClients(Game.getProperMessage("It's a Mafia"));
            }
        }
    }

    /**
     * this method awakes the doctors so they cure someone
     */
    private void awakeDoctorsForTreatment() {
        ArrayList<Server.ClientHandler> mafiasList = new ArrayList<>();
        sortedHandlerList.forEach((handler) -> {
            if (!handler.getClient().getCharacter().isCitizen())
                mafiasList.add(handler);
        });
        var goodDoctor = handlerByCharacter.get(GameCharacter.DOCTOR);
        var badDoctor = handlerByCharacter.get(GameCharacter.DOCTORLECTOR);
        if (goodDoctor != null && goodDoctor.isAlive()) {
            goodDoctor.awake("Wakeup it's time to Cure someone.");
            goodDoctor.getVote(sortedHandlerList, getProperMessage("Who To Be Cured?"));
            sendListOfAllALivePlayers();
            startTimer(20);
            goodDoctor.asleep(getProperMessage("You are now asleep."));
            goodDoctor.silent();
        }
        if (badDoctor != null && badDoctor.isAlive()) {
            badDoctor.awake("Wakeup it's time to Cure One Of Your mates.");
            badDoctor.getVote(mafiasList, getProperMessage("Who To Be Cured?"));
            sendListOfAllALivePlayers();
            startTimer(20);
            badDoctor.asleep(getProperMessage("You are now asleep."));
            badDoctor.silent();
        }
    }

    /**
     * this method awakes the mafias so they can execute someone
     */
    private void awakeMafiasForExecution() {
        ArrayList<Server.ClientHandler> mafias = new ArrayList<>(Arrays.asList(
                handlerByCharacter.get(GameCharacter.MAFIA),
                handlerByCharacter.get(GameCharacter.DOCTORLECTOR),
                handlerByCharacter.get(GameCharacter.GODFATHER)));
        for (var handler : mafias) {
            if (handler != null) {
                handler.awake(Game.getProperMessage("Wakeup it's time to Execute someone."));
                handler.getVote(sortedHandlerList, Game.getProperMessage("Who To Be Executed?"));
            }
        }
        sendListOfAllALivePlayers();
        startTimer(30);
        for (var handler : mafias) {
            if (handler != null) {
                handler.asleep(Game.getProperMessage("You are now asleep."));
                handler.silent();
            }
        }
    }

    /**
     * handle actions of the player's during the night
     */
    public void handleRepliesInNight() {
        var username  = repliesDuringTheNight.get(GameCharacter.GODFATHER);
        if (username.length() != 0)
            handlersClientUsername.get(repliesDuringTheNight.get(GameCharacter.GODFATHER)).kill();

        username = repliesDuringTheNight.get(GameCharacter.PROFESSIONAL);
        if (username != null && username.length() != 0) {
            var targetedHandler = handlersClientUsername.get(repliesDuringTheNight.get(GameCharacter.PROFESSIONAL));
            if (targetedHandler.getClient().getCharacter().isCitizen())
                handlerByCharacter.get(GameCharacter.PROFESSIONAL).kill();
            else
                targetedHandler.kill();
        }

        username = repliesDuringTheNight.get(GameCharacter.DOCTOR);
        if (username != null && username.length() != 0)
            handlersClientUsername.get(repliesDuringTheNight.get(GameCharacter.DOCTOR)).cure();

        username = repliesDuringTheNight.get(GameCharacter.DOCTORLECTOR);
        if (username != null && username.length() != 0)
            handlersClientUsername.get(repliesDuringTheNight.get(GameCharacter.DOCTORLECTOR)).cure();

        username = repliesDuringTheNight.get(GameCharacter.MENTALIST);
        sortedHandlerList.forEach(Server.ClientHandler::unmute);
        if (username != null && username.length() != 0)
            handlersClientUsername.get(repliesDuringTheNight.get(GameCharacter.MENTALIST)).mute();
    }

    /**
     * this method update all of collections due to dead players
     */
    private void indicateDeadPlayers() {
        ArrayList<Server.ClientHandler> cloneSortedHandlerList = new ArrayList<>(sortedHandlerList);
        cloneSortedHandlerList.forEach(handler -> {
            if (!handler.isAlive()) {
                handler.getConfirmation(Game.getProperMessage("You are Killed.Do you want to continue the game as spectre?(Yes/No)"));
                sortedHandlerList.remove(handler);
                deadHandlers.add(handler);
                deadCharacters.add(handler.getClient().getCharacter());
                sortedClientList.remove(handler.getClient());
                handlerByCharacter.remove(handler.getClient().getCharacter());
            }
        });
    }

    /**
     * collect all replies from the night
     */
    private void collectRepliesSentNight() {
        for (var handler : sortedHandlerList) {
            repliesDuringTheNight.put(handler.getClient().getCharacter(), handler.getVoted());
        }
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
            clientHandler.getVote(sortedHandlerList, Game.getProperMessage("Who To Be Executed?"));
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
        sortedHandlerList.forEach(Server.ClientHandler::awake);
        var mayorsHandler = handlerByCharacter.get(GameCharacter.MAYOR);
        if (mayorsHandler != null) {
            if (poleResult.equals("") ||
                mayorsHandler.getVoted().equalsIgnoreCase("no")) {
                server.updateChatroom(
                        getProperMessage("Mayor denied the execution."));
                return;
            }
        }
        server.updateChatroom(getProperMessage("Mayor confirmed the execution."));
        var targetedHandler = handlersClientUsername.get(poleResult);
        targetedHandler.kill();
        targetedHandler.kill();
        indicateDeadPlayers();
        for (var handler : sortedHandlerList)
            handler.reportPoleResult(getProperMessage(
         targetedHandler.getClient().getUsername() +
            "(" + targetedHandler.getClient().getCharacter() + ") was executed."));
    }

    /**
     * report initial pole result
     */
    private void announcePoleResult() {
        for (var handler : sortedHandlerList) {
            if (poleResult.length() == 0) {
                handler.reportPoleResult(
                        getProperMessage("no one was voted."));
            } else {
                handler.reportPoleResult(
                        getProperMessage(poleResult + " was voted."));
            }
        }
    }

    /**
     * this method awake the mayor for confirming the execution of the voted client
     */
    private void callTheMayor() {
        if (poleResult != null && !poleResult.equals("")) {
            for (var handler : sortedHandlerList) {
                if (handler.getClient().getCharacter() == GameCharacter.MAYOR) {
                    handler.getConfirmation(Game.getProperMessage(poleResult + " was voted do You confirm the execution?(Yes/No)"));
                    startTimer(10);
                    break;
                }
            }
        }
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
        StringBuilder message = new StringBuilder("Alive players are as follows:\n");
        for (var handler : sortedHandlerList) {
            if (handler.isAlive())
                message.append(handler.getClient().getUsername()).append("\n");
        }
        server.notifyAwakeAndAliveClients(Game.getProperMessage(message.toString()));
    }

    /**
     * this method gets the final result of pole
     * @param countedVotes is the hashmap of counter number of votes
     */
    private void getPoleResult(HashMap<String, Integer> countedVotes) {
        prepareUsernameHandlerHashmap();
        int max = 0;
        for (var name : countedVotes.keySet()) {
            if (name != null && !name.equals("") && countedVotes.get(name) > max) {
                max = countedVotes.get(name);
                poleResult = name;
            }
        }
    }

    /**
     * this method prepare the handler-username hashmap
     */
    private void prepareUsernameHandlerHashmap() {
        for (var handler : sortedHandlerList)
            handlersClientUsername.put(handler.getClient().getUsername(), handler);
    }

    /**
     * this method count all of the votes and assign the result to the <result> field
     */
    private HashMap<String, Integer> countTheVotes() {
        HashMap<String, Integer> countedVotes = new HashMap<>();
        ArrayList<String> votes = (ArrayList<String>) sortedHandlerList.stream()
                .map(Server.ClientHandler::getVoted).collect(Collectors.toList());
        for (var vote : votes) {
            if (countedVotes.containsKey(vote))
                countedVotes.put(vote, countedVotes.get(vote) + 1);
            else
                countedVotes.put(vote, 1);
        }
        for (var handler : sortedHandlerList) {
            if (handler.getVoted().length() == 0)
                handler.incrementWarnings("You've just got a warning.\n");
        }
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
            StringBuilder message = new StringBuilder("You are now awake, mafias are as follows...\n");
            int count = 0;
            for (var handler : sortedHandlerList) {
                if (!handler.getClient().getCharacter().isCitizen())
                    message.append(identifier.apply(count));
                count++;
            }

            for (var handler : sortedHandlerList){
                if (!handler.getClient().getCharacter().isCitizen()) {
                    handler.awake(getProperMessage(message.toString()));
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

            message = new StringBuilder(getProperMessage("It's time to wakeup, you are the mayor and you should know:\n" + identifier.apply(count)));
            for (var handler : sortedHandlerList)
                if (handler.getClient().getCharacter() == GameCharacter.MAYOR) {
                    handler.awake(message.toString());
                    sleep(5000);
                    handler.asleep(getProperMessage("Your now asleep."));
                }

            for (int i = 1; i < sortedHandlerList.size(); i++) {
                var character = sortedHandlerList.get(i).getClient().getCharacter();
                if (character.isCitizen() && character != GameCharacter.MAYOR) {
                    message = new StringBuilder(getProperMessage("It's time to wakeup you are the " + roleGetter.apply(i) + "."));
                    sortedHandlerList.get(i).awake(message.toString());
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
        if (isDay) {
            sortedHandlerList.forEach(
                    handler -> handler.asleep("It's Night and everyone are asleep.\n"));
        } else {
            sortedHandlerList.forEach(
                    handler -> handler.awake("It's Day and everyone are awake.\n"));
            var isRequired = repliesDuringTheNight.get(GameCharacter.DIEHARD);
            if (isRequired != null &&
                isRequired.equalsIgnoreCase("yes"))
                revealDeadCharacters();
            repliesDuringTheNight.clear();
        }
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
     * this methods checks if a game is finished
     * @return true if game is finished
     */
    public boolean isGameFinished() {
        AtomicInteger numberOfMafias = new AtomicInteger();
        AtomicInteger numberOfCitizens = new AtomicInteger();
        sortedHandlerList.forEach((handler) -> {
            if (handler.getClient().getCharacter().isCitizen())
                numberOfCitizens.getAndIncrement();
            else
                numberOfMafias.getAndIncrement();
        });
        if (numberOfMafias.get() >= numberOfCitizens.get()) {
            showResultOfTheGame(Game.getProperMessage("Mafia Won!"));
            return true;
        } else if (!handlerByCharacter.containsKey(GameCharacter.GODFATHER)) {
            showResultOfTheGame(Game.getProperMessage("Citizens Won! City is Safe Now!!!!"));
            return true;
        }
        return false;
    }

    /**
     * this method get result of the game
     * @param properMessage is proper message which will be sent for other clients
     */
    public void showResultOfTheGame(String properMessage) {
        sortedHandlerList.forEach((handler) -> {
            handler.writeMessage(properMessage);
        });
        deadHandlers.forEach((handler) -> {
            handler.writeMessage(properMessage);
        });
    }

    /**
     * this method map the characters to the handlers
     */
    private void mapHandlersToCharacters() {
        handlerByCharacter = new HashMap<>();
        for (var handler : sortedHandlerList)
            handlerByCharacter.put(handler.getClient().getCharacter(), handler);
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

    /**
     * this method add to dead handlers
     * @param clientHandler is the handler which will be added to dead handlers
     */
    public void addDeadHandlers(Server.ClientHandler clientHandler) {
        deadHandlers.add(clientHandler);
    }

    /**
     * this method remove the client handler from dead handlers
     * @param clientHandler is the dead handler which will be removed from the list of dead handlers
     */
    public void removeDeadHandlers(Server.ClientHandler clientHandler) {
        deadHandlers.remove(clientHandler);
    }

    /**
     * this is a getter
     * @return dead handlers of the game
     */
    public ArrayList<Server.ClientHandler> getDeadHandlers() {
        return deadHandlers;
    }

    /**
     * this method remove a client completely from the database
     * @param clientHandler is the handler intended to be removed
     */
    public void completelyRemove(Server.ClientHandler clientHandler) {
        sortedHandlerList.remove(clientHandler);
        deadHandlers.remove(clientHandler);
        handlerByCharacter.remove(clientHandler.getClient().getCharacter());
        sortedClientList.remove(clientHandler.getClient());
    }
}
//TODO Doctor can't treat him self more than once