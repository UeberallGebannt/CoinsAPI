package de.almightysatan.coins;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class CoinsBukkit extends JavaPlugin implements Listener {

	@Override
	public void onEnable() {
		try {
			loadConfig();
		} catch(IOException e) {
			e.printStackTrace();
			return;
		}

		Bukkit.getPluginManager().registerEvents(this, this);
	}

	private void loadConfig() throws IOException {
		File configFile = new File("plugins/coins/config.yml");

		YamlConfiguration yamlConfiguration = null;

		if(!configFile.exists()) {
			configFile.getParentFile().mkdirs();
			configFile.createNewFile();

			yamlConfiguration = YamlConfiguration.loadConfiguration(configFile);

			yamlConfiguration.set("mysql.url", "jdbc:mysql://localhost:port/db-name");
			yamlConfiguration.set("mysql.user", "db-user");
			yamlConfiguration.set("mysql.pw", "db-password");
			yamlConfiguration.save(configFile);
		}else
			yamlConfiguration = YamlConfiguration.loadConfiguration(configFile);

		Coins.init(yamlConfiguration.getString("mysql.url"), yamlConfiguration.getString("mysql.user"), yamlConfiguration.getString("mysql.pw"));
	}

	@EventHandler
	public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent e) {
		Coins.loadPlayer(e.getUniqueId());
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		new CoinsCommand(new CoinsCommandSender() {

			@Override
			public boolean isPlayer() {
				return sender instanceof Player;
			}

			@Override
			public UUID getUUID() {
				return ((Player) sender).getUniqueId();
			}

			@Override
			public void sendMessage(String message) {
				sender.sendMessage(message);
			}

			@Override
			public boolean hasPermission(String permission) {
				return sender.hasPermission(permission);
			}
		}, args);
		
		return true;
	}
}
