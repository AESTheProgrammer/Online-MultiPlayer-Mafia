//package com.midTerm;
//
//import java.io.IOException;
//import java.net.ServerSocket;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//public class MasterServer {
//    private static final ExecutorService executor = Executors.newCachedThreadPool();
//    private transient int port;
//
//    public MasterServer(int port) {
//        this.port = port;
//    }
//
//    /**
//     * is the main method which runs the program
//     */
//    public static void main(String[] args) {
//        MasterServer server = new MasterServer(8880);
//        try (ServerSocket welcomingSocket = new ServerSocket(server.port)) {
//            System.out.println("Server Started");
//            int count = 0;
//            while (count < 10) {
//                welcomingSocket.accept();
//                count++;
//                executor.execute(new Server(7760 + count));
//            }
//        } catch (IOException e) {
//            System.err.println(e);
//            e.printStackTrace();
//        }
//    }
//}
// in kelas baraye test shoadan comment shode
