package de.haw.java.chat.client;

@FunctionalInterface
public interface ServerResponseCommand {
	public void apply(String string);
}
