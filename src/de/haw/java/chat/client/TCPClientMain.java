package de.haw.java.chat.client;

public class TCPClientMain {
	
    public static void main(String[] args) {

        /* Standard Parameter */
    	String host = "141.22.27.107";
        int port = 56789;

        String name = "faptim";
        
		if (args.length == 3) {
            host = args[0];
            port = new Integer(args[1]);
            name  = args[2];
        }

        final ChatUI ui = new ChatSwingGUI(name);
        /* Test: Erzeuge Client und starte ihn. */
        final TCPClient tcpClient = new TCPClient(host, port, ui, name);
        ui.setClient(tcpClient);
        ui.startUi();
        tcpClient.startJob();
    }
}
