package de.haw.java.chat.server;

public class TCPServerMain {
	
    public static void main(String[] args) {
        TCPServer myServer;
        int serverPort = 56666;
        int maxClients = 5;

        if (args.length == 2) {
            serverPort = new Integer(args[0]);
            maxClients = new Integer(args[1]);
        }
        myServer = new TCPServer(serverPort, maxClients);
        myServer.run();
    }

}
