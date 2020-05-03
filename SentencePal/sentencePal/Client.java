package sentencePal;

import javafx.collections.ObservableList;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.stream.Stream;

public class Client implements Runnable {
    private static final String[] actions =
            {"", "sentence", "share", "upload", "talk", "today", "name", "exit"};
    private static final String filename = "client.dat";

    private BufferedInputStream in;
    private PrintWriter out;
    private Socket socket;

    private StringBuilder req;
    private String resp;
    private byte[] buf;

    private Map<String, String> sentences = new HashMap<>();
    private PrintStream screenStream = System.out;
    private PrintWriter writer;

    private boolean isAlive = true;

    @FunctionalInterface
    private interface Action {
        void action(String content);
    }

    private static Map<String, Action> actionMap = new HashMap<>();

    public Client() {
        loadSentence();
        buf = new byte[10240];
        req = new StringBuilder();
        File localFile = new File(filename);
        try {
            if(!localFile.exists()) localFile.createNewFile();
            writer = new PrintWriter(new FileWriter(localFile),true);
            sentences.forEach(this::saveSentences);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            isAlive = false;
        }
    }

    @Override
    public void run() {

        {
            actionMap.put("exit", content -> isAlive = false);

            actionMap.put("sentence", content -> {
                screenStream.print(content);
                Stream.of(content.split("\r\n"))
                        .map(s -> s.split(":"))
                        .forEach(s -> {
                            if (!s[1].equals("(null)")) {
                                if (sentences.putIfAbsent(s[0], s[1]) == null) {
                                    saveSentences(s[0], s[1]);
                                }
                            }
                        });
            });

            Action check = content -> {
                if (!content.equals("success")) screenStream.println(content);
            };

            actionMap.put("share", check);

            actionMap.put("upload", check);

            actionMap.put("name", check);

            actionMap.put("talk", check);

            actionMap.put("msg", screenStream::print);

            actionMap.put("today", screenStream::print);
        }

        new Thread(() -> {
            try {
                while (isAlive) {
                    int len = in.read(buf);
                    if (len == -1) {
                        close();
                        break;
                    }
                    resp = new String(buf, 0, len);
//                    System.out.println("resp:\n\t" + resp);
                    parse();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                close();
            }
        }).start();
    }

    private void parse() {
        int colon = resp.indexOf(':');
        String action = resp.substring(0, colon);
        String content = resp.substring(colon + 1);
        service(action, content);
    }

    private void service(String action, String content) {
        actionMap.getOrDefault(action, c -> screenStream.println("unrecognized response"))
                .action(content);
    }

    private void close() {
        if (socket.isClosed()) return;
        try {
            isAlive = false;
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        screenStream.println("connection closed");
    }

    private void saveSentences(String lineNo, String line) {
        writer.print(lineNo);
        writer.print(':');
        writer.println(line);
        writer.flush();
    }

    private void loadSentence() {
        try {
            BufferedReader br =
                    new LineNumberReader(new FileReader(filename));
            String line = null;
            while ((line = br.readLine()) != null) {
                var t = line.split(":");
                sentences.put(t[0], t[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setIn(BufferedInputStream in) {
        this.in = in;
    }

    public void setOut(PrintWriter out) {
        this.out = out;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public void setResp(String resp) {
        this.resp = resp;
    }

    public void setAlive(Boolean alive) {
        isAlive = alive;
    }

    public StringBuilder getReq() {
        return req;
    }

    public boolean getAlive() {
        return isAlive;
    }

    public void setScreenStream(PrintStream screenStream) {
        this.screenStream = screenStream;
    }
}