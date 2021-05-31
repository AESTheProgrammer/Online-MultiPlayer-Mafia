package com.midTerm;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    // executor is a service for executing
    // count is the number of players
    // game is the system which holds some functionalities and status
    static ExecutorService executor = Executors.newCachedThreadPool();
    static int count;
    static Game game;
    public static void main(String[] args) {
        try (ServerSocket welcomingSocket = new ServerSocket(7660)) {
            System.out.println("Server Started.\nWaiting For Players to Join");
            while (count < 10) {
                try {
                    Socket connectionSocket = welcomingSocket.accept();
                    count++;
                    var thread = new Thread(new ClientHandler(connectionSocket));
                    executor.execute(thread);
                    System.out.printf("Client %d Accepted.%n%d more left.%n", count, 10 - count);
                } catch (IOException e1) {
                    e1.printStackTrace();
                    System.out.println("Accepting Client Failed.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Building Server Failed.");
        }

    }

    // this class handle the clients socket
    private static class ClientHandler implements Runnable {
        Socket connectionSocket;

        /**
         * this is a constructor
         * @param connectionSocket is socket used for making a connection between client and server
         */
        public ClientHandler(Socket connectionSocket) {
            this.connectionSocket = connectionSocket;
        }

        /**
         * this method run the clients socket
         */
        @Override
        public void run() {


        }

        /**
         * watch for clients responds and messages
         */
        public void eavesdrop() {

        }

        /**
         * send a respond or feedback to a client
         */
        public void respondOrSendFeedback() {

        }
    }

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
         * this methods checks if a game is finished
         */
        public void isGameFinished() {

        }

    }
}
