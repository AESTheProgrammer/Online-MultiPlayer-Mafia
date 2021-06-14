package com.midTerm;

import java.io.*;
import java.util.ArrayList;

public class GameSaver {
    private static ObjectWriter writer;
    private static ObjectReader reader;

    static {
        try {
            writer = new ObjectWriter("SAVED.txt");
            reader = new ObjectReader("SAVED.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveGame(Serializable serializable) throws IOException {
        writer.writeObject(serializable);
    }

    public static Server loadGame(String ID) {
        ArrayList<Server> servers = new ArrayList<>();
        try {
            while (true) {
                servers.add((Server) reader.readObject());
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        for (Server server : servers) {
            if (server.checkEquality(ID))
                return server;
        }
        return null;
    }

    private static class ObjectReader {
        private ObjectInputStream reader;

        public ObjectReader(String fileAddress) throws IOException {
            reader = new ObjectInputStream(new FileInputStream(fileAddress));
        }

        public Object readObject() throws IOException, ClassNotFoundException {
            return reader.readObject();
        }

        public void close() throws IOException {
            reader.close();
        }
    }

    private static class ObjectWriter {
        private ObjectOutputStream writer;

        public ObjectWriter(String fileAddress) throws IOException {
            writer = new ObjectOutputStream(new FileOutputStream(fileAddress));
        }

        public void writeObject(Serializable serializable) throws IOException{
            writer.writeObject(serializable);
        }

        public void close() throws IOException {
            writer.close();
        }
    }
}
