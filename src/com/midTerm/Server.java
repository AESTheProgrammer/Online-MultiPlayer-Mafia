package com.midTerm;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.ArrayList;
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
        game = new Game();
    }

    public static void main(String[] args) {
        Server server = new Server(7660);
        try (ServerSocket welcomingSocket = new ServerSocket(server.port)) {
            System.out.println("Server Started");
            for (int i = 0; i < 10; i++) {
                Socket connectionSocket = welcomingSocket.accept();
                Client client = new Client();
                ClientHandler clientHandler = new ClientHandler(client, connectionSocket, server, server.game);

                server.clients.add(client);
                server.clientHandlers.add(clientHandler);
                server.executor.execute(clientHandler);
            }
            server.handoutCharacters();
            server.game.next();

        } catch (IOException e) {
            System.err.println(e);
            e.printStackTrace();
        }
    }

    private void handoutCharacters() {
        var cloneListOfHandlers = clientHandlers;
        for (int i = 0; i < clientHandlers.size(); i++) {
            var clientHandler = cloneListOfHandlers.get(generator.nextInt(clientHandlers.size()));
            clientHandler.sendObjectToClient(game.getCharacterInstance());
            clientHandler.sendObjectToClient(clientHandlers);
            cloneListOfHandlers.remove(clientHandler);
        }
    }

    private void updateAllClients() {
        for (var handler : clientHandlers) {
            handler.sendObjectToClient(clientHandlers);
        }
    }

    private Game getGame() {
        return game;
    }

    private void updateChatroom(String newMessage) {
        try {
            lock.lock();
            savedChat += newMessage + "\n";
        } finally {
            lock.unlock();
        }
        notifyAllClients(newMessage + "\n");
    }

    private void notifyAllClients(String newMessage) {
        for (var handler: clientHandlers)
            handler.writeMessage(newMessage);
    }

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

        private final Scanner scanner = new Scanner(System.in);
        private final Client client;
        private boolean isDay;
        private boolean isAwake;
        private final Socket connectionSocket;
        private final Server server;
        private InputStream  inputStream;
        private OutputStream outputStream;
        private ObjectOutputStream objectOutputStream;
        private BufferedReader reader;
        private boolean isConfirmation;
        private boolean isVoting;
        private byte warnings;
        private final Game game;
        private String savedChat = "";
        private static boolean isClientConnected;
        private String voted;

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
            this.isVoting = false;
            this.isConfirmation = false;
        }

        /**
         * this method runs the client handler
         */
        @Override
        public void run() {
            try {
                inputStream = connectionSocket.getInputStream();
                outputStream = connectionSocket.getOutputStream();
                objectOutputStream = new ObjectOutputStream(objectOutputStream);
                reader = new BufferedReader(new InputStreamReader(inputStream));
                reportClientConnection();
                getUsername();
                startMessageListener();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            } finally {
                try {
                    inputStream.close();
                    outputStream.close();
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
                if (!server.isValidUsername(inputUsername))
                    writeMessage(Game.getProperMessage(inputUsername + " is already in use by another player."));
                else {
                    server.updateChatroom(inputUsername.trim().strip() + " has been connected.");
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
        private void writeMessage(String properMessage) {
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
                    if (warnings >= 3) {
                        reportClientDisconnection();
                        server.updateChatroom(client.getUsername() + " has been disconnected.");
                        return;
                    } else if (!isVoting && !isConfirmation && isAwake) {
                        if (line.trim().strip().equalsIgnoreCase("exit")) {
                            server.updateChatroom(client.getUsername() + " has been disconnected.");
                            reportClientDisconnection();
                        } else if (isAwake) {
                            savedChat += client.getUsername() + ": " + line;
                            server.updateChatroom(line);
                        }
                    } else if (isConfirmation && !isVoting && isAwake) {
                        if (line.equalsIgnoreCase("yes")) {
                            isConfirmation = false;
                            server.updateChatroom(Game.getProperMessage("Mayor confirmed the execution."));
                        } else if (line.equalsIgnoreCase("NO")) {
                            isConfirmation = false;
                            server.updateChatroom(Game.getProperMessage("Mayor denied the execution."));
                        }
                    } else if (isVoting && !isConfirmation && isAwake) {
                        for (var client : game.getSortedClientList()) {
                            if (client.getUsername().equalsIgnoreCase(line.trim().strip())) {
                                voted = line.trim().strip();
                                if (!isDay) {
                                    server.updateChatroom(Game.getProperMessage(client.getUsername() + " voted " + line));
                                    break;
                                } else {
                                    server.notifyAwakeClients(Game.getProperMessage(client.getUsername() + " cured " + line));
                                    break;
                                }
                            }
                        }
                        writeMessage(Game.getProperMessage("Invalid Username, Please Try Again."));
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
            System.out.println("Client <" + client.getUsername() + "> Disconnected");
        }

        /**
         * this report clients' connection
         */
        private void reportClientConnection() {
            isClientConnected = true;
            System.out.println("Client <" + client.getUsername() + "> Connected");
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
    }
}
