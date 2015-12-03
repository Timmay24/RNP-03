package de.haw.java.chat.client;

public class UINullStub implements ChatUI {
	
	private String lastChatMessage;
	private String lastErrorMessage;
	private String lastInfoMessage;
	
	public UINullStub() {
		super();
	}

	@Override
	public void addInfoMessage(String infoMessage) {
		this.lastInfoMessage = infoMessage;
	}

	@Override
	public void setName(String string) {
	}

	@Override
	public void addChatMessage(String chatMessage) {
		this.lastChatMessage = chatMessage;
	}

	@Override
	public void setClient(TCPClient tcpClient) {
	}

	@Override
	public void startUi() {
	}

	@Override
	public void addErrorMessage(String errorMessage) {
		this.lastErrorMessage = errorMessage;
	}

	public String getLastChatMessage() {
		return lastChatMessage;
	}

	public String getLastErrorMessage() {
		return lastErrorMessage;
	}

	public String getLastInfoMessage() {
		return lastInfoMessage;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public boolean wantsToQuit() {
		return false;
	}

	@Override
	public void quit() {
	}

}
