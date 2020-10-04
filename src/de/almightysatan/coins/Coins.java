package de.almightysatan.coins;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class Coins {

	static final String PREFIX = "§7[§eCoins§7] ";

	static final Executor EXECUTOR = Executors.newSingleThreadExecutor();
	private static Mysql sql;
	private static Map<UUID, Integer> players = new HashMap<>();
	private static List<CoinsChangeListener> listeners = new Vector<>();

	static void init(String url, String user, String password) {
		try {
			sql = new Mysql(url, user, password);
		} catch(SQLException e) {
			throw new Error("Unable to connect to database", e);
		}
	}

	static void loadPlayer(UUID uuid) {
		try {
			players.put(uuid, sql.getCoins(uuid));
		} catch(SQLException e) {
			throw new Error("Unable to load coins for uuid " + uuid, e);
		}
	}
	
	static void callEvents(UUID uuid, int oldBalance, int newBalance) {
		listeners.forEach(listener -> listener.onCoinsChange(uuid, oldBalance, newBalance));
	}

	public static void getOfflineCoins(UUID uuid, Consumer<Integer> callback) {
		if(players.containsKey(uuid))
			callback.accept(players.get(uuid));
		else
			EXECUTOR.execute(() -> {
				try {
					callback.accept(sql.getCoins(uuid));
				} catch(SQLException e) {
					throw new Error("Unable to load coins for uuid " + uuid, e);
				}
			});
	}

	public static int getCoins(UUID uuid) {
		return players.get(uuid);
	}

	public static void setCoins(UUID uuid, int amount) {
		if(players.containsKey(uuid))
			players.put(uuid, amount);

		EXECUTOR.execute(() -> {
			try {
				sql.setCoins(uuid, amount);
			} catch(SQLException e) {
				e.printStackTrace();
			}
		});
	}

	public static void addCoins(UUID uuid, int amount) {
		if(players.containsKey(uuid))
			players.put(uuid, players.get(uuid) + amount);

		EXECUTOR.execute(() -> {
			try {
				sql.addCoins(uuid, amount);
			} catch(SQLException e) {
				e.printStackTrace();
			}
		});
	}

	public static void removeCoins(UUID uuid, int amount) {
		addCoins(uuid, -amount);
	}
	
	public static void registerListener(CoinsChangeListener listener) {
		if(listener == null)
			throw new NullPointerException();
		
		listeners.add(listener);
	}
	
	public static void unregisterListener(CoinsChangeListener listener) {
		if(listener == null)
			throw new NullPointerException();
		
		listeners.remove(listener);
	}
}
