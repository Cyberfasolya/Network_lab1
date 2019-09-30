import CopyListener.CopyListener;

public class Main {
    public static void main(String[] args){
        if(args.length != 1){
            throw new IllegalArgumentException("Wrong number of arguments");
        }
        CopyListener copyListener = new CopyListener();
        copyListener.run(args[0]);
    }
}
