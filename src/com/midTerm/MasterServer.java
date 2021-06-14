//package com.midTerm;
//
//import java.io.IOException;
//import java.net.ServerSocket;
//import java.net.Socket;
//import java.util.ArrayList;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//public class MasterServer {
//    private static final ArrayList<Server> servers = new ArrayList<>(1);
//    private static final ExecutorService executor = Executors.newCachedThreadPool();
//    private transient int port;
//
//    /**
//     * is the main method which runs the program
//     * @param args are arbitrarily strings which might be give to main
//     */
//    public static void main(String[] args) {
//        int count = 0;
//        Server server = new Server(7660 + count);
//        try (ServerSocket welcomingSocket = new ServerSocket(server.getPort())) {
//            System.out.println("Server Started");
//            while (count < 10) {
//                Socket connectionSocket = welcomingSocket.accept();
//                count++;
//                Client client = new Client();
//                Server.ClientHandler clientHandler = new Server.ClientHandler(client, connectionSocket, server, server.game);
//                servers.add(server);
//                executor.execute(server);
//            }
//        } catch (IOException | InterruptedException e) {
//            System.err.println(e);
//            e.printStackTrace();
//        }
//    }
//}
// it is commented so testing would be easy
