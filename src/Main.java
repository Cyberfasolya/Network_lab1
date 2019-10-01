import CopyListener.CopyListener;

public class Main {
    public static void main(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Wrong number of arguments");
        }
        CopyListener copyListener;
        try {
            copyListener = new CopyListener(args[0]);
        } catch (IllegalStateException | IllegalArgumentException e) {
            e.printStackTrace();
            return;
        }

        try {
            copyListener.run();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }
}
