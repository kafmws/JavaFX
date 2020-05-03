package Server;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class ServerThread {

    @FunctionalInterface
    private interface Action {
        void action(String content);
    }

    private static Map<String, Action> actionMap = new HashMap<>();

    private Socket clientSocket;
    private BufferedInputStream in;
    private PrintWriter out;
    private String username;

    String req;
    StringBuilder resp;
    private boolean isAlive = true;
    private byte[] buf;

    {
        actionMap.put("sentence", (String content) -> {
            Stream.of(content.split(","))
//                    .mapToInt(Integer::parseInt)
//                    .filter(i->i>0&&i<Server.sentences.size())
                    .forEach(lineNo -> {
                        resp.append(lineNo).append(':');
                        if(Server.sentences.get(lineNo) == null)
                            resp.append("(null)\r\n");
                        else
                            resp.append(Server.sentences.get(lineNo));
                    });
        });

        actionMap.put("name", username -> {
            for (var t : Server.clientList) {
                if (t.username != null && t.username.equals(username)) {
                    resp.append("this name already exits.");
                    return;
                }
            }
            this.username = new String(username);
            resp.append("success");
        });

        actionMap.put("exit", content -> isAlive = false);

        actionMap.put("talk", content -> {
            int colon = content.indexOf(':');
            String name = content.substring(0, colon);
            content = content.substring(colon + 1);
            for (var t : Server.clientList) {
                if (t.username.equals(name)) {
                    if (t.username.equals(username)) {
                        resp.append("do not tease Server\n");
                        return;
                    }
                    t.resp.append("msg:").append(username).append(" say:").append(content).append("\r\n");
                    t.out.print(t.resp.toString());
                    t.out.flush();
                    t.resp.setLength(0);
                    resp.append("success");
                    return;
                }
            }
            resp.append("failed, he/she is offline\n");
        });

        actionMap.put("upload", content -> {
            String no = String.valueOf(Server.sentences.size() + 1);
            Server.sentences.put(no, content);
            try {
                Server.uploadSentence(content);
            } catch (IOException e) {
                e.printStackTrace();
                Server.logger.severe("save sentence failed");
                resp.append("upload failed");
            }
            service("sentence", no);
        });

        actionMap.put("today", content -> {
            LocalDate date = LocalDate.now();
            String lineNo = String.valueOf(date.getDayOfMonth() + 40);
            resp.append("Today's motto:\n").append(Server.sentences.get(lineNo));
        });

        actionMap.put("share", content -> {
            int colon = content.indexOf(':');
            String name = content.substring(0, colon);
            content = content.substring(colon + 1);
            for (var t : Server.clientList) {
                if (t.username.equals(name)) {
                    if (t.username.equals(username)) {
                        resp.append("do not tease Server\n");
                        return;
                    }
                    t.resp.append("share:").append(username).append(" share for you:\n");
                    for (var lineNo : content.split(",")) {
                        t.resp.append(lineNo)
                                .append(':')
                                .append(Server.sentences.get(lineNo));
                    }
                    t.out.print(t.resp.toString());
                    t.out.flush();
                    t.resp.setLength(0);
                    resp.append("success");
                    return;
                }
            }
            resp.append("failed, he/she is offline\n");
        });
    }

    public ServerThread(Socket clientSocket) {
        this.clientSocket = clientSocket;
        resp = new StringBuilder();
        buf = new byte[10240];
        try {
            in = new BufferedInputStream(clientSocket.getInputStream());
            out = new PrintWriter(clientSocket.getOutputStream());
            doRequest();
            Server.logger.info(username + " is online now");
            new Thread(() -> {
                try {
                    while (isAlive) {
                        doRequest();
                    }
                    close();
                    System.out.println(username + " exit");
                } catch (IOException e) {
                    close();
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void doRequest() throws IOException {
        int len = in.read(buf);
        if (len == -1) {
            close();
            return;
        }
        req = new String(buf, 0, len);
        Server.logger.info("req:\n\t" + req);
        parse();
    }

    private void parse() {
        int colon = req.indexOf(':');
        String action = req.substring(0, colon);
        String content = req.substring(colon + 1);
        service(action, content);
    }

    private void service(String action, String content) {
        resp.append(action).append(':');
        actionMap.getOrDefault(action, c -> resp.append("unknown action"))
                .action(content);
        Server.logger.info("resp:\n\t" + resp.toString());
        if (!isAlive) return;
        out.print(resp.toString());
        out.flush();
        resp.setLength(0);
    }

    private void close() {
        if (clientSocket.isClosed()) return;
        try {
            isAlive = false;
            in.close();
            out.close();
            clientSocket.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        Server.clientList.remove(this);
        System.out.println(username + " is offline");
    }
}
