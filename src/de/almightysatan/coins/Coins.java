package de.almightysatan.coins;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class Coins {

	static final String PREFIX = "§7[§eCoins§7] ";

	static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();

	private static Consumer<Runnable> scheduleSyncCallback;
	private static Function<UUID, Boolean> playerOnlineCallback;
	private static BiConsumer<UUID, String> sendMessageCallback;
	private static Mysql sql;
	private static Map<UUID, Integer> players = new HashMap<>();
	private static List<CoinsChangeListener> listeners = new Vector<>();

	static void init(Consumer<Runnable> scheduleSyncCallback, Function<UUID, Boolean> playerOnlineCallback, BiConsumer<UUID, String> sendMessageCallback, String url, String user, String password) {
		Coins.scheduleSyncCallback = scheduleSyncCallback;
		Coins.playerOnlineCallback = playerOnlineCallback;
		Coins.sendMessageCallback = sendMessageCallback;

		try {
			sql = new Mysql(url, user, password);
		} catch(SQLException e) {
			throw new Error("Unable to connect to database", e);
		}

		Coins.EXECUTOR.scheduleWithFixedDelay(Coins::updateAsync, 1L, 1L, TimeUnit.SECONDS);
		Coins.listeners.add(Coins::sendCoinsNotification);
	}


	private static void updateAsync() {
		List<UUID> events;
		try {
			events = (List<UUID>) sql.getEvents();
		}catch (SQLException e) {
			e.printStackTrace();
			return;
		}

		for(final UUID uuid : events) {
			if(Coins.playerOnlineCallback.apply(uuid)) {
				int oldBalance = getCoins(uuid);
				int newBalance = loadPlayer(uuid);
				Coins.scheduleSyncCallback.accept(() -> callEvents(uuid, oldBalance, newBalance));
			}
		}
	}

	static void callEvents(UUID uuid, int oldBalance, int newBalance) {
		if(oldBalance != newBalance)
			listeners.forEach(listener -> {
				try {
					listener.onCoinsChange(uuid, oldBalance, newBalance);
				}catch(Throwable t) {
					t.printStackTrace();
				}
			});
	}

	private static void sendCoinsNotification(UUID uuid, int oldBalance, int newBalance) {
		if(oldBalance < newBalance) {
			int amount = newBalance - oldBalance;
			
			if(amount == 1)
				Coins.sendMessageCallback.accept(uuid, "§7[§eCoins§7] Du hast §eeinen §7Coin erhalten");
			else
				Coins.sendMessageCallback.accept(uuid, "§7[§eCoins§7] Du hast §e" + amount + " §7Coins erhalten");
		}else {
			int amount = oldBalance - newBalance;
			
			if(amount == 1)
				Coins.sendMessageCallback.accept(uuid, "§7[§eCoins§7] Du hast §eeinen §7Coin verloren");
			else
				Coins.sendMessageCallback.accept(uuid, "§7[§eCoins§7] Du hast §e" + amount + " §7Coins verloren");
		}
	}

	static int loadPlayer(UUID uuid) {
		try {
			int balance = sql.getCoins(uuid, true);
			players.put(uuid, balance);
			return balance;
		} catch(SQLException e) {
			throw new Error("Unable to load coins for uuid " + uuid, e);
		}
	}

	public static void getOfflineCoins(UUID uuid, Consumer<Integer> callback) {
		if(players.containsKey(uuid))
			callback.accept(players.get(uuid));
		else
			EXECUTOR.execute(() -> {
				try {
					callback.accept(sql.getCoins(uuid, false));
				} catch(SQLException e) {
					throw new Error("Unable to load coins for uuid " + uuid, e);
				}
			});
	}

	public static int getCoins(UUID uuid) {
		return players.get(uuid);
	}

	public static void setCoins(UUID uuid, int newBalance) {
		final boolean online = Coins.playerOnlineCallback.apply(uuid);
		if (online) {
			int oldBalance = Coins.players.put(uuid, newBalance);
			Coins.scheduleSyncCallback.accept(() -> callEvents(uuid, oldBalance, newBalance));
		}

		EXECUTOR.execute(() -> {
			try {
				sql.setCoins(uuid, newBalance, !online);
			} catch(SQLException e) {
				e.printStackTrace();
			}
		});
	}

	public static void addCoins(UUID uuid, int amount) {
		boolean online = Coins.playerOnlineCallback.apply(uuid);
		if (online) {
			int oldBalance = Coins.players.get(uuid);
			int newBalance = oldBalance + amount;
			Coins.players.put(uuid, newBalance);
			Coins.scheduleSyncCallback.accept(() -> callEvents(uuid, oldBalance, newBalance));
		}

		EXECUTOR.execute(() -> {
			try {
				sql.addCoins(uuid, amount, !online);
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
