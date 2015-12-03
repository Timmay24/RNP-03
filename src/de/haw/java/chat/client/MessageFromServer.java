package de.haw.java.chat.client;

public class MessageFromServer {
	
	private static final String DELIMITER = " ";
	private final String command;
	private final String parameter;

	public MessageFromServer(String commandString) {
		if (null != commandString) {
			command = commandString.substring(0, Math.max(0, 5));
			parameter = commandString.substring(Math.min(commandString.length() - 1, 5));
		} else {
			command = "";
			parameter = "";
		}
	}

	public String getCommand() {
		return command;
	}
	
	public String getParameter() {
		return parameter;
	}
}
