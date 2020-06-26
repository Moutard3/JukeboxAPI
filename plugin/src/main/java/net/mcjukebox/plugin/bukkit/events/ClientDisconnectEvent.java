package net.mcjukebox.plugin.bukkit.events;

import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ClientDisconnectEvent extends Event {

	private static final HandlerList handlers = new HandlerList();

	@Getter private final String username;
	@Getter private final long timestamp;

	public ClientDisconnectEvent(String username, long timestamp) {
		super(true);
		this.username = username;
		this.timestamp = timestamp;
	}

	public HandlerList getHandlers() {
		return handlers;
	}

}
