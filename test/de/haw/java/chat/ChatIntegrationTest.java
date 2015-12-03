package de.haw.java.chat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.haw.java.chat.client.TCPClient;
import de.haw.java.chat.server.TCPServer;

public class ChatIntegrationTest {

	private static TCPServer server;
	private static TCPClient client1;
	private static TCPClient client2;
	private static UINullStub ui1;
	private static UINullStub ui2;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		final int serverPort = 45678;
		final int maxThreads = 2;
		server = new TCPServer(serverPort, maxThreads);
		server.run();
		final String hostname = "localhost";
		ui1 = new UINullStub();
		client1 = new TCPClient(hostname, serverPort, ui1, "client1");
		ui2 = new UINullStub();
		client2 = new TCPClient(hostname, serverPort, ui2, "client2");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		client1.shutDown();
		client2.shutDown();
		server.shutDown();
	}

	@Test
	public void sendChatMessage_twoClientsOnServer_clientsGetMessage() {
		client1.writeToServer(ChatCommands.MESSAGE + " headroll");
	}

}
