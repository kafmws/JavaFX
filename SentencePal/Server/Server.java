package Server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class Server {
    private static final int port = 12345;
    private static final String sentenceFile = "sentence.dat";
    static final Logger logger = Logger.getLogger("Sever Log");

    static List<ServerThread> clientList = new ArrayList<>();


    //particular
    static Map<String, String> sentences = new HashMap<>(900);

    public static void main(String[] args) {
        try {
            loadSentence();
            ServerSocket serverSocket = new ServerSocket(port);
            while (true) {
                Socket client = serverSocket.accept();
                clientList.add(new ServerThread(client));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadSentence() {
        try {
            LineNumberReader lineNumberReader =
                    new LineNumberReader(new FileReader(sentenceFile));
            String line = null;
            while ((line = lineNumberReader.readLine()) != null) {
                sentences.put(String.valueOf(lineNumberReader.getLineNumber()), line+"\r\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    synchronized static void uploadSentence(String sentence) throws IOException {
        RandomAccessFile file = new RandomAccessFile(new File(sentenceFile), "rw");
        long fileLength = file.length();
        file.seek(fileLength);
        file.writeUTF(sentence);
        file.writeUTF("\r\n");
        file.close();
    }
}