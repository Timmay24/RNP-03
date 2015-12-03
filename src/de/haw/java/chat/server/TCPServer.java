package de.haw.java.chat.server;
/*
* TCPServer.java
*
* <Bearbeitungsinfo!> TODO
*
* Version 3.1
* Autor: M. Huebner HAW Hamburg (nach Kurose/Ross)
* Zweck: TCP-Server Beispielcode:
*        Bei Dienstanfrage einen Arbeitsthread erzeugen, der eine Anfrage bearbeitet:
*        einen String empfangen, in Grossbuchstaben konvertieren und zuruecksenden
*        Maximale Anzahl Worker-Threads begrenzt durch Semaphore
*
*/

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Semaphore;


/**
 * @author abq364
 *
 */
public class TCPServer implements Runnable {
/* TCP-Server, der Verbindungsanfragen entgegennimmt */
	
    public static final String VERSION = "Chat-0.0.1";

    /* Konstante für Umbrüche */
    public static final String CRLF  = "\r\n";

    /* Semaphore begrenzt die Anzahl parallel laufender Worker-Threads  */
    public Semaphore clientThreadsSem;

    /* Portnummer */
    public final int serverPort;

    /* Anzeige, ob der Server-Dienst weiterhin benoetigt wird */
    public boolean serviceRequested = true;

    /* Löst Nutzernamen in ihre zugehörigen Threads auf */
    public final Map<String, ClientThread> nicknamesToClients;

    /* Liste aller aktiven Client-Threads */
    public final List<ClientThread> clients;
    
    private final Thread listener;

    /**
     * Konstruktor mit Parametern: Server-Port, Maximale Anzahl paralleler Worker-Threads
     */
    public TCPServer(int serverPort, int maxThreads) {
        this.serverPort = serverPort;
        this.clientThreadsSem = new Semaphore(maxThreads);
        clients = new ArrayList<>();
        nicknamesToClients = new HashMap<>();
        this.listener = new Thread(this);
    }

    /**
     * Startet den Chat-Server
     */
    @Override
	public void run() {
        ServerSocket welcomeSocket;   // TCP-Server-Socketklasse
        Socket connectionSocket;      // TCP-Standard-Socketklasse

        int nextThreadNumber = 0;

        try {
            /* Server-Socket erzeugen */
            welcomeSocket = new ServerSocket(serverPort);

            while (serviceRequested) {
                clientThreadsSem.acquire();  // Blockieren, wenn max. Anzahl Client-Threads erreicht

                System.out.println("Chat server is waiting for connection on port " + serverPort);
                /*
                * Blockiert auf Verbindungsanfrage warten --> nach Verbindungsaufbau
                * Standard-Socket erzeugen und an connectionSocket zuweisen
                */
                connectionSocket = welcomeSocket.accept();
                
                System.out.println("Incoming connection from " + connectionSocket.getInetAddress() + " bound to port " + connectionSocket.getPort());

                /* Neuen Client-Thread erzeugen und die Nummer, den Socket sowie das Serverobjekt uebergeben */
                final ClientThread newClientThread = new ClientThread(++nextThreadNumber, connectionSocket, this);
                clients.add(newClientThread);
                newClientThread.start();

               
                /* AUTHORISIERUNG (BZW. WAHL DES NUTZERNAMENS) ERFOLGT GESONDERT NACH EINRICHTUNG VON SOCKET UND THREAD  */
            }
        } catch (final Exception e) {
            System.err.println(e.toString());
        }
    }
    

    /** OPERATIONS **/
    public void shutDown() {
    	for (final ClientThread clientThread : clients) {
			clientThread.shutDown();
		}
    	this.listener.stop();
    }

    /**
     * Sendet die eine Servernachricht an alle Clients
     * @param message Zu versendende Nachricht
     * @throws IOException
     */
    public void notifyClients(String message) throws IOException {
        for (final ClientThread recipient : clients) {
            if (recipient.isAuthorized()) {
                recipient.writeServerMessageToClient(message);
            }
        }
    }

    /**
     * Sendet eine Client-Nachricht an alle anderen Clients auf dem Server
     * @param message Zu sendende Nachricht
     * @param sender Absender
     * @throws IOException
     */
    public void notifyClients(String message, ClientThread sender) throws IOException {
        for (final ClientThread recipient : clients) {
            if (!recipient.equals(sender) && recipient.isAuthorized()) {
                recipient.writeToClient(sender.getNickname() + ": " + message);
            }
        }
    }

