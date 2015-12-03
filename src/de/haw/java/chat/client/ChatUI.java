package de.haw.java.chat.client;

public interface ChatUI {

	void addInfoMessage(String infoMessage);
	
	void addErrorMessage(String errorMessage);

	void setName(String name);

	void addChatMessage(String reply);

	void setClient(TCPClient tcpClient);

	void startUi();

	String getName();
	
	boolean wantsToQuit();
	
	void quit();

}
