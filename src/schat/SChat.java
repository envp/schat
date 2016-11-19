package schat;

import java.io.IOException;
import schat.client.*;
import schat.server.*;

/**
 *
 * @author Vaibhav Yenamandra (vyenman@ufl.edu)
 */
public class SChat
{
    private static final String USAGE_TIP
        = "Usage:\n"
        + "\n"
        + "java SChat subcommand <options>\n"
        + "\n"
        + "Supported subcommands and their [h]otkeys:\n"
        + "[h]elp            Print this usage dialog\n"
        + "\n"
        + "[s]erver   <port> Starts a new server instance listening to the given port\n"
        + "           <port> The port at which the server should listen for incoming client requests\n"
        + "\n"
        + "[c]lient   <username> <port> [<ip>] Starts a new client instance with the chosen username\n"
        + "           <username> Username choice of client, subject to change based on server side availability\n"
        + "           <port> Server's listening port to connect to\n"
        + "\n";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException
    {
        // Check and parse arguements
        if (args.length < 2)
        {
            System.out.println(USAGE_TIP);
        }
        else
        {
            switch (args[0])
            {
                case "s":
                case "server":
                    // Server instantiation
                    // Add something for optional port too
                    Server.getInstance(Integer.parseInt(args[1])).listen();
                    break;
                case "c":
                case "client":
                    // Client instantitation
                    if (args.length < 3)
                    {
                        System.out.println(USAGE_TIP);
                        System.exit(1);
                    }
                    if (args.length == 3)
                    {
                        new Thread(
                            new Client(args[1], Integer.parseInt(args[2]))
                        ).start();
                    }
                    else if (args.length == 4)
                    {
                        new Thread(
                            new Client(
                                args[1], Integer.parseInt(args[2]), args[3]
                            )
                        ).start();
                    }
                    break;
                case "h":
                case "help":
                default:
                    System.out.println(USAGE_TIP);
                    break;
            }
        }
    }

}
