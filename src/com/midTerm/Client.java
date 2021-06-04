package com.midTerm;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    private Scanner scanner = new Scanner(System.in);
    private BufferedReader reader;
    private OutputStream out;
    private String savedChat = "";
    private String username = "";
    private boolean isLogin;

    public static void main(String[] args) {
        Client client = new Client();
        try (Socket connectionSocket = new Socket("127.0.0.1", 7660)){
            client.login();

            InputStream in = connectionSocket.getInputStream();
            client.out = connectionSocket.getOutputStream();
            client.reader = new BufferedReader(new InputStreamReader(in));

            client.startMessageListener();
            client.startMessageSender();

        } catch (IOException | InterruptedException e) {
            System.err.println(e);
            System.out.println("invalid IP Address or Port number");
        }
    }

    private void login() {
        System.out.println("Connected to the Server");
        System.out.print("Enter Your Username: ");
        username = scanner.nextLine();
        isLogin = true;
    }

    private void logout() {
        System.out.println("Disconnected from the Server");
        isLogin = false;
    }

    private void startMessageListener() throws InterruptedException {
        Thread listener = new Thread(this::readMessage);
        listener.start();
    }

    private void startMessageSender() throws InterruptedException {
        Thread sender = new Thread(this::writeMessage);
        sender.start();
        sender.join();
    }

    private void writeMessage() {
        String inputString = "";
        try {
            out.write((username +"\n").getBytes());
            while (!inputString.trim().strip().equalsIgnoreCase("exit")) {
                inputString = scanner.nextLine();
                if (inputString.trim().strip().length() > 0)
                    out.write((username + ": " + inputString + "\n").getBytes());
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
        logout();
    }

    private void readMessage() {
        String line = "";
        try {
            while ((line = reader.readLine()) != null) {
                cls();
                savedChat += line + "\n";
                System.out.println(savedChat);
                if (!isLogin)
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void cls() {
        try {
            new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
