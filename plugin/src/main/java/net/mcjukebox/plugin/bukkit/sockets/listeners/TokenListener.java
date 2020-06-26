package net.mcjukebox.plugin.bukkit.sockets.listeners;

import io.socket.emitter.Emitter;
import org.json.JSONObject;

import java.util.*;

public class TokenListener implements Emitter.Listener {

	private final Map<String, List<Object>> tokenLocks;
	private final Map<String, String> tokens;

	public TokenListener() {
		tokenLocks = new HashMap<>();
		tokens = new HashMap<>();
	}

	@Override
	public void call(Object... objects) {
		JSONObject data = (JSONObject) objects[0];
		String username = data.getString("username");
		String token = data.getString("token");
		tokens.put(username, token);

		if(!tokenLocks.containsKey(username)) return;

		for (Object lock : tokenLocks.get(username)) {
			synchronized (lock) {
				lock.notify();
			}
		}

		tokenLocks.remove(username);
	}

	public void addLock(String username, Object lock) {
		if (tokenLocks.containsKey(username)) {
			tokenLocks.get(username).add(lock);
		} else {
			ArrayList<Object> locks = new ArrayList<>();
			locks.add(lock);
			tokenLocks.put(username, locks);
		}
	}

	public String getToken(String username) {
		return tokens.get(username);
	}

}
