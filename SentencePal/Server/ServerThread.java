package Server;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

public class ServerThread {

    @FunctionalInterface
    private interface Action {
        void action(ServerThread s, String content);
    }

    private static Map<String, Action> actionMap = new HashMap<>();

    private Socket clientSocket;
    private BufferedInputStream in;
    private PrintWriter out;
    private String username;

    private String req;
    private StringBuilder resp;
    private boolean isAlive = true;
    private byte[] buf;

    static {
        actionMap.put("sentence", (s, content) -> {
            Stream.of(content.split(","))
//                    .mapToInt(Integer::parseInt)
//                    .filter(i->i>0&&i<Server.sentences.size())
                    .forEach(lineNo -> {
                        s.resp.append(lineNo).append(':');
                        if (Server.sentences.get(lineNo) == null)
                            s.resp.append("(null)\r\n");
                        else
                            s.resp.append(Server.sentences.get(lineNo));
                    });
        });

        actionMap.put("name", (s, username) -> {
            for (var t : Server.clientList) {
                if (t.username.equals(username)) {
                    s.resp.append("this name already exits.\r\n");
                    return;
                }
            }
            if(!s.username.equals("")) {
                s.notifyClientListChanged(s.username, username);
                s.resp.append("change:").append(username);
            }
            else s.resp.append("init:").append(username);
            s.username = username;
        });

        actionMap.put("exit", (s, content) -> s.isAlive = false);

        actionMap.put("talk", ServerThread::serveTalk);

        actionMap.put("upload", (s, content) -> {
            String no = String.valueOf(Server.sentences.size() + 1);
            Server.sentences.put(no, content + "\r\n");
            try {
                Server.uploadSentence(content + "\r\n");
            } catch (IOException e) {
                e.printStackTrace();
                Server.logger.severe("save sentence failed");
                s.resp.append("upload failed");
            }
            s.service("sentence", no);
        });

        actionMap.put("today", (s, content) -> {
            LocalDate date = LocalDate.now();
            String lineNo = String.valueOf(date.getDayOfMonth() + 40);
            s.resp.append("Today's motto:\n").append(Server.sentences.get(lineNo));
        });

        actionMap.put("share", (s, content) -> {
            int colon = content.indexOf(':');
            String name = content.substring(0, colon);
            content = content.substring(colon + 1);
            for (var t : Server.clientList) {
                if (t.username.equals(name)) {
                    if (t.username.equals(s.username)) {
                        s.resp.append("do not tease Server\n");
                        return;
                    }
                    synchronized (t){
                        t.resp.append("share:").append(s.username).append(" share for you:\n");
                        for (var lineNo : content.split(",")) {
                            t.resp.append(lineNo)
                                    .append(':')
                                    .append(Server.sentences.get(lineNo));
                        }
                        t.out.print(t.resp.toString());
                        t.out.flush();
                        t.resp.setLength(0);
                    }
                    s.resp.append("success");
                    return;
                }
            }
            s.resp.append("failed, he/she is offline\n");
        });
    }

    public ServerThread(Socket clientSocket) {
        this.clientSocket = clientSocket;
        resp = new StringBuilder();
        buf = new byte[10240];
        username = "";
        try {
            in = new BufferedInputStream(clientSocket.getInputStream());
            out = new PrintWriter(clientSocket.getOutputStream());
        } catch (IOException e) {
            close();
        }
        new Thread(() -> {
            try {
                doRequest();
                Server.logger.info(username + " is online now");
                initClientList();
                notifyClientList(username, true);
                while (isAlive) {
                    doRequest();
                }
                close();
                System.out.println(username + " exit");
            } catch (IOException e) {
                close();
            }
        }).start();
    }

    private void doRequest() throws IOException {
        int len = in.read(buf);
        if (len == -1) {
            close();
            return;
        }
        req = new String(buf, 0, len);
        Server.logger.info(username + " req:\n\t" + req);
        parse();
    }

    private void parse() {
        int colon = req.indexOf(':');
        String action = req.substring(0, colon);
        String content = req.substring(colon + 1);
        service(action, content);
    }

    private synchronized void service(String action, String content) {
        resp.append(action).append(':');
        actionMap.getOrDefault(action, (s, c) -> resp.append("unknown action"))
                .action(this, content);
        Server.logger.info(username + " resp:\n\t" + resp.toString());
        resp.append((char )1);
        out.print(resp.toString());
        out.flush();
        resp.setLength(0);
    }

    private void serveTalk(String content) {
        int colon = content.indexOf(':');
        String name = content.substring(0, colon);
        content = content.substring(colon + 1);
        for (var t : Server.clientList) {
            if (t.username.equals(name)) {
                if (t.username.equals(username)) {
                    resp.append("do not tease Server\n");
                    return;
                }
                t.resp.append("msg:").append(username).append(" say to you:").append(content).append("\r\n");
                t.out.print(t.resp.toString());
                t.out.flush();
                t.resp.setLength(0);
                resp.append("success");
                return;
            }
        }
        resp.append("failed, he/she is offline\n");
    }

    private void notifyClientListChanged(String oldName, String newName){
        for(ServerThread t : Server.clientList){
            if(t == this) continue;
            synchronized (t){
                t.resp.append("changeClientName:")
                        .append(oldName)
                        .append((char)0)
                        .append(newName);
                t.out.print(t.resp.toString());
                t.out.flush();
                t.resp.setLength(0);
            }
        }
        Server.logger.info(username + " notify other clients name changed");
    }

    private void notifyClientList(String name, boolean toAdd) {
        for(ServerThread t : Server.clientList){
            if(t == this) continue;
            synchronized (t){
                t.resp.append( toAdd ? "addClient:" : "removeClient:").append(name);
                t.out.print(t.resp.toString());
                t.out.flush();
                t.resp.setLength(0);
            }
        }
        Server.logger.info(username + " notify other clients");
    }

    private void initClientList(){
        resp.append("initClientList:");
        for (var client : Server.clientList) {
            resp.append(client.username)
                    .append((char)0);
        }
        if(!Server.clientList.isEmpty()) {
            resp.deleteCharAt(resp.length() - 1);
        }
        out.print(resp.toString());
        out.flush();
        resp.setLength(0);
        Server.logger.info(username + "'clientList initialized");
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
        if(!username.equals("")) {
            notifyClientList(username, false);
            System.out.println(username + " is offline");
        }
    }
}
