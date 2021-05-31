package com.midTerm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;

public class Client {
   static Scanner scanner = new Scanner(System.in);
   public static void main(String[] args) {
       try (Socket client = new Socket("127.0.0.1", 7660)) {
           System.out.println("Connected to the server.");
           OutputStream out = client.getOutputStream();
           InputStream in = client.getInputStream();
           getInputMessageFromUser(out);
           getMessageOrFeedbackFromServer(in);
       } catch (IOException e) {
           e.printStackTrace();
           System.out.println("Did not Connected to the server");
       }
   }

    /**
     * get message from server
     * @param in is the input stream
     * @throws IOException is an exception which might be thrown
     */
    private static void getMessageOrFeedbackFromServer(InputStream in) throws IOException {
        System.out.println("Receiving Message From Server ...");
        byte[] buffer = new byte[4096];
        System.out.println(in.read(buffer));
        System.out.println("SERVER: ");
        System.out.println(new String(buffer));
    }

    /**
     * get input from user and send it to user
     * @param out is the out put stream
     */
    public static void getInputMessageFromUser(OutputStream out) {
        System.out.println("now you can write ...");
        var message = "";
        while (!message.trim().strip().equals("over")) {
            message = scanner.nextLine();
            try {
                out.write(message.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
