package sentencePal;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TextArea;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;

public class Util {

    static String getIPAddress(){
        String string = "";
        try {
            string = new String(
                    new URL("https://ip.cn/")
                    .openConnection()
                    .getInputStream()
                    .readAllBytes()
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(string.equals("")) return "network error";
        else return string.split("\"")[3];
    }

    static void filterString(StringBuilder stringBuilder){
        while(Character.isSpaceChar(stringBuilder.charAt(stringBuilder.length() - 1))
                || Character.isISOControl(stringBuilder.charAt(stringBuilder.length() - 1))
                || stringBuilder.charAt(stringBuilder.length() - 1) == '\r'
                || stringBuilder.charAt(stringBuilder.length() - 1) == '\n')
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
    }

    static class ScreenPrintStream extends PrintStream{
        private TextArea screen;

        public ScreenPrintStream(TextArea screen, OutputStream out) {
            super(out);
            this.screen = screen;
        }

        @Override
        public void print(String s) {
            screen.appendText(s);
        }

        @Override
        public void print(char c) {
            screen.appendText(String.valueOf(c));
        }

        @Override
        public void println() {
            screen.appendText("\r\n");
        }

        @Override
        public void println(String x) {
            screen.appendText(x);
            screen.appendText("\r\n");
        }
    }
}
