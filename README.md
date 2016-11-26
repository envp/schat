# CNT5106C Fall'16 Project - SChat "A simple chat application"

## Introduction
SChat is a simple chat application that bundles a client and server in partial fulfillment of CNT5106C - Computer Networks. The full project specifications are in the `specifications/` directory.

SChat serves as an example application to demonstrate one of the many ways to build a reliant chat application.

## Usage
The project comes with a `Makefile` that contains various common actions (compilation, running a server instance on a predefined port, running a client instance etc.).

To complile the project and store all class files in the `build/` directory run:
```bash
$: make compile
```

To get help about how to run the application:
```bash
$: java -cp build/ schat.SChat help
Usage:

java SChat subcommand <options>

Supported subcommands and their [h]otkeys:
[h]elp     Print this usage dialog

[s]erver   <port> Starts a new server instance listening to the given port
           <port> The port at which the server should listen for incoming client requests

[c]lient   <username> <port> [<ip>] Starts a new client instance with the chosen username
           <username> Username choice of client, subject to change based on server side availability
           <port> Server's listening port to connect to
```

Running a server on port `12410` is achieved with one of:
1. `java -cp build/ schat.SChat server 12410`
2. `java -jar schat server 12410`
3. `java -jar schat s 12410`

Connecting a client to the same server (server ip not required in test /localhost mode.
1. `java -cp build/ schat.SChat client <username> 12410 <server_ip>`
2. `java -jar schat.jar client <username> 12410 <server_ip>`
3. `java -jar schat.jar c <username> 12410 <server_ip>`

Commands to the server are plaintext statements, like so:
1. By default, whatever you type into the console is taken as input for a server-wide text broadcast
2. `/text <message>` also broadcasts a message to everyone on the server
3. `/text @a @b @c <message>` sends a message to users with usernames `a`, `b`, `c`, list can be as long as required
4. `/text !a !b !c` sends a message to everyone but users with usernames `a`, `b`, `c`, list can be as long as required
5. `/file <relative-path>` sends a file stored at `<relative-path>` from where the executable is being run, this is broadcasted to all
6. `/file @a @b @c <relative-path>` sends the file to users with usernames `a`, `b`, `c`, list can be as long as required
7. `/file !a !b !c` sends the file to everyone but users with usernames `a`, `b`, `c`, list can be as long as required

## Documentation
See release.

## Contributing
Contributions to this project (other than from authors listed below) are disallowed until the end of Fall'16 semester.

## Authors
* Vaibhav Yenamandra <vyenaman@ufl.edu>
