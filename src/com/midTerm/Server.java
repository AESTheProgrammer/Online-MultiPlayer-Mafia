package com.midTerm;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Server {
    private final ArrayList<Client> clients = new ArrayList<>(1);
    private final ArrayList<ClientHandler> clientHandlers = new ArrayList<>(1);
    private final Lock lock = new ReentrantLock();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private int port;
    private String savedChat = "";

    public Server(int port) {
        this.port = port;
    }

    public static void main(String[] args) {
        Server server = new Server(7660);
        try (ServerSocket welcomingSocket = new ServerSocket(server.port)) {
            System.out.println("Server Started");
            for (int i = 0; i < 10; i++) {
                Socket connectionSocket = welcomingSocket.accept();
                Client client = new Client();
                ClientHandler clientHandler = new ClientHandler(client, connectionSocket, server);
                server.clients.add(client);
                server.clientHandlers.add(clientHandler);
                server.executor.execute(clientHandler);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

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
    private static class ClientHandler implements Runnable{
        private final Scanner scanner = new Scanner(System.in);
        private final Client client;
        private final Socket connectionSocket;
        private final Server server;
        private InputStream  inputStream;
        private OutputStream outputStream;
        private BufferedReader reader;
        private String savedChat = "";
        private static boolean isClientConnected;

        public ClientHandler(Client client,
                             Socket connectionSocket,
                             Server server) {
            this.server = server;
            this.client = client;
            this.connectionSocket = connectionSocket;
        }

        @Override
        public void run() {
            try {
                inputStream = connectionSocket.getInputStream();
                outputStream = connectionSocket.getOutputStream();
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
                    } else {
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
    }

    //----------------------------------------
    // this class holds game related status and method
    private static class Game {

        // players is the list of all players which game started with
        // deadPlayers is the list of dead players
        // deadPlayers is the list of dead players
        // isDay check if its night or day
        // result is the final result of the game
        private static ArrayList<Player> players;
        private static ArrayList<Player> deadPlayers;
        private static ArrayList<Player> alivePlayers;
        private static boolean isDay;
        private final String result = null;

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
         */
        public void isGameFinished() {

        }

    }
}
