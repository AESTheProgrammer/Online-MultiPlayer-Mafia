package com.midTerm;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Server {
    // clients of this server
    // client handlers of this server
    // sorted client handler list
    // game is game which currently running on the server
    // generator creates random number
    // lock is used for synchronization
    // executor is used for running a pool of threads
    // port is the port of the server
    // saved chat is the chats which are saved during the game(which were public not private)
    private final ArrayList<Client> clients = new ArrayList<>(1);
    private final ArrayList<ClientHandler> clientHandlers = new ArrayList<>(1);
    private ArrayList<ClientHandler> sortedClientHandlerList;
    private final Game game;
    private final SecureRandom generator = new SecureRandom();
    private final Lock lock = new ReentrantLock();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private int port;
    private String savedChat = "";

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
                Socket connectionSocket = welcomingSocket.accept();
                count++;
                Client client = new Client();
                ClientHandler clientHandler = new ClientHandler(client, connectionSocket, server, server.game);

                server.clients.add(client);
                server.clientHandlers.add(clientHandler);
                server.executor.execute(clientHandler);
                Thread.sleep(5000);
            }
            server.handoutCharacters();
            server.game.sortHandlersAndClients(server.clientHandlers);
            while (!server.isAllClientsReady())
                Thread.sleep(1000);
            server.game.next();
        } catch (IOException | InterruptedException e) {
            System.err.println(e);
            e.printStackTrace();
        }
    }

    /**
     * @return true if all clients are ready
     */
    private boolean isAllClientsReady() {
        return clientHandlers.stream().allMatch(handler -> handler.isReady);
    }

    /**
     * this method handout characters to the clients randomly
     */
    private void handoutCharacters() {
        Collections.shuffle(clientHandlers);
        for (var handler : clientHandlers) {
            var randomRole = game.getCharacterInstance();
            handler.getClient().setCharacter(randomRole);
        }
    }

    /**
     * this method updates the chatroom for all players
     * @param newMessage is the new message which will be added to the chatroom
     */
    public void updateChatroom(String newMessage) {
        try {
            lock.lock();
            savedChat += newMessage + "\n";
        } finally {
            lock.unlock();
        }
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
     * this method notify all clients with a new message
     * @param newMessage is the new message which will be sent to clients from the Narrator
     */
    private void notifyAllClients(String newMessage) {
        for (var handler: clientHandlers)
            handler.writeMessage(newMessage);
    }

    /**
     * this is a getter
     * @return the saved chat
     */
    private String getSavedChat() {
        return savedChat;
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

    //----------------------------------------
    // this class handles the client
    protected static class ClientHandler implements Runnable{

        private final Game game;
        private final Server server;
        private final Client client;
        private final Socket connectionSocket;
        private InputStream  inputStream;
        private OutputStream outputStream;
        private ObjectOutputStream objectOutputStream;
        private BufferedReader reader;
        private boolean isReady;
        private boolean isMuted;
        private boolean isAwake;
        private boolean isConfirmation;
        private boolean isVoting;
        private boolean isAlive;
        private boolean isClientConnected;
        private byte warnings;
        private String savedChat = "";
        private String voted;
        private final Scanner scanner = new Scanner(System.in);
        private ArrayList<Server.ClientHandler> listOfHandlersForVoting;

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
                objectOutputStream = new ObjectOutputStream(outputStream);
                reader = new BufferedReader(new InputStreamReader(inputStream));
                getUsername();
                reportClientConnection();
                startMessageListener();
                while (isClientConnected)
                    Thread.sleep(1000);
                isAlive = false;
                game.completelyRemove(this);
            } catch (IOException | InterruptedException e) {}
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
                        if (!isVoting && !isConfirmation && isAlive && !isMuted) {
                            if (line.trim().strip().equalsIgnoreCase("ready"))
                                isReady = true;
                            savedChat += client.getUsername() + ": " + line;
                            server.updateChatroom(client.getUsername() + ": " + line);
                        } else if (isConfirmation && !isVoting) {
                            voted = "";
                            line = line.strip().trim();
                            if (isAlive && line.equalsIgnoreCase("yes")) {
                                voted = "yes";
                                if (client.getCharacter() == GameCharacter.DIEHARD)
                                    client.getCharacter().decreaseConstraint();
                            } else if (isAlive && line.equalsIgnoreCase("NO")) {
                                voted = "no";
                            }
                            if (!isAlive && line.equalsIgnoreCase("no")) {
                                reportClientDisconnection();
                                game.completelyRemove(this);
                                return;
                            } else if (!isAlive && line.equalsIgnoreCase("yes")) {
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
                                    } else if (client.getCharacter().getConstraint() == 1 && (
                                               client.getCharacter() == GameCharacter.DOCTORLECTOR ||
                                               client.getCharacter() == GameCharacter.DOCTOR)) {
                                        voted = line.trim().strip();
                                        if (game.isDay()) {
                                            server.updateChatroom(Game.getProperMessage(
                                                    this.client.getUsername() + " voted " + voted));
                                        }
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
         * this method send an object to the client
         * @param object is the object which will be sent to the client.
         */
        public void sendObjectToClient(Object object) {
            try {
                objectOutputStream.writeObject(object);
            } catch (IOException e) {
                System.err.println(e);
            }
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
         * this method is a getter
         * @return true is ready else false
         */
        public boolean isReady() {
            return isReady;
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
    }
}