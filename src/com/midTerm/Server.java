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
    private final ArrayList<Client> clients = new ArrayList<>(1);
    private final ArrayList<ClientHandler> clientHandlers = new ArrayList<>(1);
    private ArrayList<ClientHandler> sortedClientHandlerList;
    private final Game game;
    private final SecureRandom generator = new SecureRandom();
    private final Lock lock = new ReentrantLock();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private int port;
    private String savedChat = "";

    public Server(int port) {
        this.port = port;
        game = new Game(this);
    }

    public static void main(String[] args) {
        Server server = new Server(7660);
        try (ServerSocket welcomingSocket = new ServerSocket(server.port)) {
            System.out.println("Server Started");
            int count = 0;
            while ((count < 3 || !server.isAllClientsReady()) && count < 4) {
                Socket connectionSocket = welcomingSocket.accept();
                count++;
                Client client = new Client();
                ClientHandler clientHandler = new ClientHandler(client, connectionSocket, server, server.game);

                server.clients.add(client);
                server.clientHandlers.add(clientHandler);
                server.executor.execute(clientHandler);
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
            handler.writeMessage(Game.getProperMessage("<character> " + randomRole.toString()));
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
        notifyAllClients(newMessage + "\n");
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

    private boolean isValidUsername(String username) {
        return username.length() != 0 &&
        clientHandlers.stream().noneMatch((handler) -> handler.getClient().getUsername().equalsIgnoreCase(username));
    }

    public void notifyAwakeClients(String newMessage) {
        for (var handler : clientHandlers)
            if (handler.isAwake)
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
        private boolean isAwake;
        private boolean isConfirmation;
        private boolean isVoting;
        private boolean isAlive;
        private boolean isClientConnected;
        private byte warnings;
        private String savedChat = "";
        private String voted;
        private final Scanner scanner = new Scanner(System.in);

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
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
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
            } catch(IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * this method reads message from input not stop
         */
        private void readMessage() {
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    if (warnings >= 3 || line.trim().strip().equalsIgnoreCase("exit")) {
                        reportClientDisconnection();
                        server.updateChatroom(client.getUsername() + " has been disconnected.");
                        return;
                    } else if (isAwake && isAlive) {
                        if (!isVoting && !isConfirmation) {
                            if (line.trim().strip().equalsIgnoreCase("ready"))
                                isReady = true;
                            savedChat += client.getUsername() + ": " + line;
                            server.updateChatroom(client.getUsername() + ": " + line);
                        } else if (isConfirmation && !isVoting) {
                            voted = "";
                            if (line.equalsIgnoreCase("yes")) {
                                isConfirmation = false;
                                server.updateChatroom(Game.getProperMessage("Mayor confirmed the execution."));
                                voted = "no";
                            } else if (line.equalsIgnoreCase("NO")) {
                                isConfirmation = false;
                                voted = "yes";
                                server.updateChatroom(Game.getProperMessage("Mayor denied the execution."));
                            }
                        } else if (isVoting && !isConfirmation) {
                            for (var client : game.getSortedClientList()) {
                                voted = line.trim().strip();
                                if (client.getUsername().equalsIgnoreCase(voted) &&
                                   !voted.equalsIgnoreCase(client.getUsername())){
                                    if (game.isDay()) {
                                        server.updateChatroom(Game.getProperMessage(
                                                this.client.getUsername() + " voted " + voted));
                                        break;
                                    } else {
                                        server.notifyAwakeClients(Game.getProperMessage(
                                                this.client.getUsername() + " cured " + voted));
                                        break;
                                    }
                                }
                            }
                            if (voted.length() == 0)
                                writeMessage(Game.getProperMessage("Invalid Username, Please Try Again."));
                        }
                    }
                }
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
            isAwake = true;
            writeMessage(message);
        }

        /**
         * this method put client to sleep
         * @param message is the message which will be sent to the client
         */
        public void asleep(String message) {
            isAwake = false;
            writeMessage(message);
        }

        /**
         * this method get a vote from client
         */
        public void getVote(String message) {
            isVoting = true;
            isAwake = true;
            voted = "";
            writeMessage(message);
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
        public void getConfirmation(String poleResult) {
            isConfirmation = true;
            isAwake = true;
            writeMessage(Game.getProperMessage(poleResult + " was voted do You confirm the execution?(Yes/No)"));
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
                game.updateDeadCharacters(client.getCharacter());
            }
        }

        /**
         * this method cure the player
         */
        public void cure() {
            isAlive = true;
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
    }
}
