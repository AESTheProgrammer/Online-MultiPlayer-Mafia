package com.midTerm;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class Server implements Serializable{
    // client handlers of this server
    // game is game which currently running on the server
    // executor is used for running a pool of threads
    // port is the port of the server
    // saved chat is the chats which are saved during the game(which were public not private)
    // ID is unique id of this game
    // characterResource if it is not empty we use it as only source of character
    private final ArrayList<ClientHandler> clientHandlers = new ArrayList<>(1);
    private transient final Game game;
    private transient final ExecutorService executor = Executors.newCachedThreadPool();
    private transient int port;
    private String ID;
    private transient ArrayList<ClientHandler> charactersResource = null;

    /**
     * this is a constructor
     * @param port is the port number of the server
     */
    public Server(int port) {
        this.port = port;
        game = new Game(this);
    }

    /**
     * is the main method which runs the program
     * @param args are arbitrarily strings which might be give to main
     */
    public static void main(String[] args) {
        Server server = new Server(7660);
        try (ServerSocket welcomingSocket = new ServerSocket(server.port)) {
            System.out.println("Server Started");
            int count = 0;
            while ((count < 3 || !server.isAllClientsReady()) && count < 10) {
                if (server.charactersResource != null) {
                    if (count == server.charactersResource.size())
                        break;
                }
                Socket connectionSocket = welcomingSocket.accept();
                count++;
                Client client = new Client();
                ClientHandler clientHandler = new ClientHandler(client, connectionSocket, server, server.game);
                server.clientHandlers.add(clientHandler);
                server.executor.execute(clientHandler);
                if (count == 1) {
                    Thread.sleep(500);
                    waitForLoadRequest(clientHandler);
                } else {
                    Thread.sleep(10000);
                }
            }
            server.handoutCharacters();
            server.game.sortHandlersAndClients(server.clientHandlers);
            while (!server.isAllClientsReady())
                Thread.sleep(1000);
            printUsersCharacter(server);
            server.game.start();
        } catch (IOException | InterruptedException e) {
            System.err.println(e);
            e.printStackTrace();
        }
    }

    /**
     * this method saves the game
     */
    private String save() throws IOException {
        ID = UUID.randomUUID().toString();
        GameSaver.saveGame(this);
        return ID;
    }

    /**
     * @param handler is the hosts handler
     * @throws InterruptedException handle an error
     */
    private static void waitForLoadRequest(ClientHandler handler) throws InterruptedException {
        handler.writeMessage(Game.getProperMessage(
                     ConsoleColors.YELLOW_BOLD + "You are Host of the Game\n" +
                       ConsoleColors.YELLOW_BOLD + "You can save the game by Entering \"SAVE\"\n" +
                       ConsoleColors.YELLOW_BOLD + "Or Load a Game BY entering \"LOAD <ID>\" In the next 30 seconds" + ConsoleColors.RESET));
        Thread.sleep(30000);

    }

    /**
     * this method prints users with their characters
     * @param server server of those users
     */
    private static void printUsersCharacter(Server server) {
        server.clientHandlers.forEach(handler ->
            System.out.println( "CHARACTER: " + handler.getClient().getCharacter() +
                                "    Username: " + handler.getClient().getUsername())
        );
    }

    /**
     * @return true if all clients are ready
     */
    public boolean isAllClientsReady() {
        return clientHandlers.stream().allMatch(handler -> handler.isReady);
    }

    /**
     * this method handout characters to the clients randomly
     */
    private void handoutCharacters() {
        if (charactersResource == null) {
            Collections.shuffle(clientHandlers);
            for (var handler : clientHandlers) {
                var randomRole = game.getCharacterInstance();
                handler.getClient().setCharacter(randomRole);
            }
        } else {
            for(var i = 0; i < clientHandlers.size(); i++) {
                clientHandlers.get(i).getClient().setCharacter
                (charactersResource.get(i).getClient().getCharacter());
            }
        }
    }

    /**
     * this method updates the chatroom for all players
     * @param newMessage is the new message which will be added to the chatroom
     */
    public void updateChatroom(String newMessage) {
        notifyAwakeAndAliveClients(newMessage + "\n");
        notifySpecters(newMessage + "\n");
    }

    /**
     * this method notify specters from new messages
     * @param newMessage is the new message which will be sent to the specter
     */
    private void notifySpecters(String newMessage) {
        for (var handler : game.getDeadHandlers()) {
            handler.writeMessage(newMessage + "\n");
        }
    }

    /**
     * checks if the given username is valid or not
     * @param username is the username of the which is given to be checked
     * @return true if it satisfies criterion else false
     */
    private boolean isValidUsername(String username) {
        return username.length() != 0 &&
        clientHandlers.stream().noneMatch((handler) ->
        handler.getClient().getUsername().equalsIgnoreCase(username));
    }

    /**
     * this method notify awake clients.
     * @param newMessage is the new message which will
     * be sent to the players chat and will be saved there
     */
    public void notifyAwakeAndAliveClients(String newMessage) {
        for (var handler : clientHandlers)
            if (handler.isAwake && handler.isAlive)
                handler.writeMessage(newMessage + "\n");
    }

    /**
     * @return true if 2 id are the same
     */
    public boolean checkEquality(String ID) {
        return ID.equals(this.ID);
    }

    /**
     * this method tries to load the game
     * @return true if it was successful else false
     */
    private boolean load(String ID) {
        var server = GameSaver.loadGame(ID);
        if (server != null) {
            this.ID = ID;
            charactersResource = server.clientHandlers;
            return true;
        }
        return false;
    }

    /**
     * this is a getter
     * @return port of the server
     */
    public int getPort() {
        return port;
    }

    //----------------------------------------
    // this class handles the client
    protected static class ClientHandler implements Runnable, Serializable{
        // game is the game engine which runs the game
        // server is the server which games run on it
        // client is the client which this handler handler
        // connectionSocket is the socket which makes the connection between client
        // inputStream is the input stream which get inputs with it
        // outputStream is the stream which is used for sending messages for client
        // reader reads string from the buffer
        // isReady indicates whether the player is ready or not
        // isMuted indicates whether the player is muted or not
        // isConfirmation indicates whether the player should confirm sth or not
        // isVoting indicates whether the player should vote or not
        // isAlive indicates whether the player is alive or not
        // isClientConnected indicates whether the player is connected to the server or not
        // warnings is the number of warnings gotten until now
        // voted is the last vote of this player
        // listOfHandlersForVoting is the list which is used for voting to intended players
        private transient final Game game;
        private transient final Server server;
        private final Client client;
        private transient final Socket connectionSocket;
        private transient InputStream  inputStream;
        private transient OutputStream outputStream;
        private transient BufferedReader reader;
        private transient boolean isReady;
        private transient boolean isMuted;
        private transient boolean isAwake;
        private transient boolean isConfirmation;
        private transient boolean isVoting;
        private transient boolean isAlive;
        private transient boolean isClientConnected;
        private transient byte warnings;
        private transient String voted;
        private transient ArrayList<Server.ClientHandler> listOfHandlersForVoting;

        /**
         * this is a constructor
         * @param client is the client of the handler
         * @param connectionSocket is the connection Socket with client
         * @param server is the server which handles the hole game
         * @param game is the engine of the game
         */
        public ClientHandler(Client client,
                             Socket connectionSocket,
                             Server server,
                             Game game) {
            this.server = server;
            this.game = game;
            this.client = client;
            this.connectionSocket = connectionSocket;
            voted = "";
            warnings = 0;
            isReady = false;
            isVoting = false;
            isConfirmation = false;
            isAwake = true;
            isAlive = true;
        }

        /**
         * this method runs the client handler
         */
        @Override
        public void run() {
            try {
                inputStream = connectionSocket.getInputStream();
                outputStream = connectionSocket.getOutputStream();
                reader = new BufferedReader(new InputStreamReader(inputStream));
                if (server.clientHandlers.size() == 1) {
                    getLoadRequest();
                }
                getUsername();
                reportClientConnection();
                startMessageListener();
                while (isClientConnected)
                    Thread.sleep(1000);
                isAlive = false;
                game.completelyRemove(this);
            } catch (IOException | InterruptedException ignored) {}
        }

        /**
         * this method handles a load request
         */
        private void getLoadRequest() throws IOException {

            AtomicInteger timer = new AtomicInteger(30);
            Thread thread = new Thread(() ->{
                while (timer.get() > 0) {
                    try {
                        timer.getAndDecrement();
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {}
                }
            });
            thread.start();

            var loadRequest = "";
            while (timer.get() > 0 && (loadRequest = reader.readLine()) != null)  {
                if (loadRequest.contains("LOAD") &&
                    server.load(loadRequest.substring(loadRequest.indexOf("D") + 2))) return;
                else if (!loadRequest.equals(""))
                    writeMessage(Game.getProperMessage("Invalid Request"));
            }
        }

        /**
         * this method get a username from the server
         * @throws IOException is an Exception which might be thrown
         */
        private void getUsername() throws IOException {
            var inputUsername = "";
            writeMessage(Game.getProperMessage("Enter Your Username"));
            while ((inputUsername = reader.readLine()) != null)  {
                if (!inputUsername.equals("") && !server.isValidUsername(inputUsername))
                    writeMessage(Game.getProperMessage(inputUsername + " is already in use by another player."));
                else if (!inputUsername.equals("")){
                    server.updateChatroom(Game.getProperMessage(inputUsername.trim().strip() + " has been connected."));
                    client.setUsername(inputUsername.trim().strip());
                    return;
                }
            }
        }

        /**
         * this method runs the listener of the message
         * @throws InterruptedException is an exception which might be thrown as a result of stopping the thread
         */
        private void startMessageListener() throws InterruptedException {
            Thread listener = new Thread(this::readMessage);
            listener.start();
            listener.join();
        }

        /**
         * this method writes message to output by awaking it
         * @param properMessage is a message which should satisfies some criterion
         */
        public void writeMessage(String properMessage) {
            try {
                outputStream.write(properMessage.getBytes());
            } catch(IOException ignored) {
            }
        }

        /**
         * this method reads message from input not stop
         */
        private void readMessage() {
            String line;
            try {
                while (warnings < 3 && isClientConnected &&
                        (line = reader.readLine()) != null &&
                        !line.trim().strip().equalsIgnoreCase("exit")) {
                    if (isAwake) {
                        if (!isVoting && !isConfirmation && isAlive) {
                            if (line.trim().strip().equalsIgnoreCase("ready")) {
                                isReady = true;
                            server.updateChatroom(client.getUsername() + ": " + line);
                            }else if (line.trim().strip().equals("SAVE")) {
                                writeMessage(Game.getProperMessage("You can Load the game Later Using this ID: " + server.save()));
                                server.updateChatroom(client.getUsername() + ": " + line);
                            } else if (!isMuted) {
                                server.updateChatroom(client.getUsername() + ": " + line);
                            }
                        } else if (isConfirmation && !isVoting) {
                            voted = "";
                            line = line.strip().trim();
                            if (isAlive && line.equalsIgnoreCase("yes")) {
                                writeMessage(Game.getProperMessage("You Admitted."));
                                voted = "yes";
                                if (client.getCharacter() == GameCharacter.DIEHARD)
                                    client.getCharacter().decreaseConstraint();
                            } else if (isAlive && line.equalsIgnoreCase("NO")) {
                                writeMessage(Game.getProperMessage("You Refused."));
                                voted = "no";
                            }
                            if (!isAlive && line.equalsIgnoreCase("no")) {
                                writeMessage(Game.getProperMessage("You Refused."));
                                reportClientDisconnection();
                                game.completelyRemove(this);
                                return;
                            } else if (!isAlive && line.equalsIgnoreCase("yes")) {
                                writeMessage(Game.getProperMessage("You Admitted."));
                            }
                        } else if (isVoting && !isConfirmation && isAlive) {
                            for (var handler : listOfHandlersForVoting) {
                                var str = line.trim().strip();
                                if (handler.getClient().getUsername().equalsIgnoreCase(str)) {
                                    if (!str.equalsIgnoreCase(client.getUsername())) {
                                        voted = line.trim().strip();
                                        server.updateChatroom(Game.getProperMessage(
                                                this.client.getUsername() + " voted " + voted));
                                        break;
                                    } else if (client.getCharacter().getConstraint() == 1 && !game.isDay() && (
                                               client.getCharacter() == GameCharacter.DOCTORLECTOR ||
                                               client.getCharacter() == GameCharacter.DOCTOR)) {
                                        voted = line.trim().strip();
                                        server.updateChatroom(Game.getProperMessage(
                                                this.client.getUsername() + " cured " + voted));
                                        client.getCharacter().decreaseConstraint();
                                        break;
                                    }
                                }
                            }
                            if (voted.length() == 0)
                                writeMessage(Game.getProperMessage("Invalid Username, Please Try Again."));
                        }
                    }
                }
                reportClientDisconnection();
                server.updateChatroom(client.getUsername() + " has been disconnected.");
                } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * this report clients' disconnection
         */
        private void reportClientDisconnection() {
            isClientConnected = false;
            isAwake = false;
            isAlive = false;
            System.out.println("Player <" + client.getUsername() + "> Disconnected");
        }

        /**
         * this report clients' connection
         */
        private void reportClientConnection() {
            isClientConnected = true;
            isAlive = true;
            System.out.println("Player <" + client.getUsername() + "> Connected");
        }

        /**
         * this is a getter
         * @return client of this handler
         */
        public Client getClient() {
            return client;
        }

        /**
         * this method awake the client
         * @param message is the message which will be sent to the client.
         */
        public void awake(String message) {
            if (isAlive) {
                isAwake = true;
                writeMessage(message);
            }
        }

        /**
         * this method awake the client
         */
        public void awake() {
            isAwake = true;
        }

        /**
         * this method put client to sleep
         * @param message is the message which will be sent to the client
         */
        public void asleep(String message) {
            isConfirmation = false;
            isVoting = false;
            isAwake = false;
            writeMessage(message);
        }

        /**
         * this method get a vote from client
         */
        public void getVote(ArrayList<ClientHandler> listForVotingFrom, String message) {
            if (isAlive) {
                listOfHandlersForVoting = listForVotingFrom;
                isVoting = true;
                isAwake = true;
                voted = "";
                writeMessage(message);
            }
        }

        /**
         * this is a getter
         * @return username of the voted client
         */
        public String getVoted() {
            return voted;
        }

        /**
         * this method get an acceptation from the mayor if the voted person could be executed
         */
        public void getConfirmation(String question) {
            isConfirmation = true;
            isAwake = true;
            writeMessage(question);
        }

        /**
         * this method silent the client with no quote
         */
        public void silent() {
            isVoting = false;
            isAwake = false;
        }

        /**
         * this method send a report to all client to announce the pole result
         * @param finalPoleResult is the username of the client which was voted
         */
        public void reportPoleResult(String finalPoleResult) {
            writeMessage(finalPoleResult);
        }

        /**
         * this method increment number of warnings by one
         */
        public void incrementWarnings(String message) {
            writeMessage(message);
            warnings++;
        }

        /**
         * this method kills the client
         */
        public void kill() {
            client.getCharacter().decreaseLive();
            if (client.getCharacter().getLives() == 0) {
                isAlive = false;
            }
        }

        /**
         * this method cure the player
         */
        public void cure() {
            if (client.getCharacter().getLives() == 0) {
                isAlive = true;
                client.getCharacter().increaseLives();
            }
        }

        /**
         * this is a getter
         * @return true if the client is alive otherwise false
         */
        public boolean isAlive() {
            return isAlive;
        }

        /**
         * this method mutes the client
         */
        public void mute() {
            isReady = true;
            isMuted = true;
        }

        /**
         * this method unmute the client
         */
        public void unmute() {
            isMuted = false;
        }

        /**
         * set voted empty
         */
        public void setEmptyVoted() {
            voted = "";
        }

        /**
         * this method makes and return state of the player
         * @return current state of the player
         */
        public String getStatus() {
            if (isAlive() && !isMuted) {
                return "Alive";
            } else if (isAlive() && isMuted) {
                return "Alive & Mute";
            } else {
                return "Dead & Mute";
            }
        }

        /**
         * put client in not ready state
         */
        public void unReady() {
            isReady = false;
        }

        /**
         * @return true if ready else false
         */
        public boolean isReady() {
            return isReady;
        }
    }
}