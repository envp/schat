package schat;

import schat.server.*;

/**
 *
 * @author Vaibhav Yenamandra (vyenman@ufl.edu)
 */
public class SChat {
    private static final String USAGE_TIP = 
    "Usage:\n" + 
    "\n" +
    "java SChat [s]erver|[c]lient <port> ";
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // Check and parse arguements
        if(args.length < 2) {
            System.out.println(USAGE_TIP);
        }
        else {
            switch(args[0]) {
                case "s":
                case "server":
                    // Server instantiation
                    Server s = new Server(Integer.parseInt(args[1]));
                    s.listen();
                    break;
                case "c":
                case "client":
                    // Client instantitation
                    break;
                default:
                    System.out.println(USAGE_TIP);
                    break;
            }
        }
    }
    
}
