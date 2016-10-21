package schat;

import schat.server.*;

import java.io.IOException;

/**
 *
 * @author Vaibhav Yenamandra (vyenman@ufl.edu)
 */
public class SChat {
    private static final String USAGE_TIP =
    "Usage:\n" +
    "\n" +
    "java SChat subcommand <options>\n" +
    "\n" +
    "Supported subcommands and their [h]otkeys:\n" +
    "[s]erver <port>   Starts a new server instance listening to the given port\n" +
    "                  <port>: The port at which the server should listen for incoming client requests";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
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
