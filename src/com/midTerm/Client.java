package com.midTerm;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    private final Scanner scanner = new Scanner(System.in);
    private BufferedReader reader;
    private OutputStream out;
//    private ObjectInputStream objectInputStream;
    private String savedChat = "";
    private String username = "";
    private boolean isLogin;
    public GameCharacter character;

    public Client() {

    }

    public static void main(String[] args) {
        Client client = new Client();
        try (Socket connectionSocket = new Socket("127.0.0.1", 7660)){
            client.login();
            InputStream in = connectionSocket.getInputStream();
            client.out = connectionSocket.getOutputStream();
            client.reader = new BufferedReader(new InputStreamReader(in));
//            client.objectInputStream = new ObjectInputStream(in);

//            client.startObjectListener();
            client.startMessageListener();
            client.startMessageSender();
        } catch (IOException | InterruptedException e) {
            System.err.println(e);
            e.printStackTrace();
        }
    }

//    private void startObjectListener() {
//        Thread thread = new Thread(() -> {
//            while (character == null)
//                readObject();
//            System.out.println(character);
//        });
//        thread.start();
//    }
//
//    private void readObject() {
//        try {
//            character = (GameCharacter) objectInputStream.readObject();
//        } catch (IOException | ClassNotFoundException ignored) {}
//    }

    private void startMessageListener() throws InterruptedException {
        Thread listener = new Thread(this::readMessage);
        listener.start();
    }

    private void startMessageSender() throws InterruptedException {
        Thread sender = new Thread(this::writeMessage);
        sender.start();
        sender.join();
    }

    private void login() {
        System.out.println("Connected to the Server");
        isLogin = true;
    }

    private void logout() {
        System.out.println("Disconnected from the Server");
        isLogin = false;
    }

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

    private void readMessage() {
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

    public GameCharacter getCharacter() {
        return character;
    }

    public void setCharacter(GameCharacter randomRole) {
        this.character = randomRole;
    }
}
