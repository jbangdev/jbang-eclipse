import java.util.Map;

import picocli.CommandLine;
import picocli.CommandLine.Option;

public class foo implements Runnable {
    @Option(names = "-fix", split = "\\|")
    Map<Integer, String> message;

    @Option(names = "-other")
    String other;

    @Override
    public void run() {
        if(System.getProperty("value")!=null) {
            System.out.print("value::" + System.getProperty("value") + " ");
        }
        System.out.println("other: [" + (other==null?"":other) + "] " + "fix : " + message);
    }

    public static void main(String[] args) {
        new CommandLine(new foo()).execute(args);
    }
}