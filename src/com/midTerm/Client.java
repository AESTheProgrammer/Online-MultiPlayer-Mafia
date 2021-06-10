package com.midTerm;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    // scanner is used to get input from the user
    // reader is used for reading information from input
    // out is used to write and send messages for server
    // savedChat is the saved chats which are considered this client
    // is the username of this client until end of the game
    // is a predicate for being logged in or out
    // is the character of this client
    private final Scanner scanner = new Scanner(System.in);
    private BufferedReader reader;
    private OutputStream out;
    private String savedChat = "";
    private String username = "";
    private boolean isLogin;
    public GameCharacter character;

    /**
     * this is a constructor
     */
    public Client() {

    }

    /**
     * this is the main method of the class which is used for running a client server
     * @param args is an arbitrarily array of strings
     */
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
            e.printStackTrace();
        }
    }

    /**
     * @throws InterruptedException exists because i stop the thread in middle somewhere and the error might occur
     */
    private void startMessageListener() throws InterruptedException {
        Thread listener = new Thread(this::readAndPrintMessage);
        listener.start();
    }

    /**
     * this method start the message sender
     * @throws InterruptedException is an error exception which might occur due to stopping the thread
     */
    private void startMessageSender() throws InterruptedException {
        Thread sender = new Thread(this::writeMessage);
        sender.start();
        sender.join();
    }

    /**
     * this method login the client into the server
     */
    private void login() {
        System.out.println("Connected to the Server");
        isLogin = true;
    }

    /**
     * this method logout the client from the server
     */
    private void logout() {
        System.out.println("Disconnected from the Server");
        isLogin = false;
    }

    /**
     * this method get an input from client and send it for server
     */
    private void writeMessage() {
        String inputString = "";
        try {
            out.write((username +"\n").getBytes());
            while (!inputString.trim().strip().equalsIgnoreCase("exit")) {
                inputString = scanner.nextLine();
                if (inputString.trim().strip().length() > 0)
                    out.write((inputString + "\n").getBytes());
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
        logout();
    }

    /**
     * this method read the messages from the input and print it on the window
     */
    private void readAndPrintMessage() {
        String line;
        try {
            reader.read();
            reader.read();
            reader.read();
            reader.read();
            while ((line = reader.readLine()) != null && isLogin) {
                // cls();
//                System.out.println(savedChat + line);
                if (line.contains("<character>") && character == null) {
                    character = GameCharacter.getCharacterByName(line.substring(line.indexOf(">") + 2));
                } else if (line.length() != 0) {
                    System.out.println(line);
                    savedChat += line + "\n";
                }
                if (!isLogin)
                    break;
            }
        } catch (IOException ignored) {}
    }

    /**
     * this method cleans the terminal
     */
    public static void cls() {
        try {
            new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * this method is a getter
     * @return username of the client
     */
    public String getUsername() {
        return username;
    }

    /**
     * this method is a setter
     * @param username is the username which will be set for the player as the username until end of the game
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * this method is a getter
     * @return character of the client in the game
     */
    public GameCharacter getCharacter() {
        return character;
    }

    /**
     * this method is a setter
     * @param randomRole get a random role and set it as role of the player in the game
     */
    public void setCharacter(GameCharacter randomRole) {
        this.character = randomRole;
    }
}
