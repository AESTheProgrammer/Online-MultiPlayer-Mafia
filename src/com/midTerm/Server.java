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

    //----------------------------------------
    // this class handles the client
    protected static class ClientHandler implements Runnable{

        private final Scanner scanner = new Scanner(System.in);
        private final Client client;
        private boolean isAwake;
        private final Socket connectionSocket;
        private final Server server;
        private InputStream  inputStream;
        private OutputStream outputStream;
        private ObjectOutputStream objectOutputStream;
        private BufferedReader reader;
        private final Game game;
        private String savedChat = "";
        private static boolean isClientConnected;

        public ClientHandler(Client client,
                             Socket connectionSocket,
                             Server server,
                             Game game) {
            this.server = server;
            this.game = game;
            this.client = client;
            this.connectionSocket = connectionSocket;
        }

        @Override
        public void run() {
            try {
                inputStream = connectionSocket.getInputStream();
                outputStream = connectionSocket.getOutputStream();
                objectOutputStream = new ObjectOutputStream(objectOutputStream);
                reader = new BufferedReader(new InputStreamReader(inputStream));
                reportClientConnection();
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

        private void startMessageListener() throws InterruptedException {
            Thread listener = new Thread(this::readMessage);
            listener.start();
            listener.join();
        }

        private void writeMessage(String properMessage) {
            try {
                outputStream.write(properMessage.getBytes());
            } catch(IOException e) {
                e.printStackTrace();
            }
        }

        private void readMessage() {
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    if (client.getUsername().length() == 0) {
                        server.updateChatroom(line.trim().strip() + " has been connected.");
                        client.setUsername(line.trim().strip());
                    } else if (line.substring(line.indexOf(":") + 1).trim().strip().equalsIgnoreCase("exit") && client.getUsername().length() !=0){
                        server.updateChatroom(line.substring(0, line.indexOf(":")) + " has been disconnected.");
                        reportClientDisconnection();
                    } else if (isAwake) {
                        savedChat += line;
                        server.updateChatroom(line);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void reportClientDisconnection() {
            isClientConnected = false;
            System.out.println("Client Disconnected");
        }

        private void reportClientConnection() {
            isClientConnected = true;
            System.out.println("Client Connected");
        }

        public Client getClient() {
            return client;
        }

        public void sendObjectToClient(Object object) {
            try {
                objectOutputStream.writeObject(object);
            } catch (IOException e) {
                System.err.println(e);
                e.printStackTrace();
            }
        }

        public void awake() {
            isAwake = true;
            writeMessage(Game.getProperMessage("It's DayTime And EveryOne Are Awake."));
        }

        public void asleep() {
            isAwake = false;
            writeMessage(Game.getProperMessage("It's Night and EveryOne Are Asleep."));
        }
    }
}
