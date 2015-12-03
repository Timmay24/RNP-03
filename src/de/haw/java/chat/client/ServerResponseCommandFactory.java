package de.haw.java.chat.client;

import java.util.HashMap;

public class ServerResponseCommandFactory {
	
	private final HashMap<String, ServerResponseCommand>	commands;
	
	private ServerResponseCommandFactory() {
		this.commands = new HashMap<>();
	}

	private void addCommand(String name, ServerResponseCommand command) {
		this.commands.put(name, command);
	}
	
	public void processReply(MessageFromServer command) {
		if ( this.commands.containsKey(command.getCommand()) ) {
			this.commands.get(command.getCommand()).apply(command.getParameter());
		}
	}

	public void listCommands() {
		System.out.println("Commands enabled :");
		this.commands.keySet().stream().forEach(System.out::println);
	}
	
	public static ServerResponseCommandFactory init(ChatUI ui) throws RuntimeException {
		final ServerResponseCommandFactory cf = new ServerResponseCommandFactory();
		cf.addCommand("", (parameter)                   -> ui.addInfoMessage("Unreadable or empty message from server."));
		cf.addCommand("/MSGE", (parameter)      -> ui.addChatMessage(parameter));
		cf.addCommand("/QUIT", (parameter)      -> {
			if (ui.wantsToQuit()) {
				ui.quit();
			}
		});
		return cf;
	}
}