    /**
     * Sendet eine private Nachricht eines Clients an einen bestimmten Client (Flüstern)
     * @param message Zu sendene Nachricht
     * @param sender Absender
     * @param recipientNickname Geheimer Empfänger
     * @throws IOException
     */
    public synchronized void whisperToClient(String message, ClientThread sender, String recipientNickname) throws IOException {
        if (userExists(recipientNickname)) {
            nicknamesToClients.get(recipientNickname).writeToClient(sender.getNickname() + " <whisper>: " + message);
            log(sender.getNickname() + " whispers to " + recipientNickname + ": " + message);
        }
    }
    
    /**
     * Stupst einen Client an
     * @param sender Der stupsende Client
     * @param recipientNickname Der anzustupsende Client
     * @throws IOException
     */
    public synchronized void pokeClient(ClientThread sender, String recipientNickname) throws IOException {
    	if (userExists(recipientNickname)) {
            nicknamesToClients.get(recipientNickname).writeToClient(sender.getNickname() + " poked you!");
            log(sender.getNickname() + " pokes the s$%# out of " + recipientNickname);
        }
    }

    /**
     * Registriert einen neu angemeldeten Client am Server zur Nutzung des Chats
     * @param inNickname   Gewünschter Nutzername
     * @param clientThread Threadreferenz des anfragenden Clients
     * @return true, falls Nutzer erfolgreich beigetreten ist.
     */
    public synchronized boolean registerUser(String inNickname, ClientThread clientThread) throws IOException {
        if (!userExists(inNickname)) {
            nicknamesToClients.put(inNickname, clientThread);
            clientThread.setNickname(inNickname);
            onClientLogin(clientThread);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Entfernt und deregistriert einen Nutzer aus dem Chat
     * @param client Thread des zu entfernenden Nutzers
     * @return true, falls Nutzer erfolgreich entfernt werden konnte
     */
    public synchronized boolean removeUser(ClientThread client) {
        if (client != null && !client.getNickname().isEmpty()) {
	    	clients.remove(client);
	        nicknamesToClients.remove(client.getNickname());
	        try {
	            onClientLogout(client);
	        } catch (final IOException e) {
	            e.printStackTrace();
	        }
	        clientThreadsSem.release();
	        return true;
        } else {
        	return false;
        }
    }

    public synchronized boolean removeUser(String nickname) {
        if (userExists(nickname)) {
            return removeUser(nicknamesToClients.get(nickname));
        } else {
            return false;
        }
    }

    /**
     * Loggt eine Nachricht in der Konsole
     * @param message Zu loggene Nachricht
     */
    private void log(String message) {
        System.out.println(message);
    }

    /** EVENTS **/

    /**
     * Benachrichtigt alle Clients über den Beitritt eines Clients
     * @param client Client, der beitritt
     * @throws IOException
     */
    public void onClientLogin(ClientThread client) throws IOException {
        notifyClients(client.getNickname() + " enters the chat.");
    }

    /**
     * Benachrichtigt alle Clients über das Verlassen eines Clients
     * @param client Client, der den Server verlässt
     * @throws IOException
     */
    public void onClientLogout(ClientThread client) throws IOException {
        notifyClients(client.getNickname() + " leaves the chat.");
    }

    /**
     * Behandelt eine Umbenennungsanfrage eines Clients und führt die Umbenennung durch
     * @param desiredNickname Gewünschter Nutzername
     * @param clientThread    Threadreferenz des Nutzers, der sich umbenennen möchte
     * @return true, falls Umbenennung erfolgreich
     */
    public synchronized boolean onClientRename(String desiredNickname, ClientThread clientThread) throws IOException {
        if (!userExists(desiredNickname)) {
            nicknamesToClients.put(desiredNickname, clientThread); // neuen Namen anmelden
            final String formerNickname = clientThread.getNickname();
            nicknamesToClients.remove(formerNickname); // alten Namen entfernen
            clientThread.setNickname(desiredNickname);
            notifyClients(formerNickname + " renamed to " + desiredNickname);
            return true;
        }
        return false;
    }

    /** PREDICATES **/

    /**
     * Prüft, ob ein Nutzername bereits vergeben ist
     * @param nickname Zu prüfender Nutzername
     * @return true, falls Nutzername bereits vergeben
     */
    public synchronized boolean userExists(String nickname) {
        return nicknamesToClients.keySet().contains(nickname);
    }

    /** GETTER & SETTER **/

    /**
     * Gibt alle angemeldeten Clients mit Nutzernamen zurück
     * @return Alle angemeldeten Clients mit Nutzernamen
     */
    // TODO Zugriffe auf Thread-List synchronisieren --> Zugriffe auf Liste kapseln mit einer synchronized Methode
    public synchronized String getUserlist() {
        String result = "";
        for (final String client : nicknamesToClients.keySet()) {
            result += client + '\n';
        }
        return result;
    }

    /**
     * Gibt alle verfügbaren Befehle aus samt Nutzungsbeschreibung
     * @return siehe Beschreibung
     */
    public String getHelp() {
        return "/help - display this help" + CRLF +
                "/users - Show list of all users on this server" + CRLF +
                "/list - See /users" + CRLF +
                "/rename <desired nickname> - Rename yourself using this command" + CRLF +
                "/w <nickname> - Whisper to a user" + CRLF +
                "/poke - Poke the s$%# outta somebody" + CRLF +
                "/logout - Sign off the chat";
    }

    /**
     * Gibt an, ob der übergebene Nutzername zulässig ist.
     * @param inNickname Zu prüfender Nutzername
     * @return true, falls Nutzername zulässig ist
     */
    public boolean isValidNickname(String inNickname) {
        // TODO mocked
        return !inNickname.isEmpty() && !userExists(inNickname);
    }
}

// ----------------------------------------------------------------------------

class ClientThread extends Thread {
    private static final String CRLF = "\r\n";
    /*
        * Arbeitsthread, der eine existierende Socket-Verbindung zur Bearbeitung
        * erhaelt
        */
    private final int name;
    private final Socket socket;
    private final TCPServer server;
    private String nickname;
    private BufferedReader inFromClient;
    private DataOutputStream outToClient;
    boolean clientServiceRequested = true; // Guard der Hauptschleife

    private boolean isAuthorized;

    private int logcount;
    /**
     * Konstruktor
     * @param num Thread-Nummer
     * @param socket Zugehöriger Socket
     * @param server Serverobjekt
     */
    public ClientThread(int num, Socket socket, TCPServer server) {
        this.name = num;
        this.socket = socket;
        this.server = server;
        isAuthorized = false;
    }

    public void shutDown() {
		try {
			this.socket.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}
		this.stop();
	}

	private void log(String message) {
        System.err.println(++logcount + ": " + message);
    }

    @Override
	public void run() {

        try {
            /* Socket-Basisstreams durch spezielle Streams filtern */
            inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            outToClient = new DataOutputStream(socket.getOutputStream());

            // Protocol
            if (readFromClient().equals("Protocol: " + TCPServer.VERSION)) {
            	clientServiceRequested = true;
            	writeToClient("ACCESS GRANTED");
            } else {
            	clientServiceRequested = false;
            	writeToClient("ACCESS DENIED - Supported Protocol: " + TCPServer.VERSION);
            }

            /** HAUPTSCHLEIFE **/
            while (clientServiceRequested) {
            /* Eingehende Nachricht vom Client einlesen und verarbeiten */
                final String inMessage = readFromClient();
                String commandString = "";
                String outMessage = "";

                final Scanner s = new Scanner(inMessage).useDelimiter(" ");

                if (isCommand(inMessage)) {
                    commandString += s.next();

                    /* Prüfen, ob Client bereits zur Chat-Kommunikation authorisiert ist */
                    if (isAuthorized) {
                        /* Prüfen, um welchen Befehl es sich handelt */
                        if (isCommand("msg", commandString)) {
                            outMessage = inMessage.replaceFirst("/msg", "");
                            server.notifyClients(outMessage, this);
                        } else if (isCommand("w", commandString)) {
                            if (s.hasNext()) {
                                final String recipient = s.next();
                                outMessage = inMessage.substring((commandString + recipient).length() + 1);
                                server.whisperToClient(outMessage, this, recipient);
                            } else {
                                writeErrorMessageToClient("Recipient missing. /w <recipient nickname> <message>");
                            }
                        } else if (isCommand("poke", commandString)) {
                            if (s.hasNext()) {
                                final String recipient = s.next();
                                server.pokeClient(this, recipient);
                            } else {
                                writeErrorMessageToClient("Recipient missing. /poke <recipient nickname>");
                            }
                        } else if (isCommand("help", commandString)) {
                            writeServerMessageToClient(server.getHelp());
                        } else if (isCommand("users", commandString) || (isCommand("list", commandString)))  {
                        	writeToClient("/USERLIST\n" + server.getUserlist());
                        } else if (isCommand("rename", commandString)) {
                            String desiredNickname = "";
                            if (s.hasNext()) {
                                desiredNickname = s.next();
                            } else {
                                writeErrorMessageToClient("Nickname invalid. /rename <desired nickname>");
                            }
                            if (server.onClientRename(desiredNickname, this)) {
                                writeToClient("/RENAMESUCCESS " + desiredNickname);
                            } else {
                                writeToClient("Nickname invalid or already taken. /rename <desired nickname>");
                            }
                        } else if (isCommand("logout", commandString) || isCommand("quit", commandString)) {
                            clientServiceRequested = false;
                        } else {
                            writeToClient("/ERR_INVALID_CMD");
                        }
                    // Client ist noch nicht authorisiert
                    } else {
                        // Anmelde-Befehl empfangen
                        if (isCommand("login", commandString)) {
                            String inNickname = "";
                            if (s.hasNext()) {
                                // Nickname Parameter einlesen
                                inNickname = s.next();
                                if (server.isValidNickname(inNickname)) {
                                    if (server.registerUser(inNickname, this)) {
                                    	writeToClient("/LOGINSUCCESS " + inNickname);
                                        writeServerMessageToClient("Login successful. Hello " + inNickname + "!");
                                        isAuthorized = true;
                                    }
                                // falls Nickname nicht zulässig
                                } else {
                                    writeErrorMessageToClient("Nickname invalid or already taken. " + inNickname);
                                    writeErrorMessageToClient("Please try again: /login <name>");
                                    isAuthorized = false;
                                }
                            // falls kein Parameter angegeben wurde
                            } else {
                                writeErrorMessageToClient("Nickname missing. /login <nickname>");
                                isAuthorized = false;
                            }
                        // falls nicht, wie erforderlich, der Anmelde-Befehl empfangen wurde
                        } else {
//                            log(commandString);
                            if (isCommand("quit", commandString)) {
                                clientServiceRequested = false;
                            } else {
                                writeErrorMessageToClient("Please choose a nickname first. /login <nickname>");
                            }
                            isAuthorized = false;
                        }
                    }
                } else {
                    writeToClient("/ERR_MALFORMED_CMD");
                }
                s.close();
            }

            /* Socket-Streams schliessen --> Verbindungsabbau */
            socket.close();
        } catch (final IOException e) {
            System.err.println("Connection aborted by client!");
        } finally {
            System.out.println("TCP Worker Thread " + name + " stopped!");
            server.removeUser(this);
        }
        server.removeUser(this);
    }

    /** OPERATIONS **/
    
    /**
     * Liest eingehende Zeichen aus dem Puffer
     * @return Eingelesene Zeichenkette aus dem Puffer
     * @throws IOException
     */
    private String readFromClient() throws IOException {
        final String request = inFromClient.readLine();
        return request;
    }

    /**
     * Schreibt eine Nachricht in den Puffer des Clients
     * @param outMessage Zu sendende Nachricht
     * @throws IOException
     */
    public void writeToClient(String outMessage) throws IOException {
        /* Sende den String als Antwortzeile (mit CRLF) zum Client */
        final String out = outMessage + '\r' + '\n';
        outToClient.writeBytes(out);
        System.out.println(out);
    }

    /**
     * Sendet eine Servernachricht an den Client
     * @param outMessage Zu versendene Nachricht
     * @throws IOException
     */
    public void writeServerMessageToClient(String outMessage) throws IOException {
        writeToClient("SERVER: " + outMessage);
    }

    /**
     * Sendet dem Client eine Fehlermeldung
     * @param errMessage Zu sendene Fehlermeldung
     * @throws IOException
     */
    public void writeErrorMessageToClient(String errMessage) throws IOException {
        writeToClient("ERROR: " + errMessage);
    }

    /** PREDICATES **/

    /**
     * Prüft, ob eingehende Nachricht einen Befehl darstellt
     * @param inMessage Eingehende Nachricht
     * @return true, falls Nachricht ein Befehl ist.
     */
    private boolean isCommand(String inMessage) {
        return inMessage.startsWith("/");
    }

    /**
     * Prüft, ob eingehende Nachricht einem bestimmten Befehl entspricht
     * @param inMessage Eingehende Nachricht
     * @param command   Erwarteter Befehl
     * @return true, falls erwarteter Befehl empfangen
     */
    private boolean isCommand(String command, String inMessage) {
        return inMessage.toLowerCase().startsWith("/" + command.toLowerCase());
    }
    
    /**
     * Gibt an, ob der Client bereits authorisiert ist am Chat teilzunehmen
     * @return true, falls Client authorisiert ist
     */
    public boolean isAuthorized() {
        return isAuthorized;
    }

    /** GETTER & SETTER **/

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getNickname() {
        return nickname;
    }
    
}
