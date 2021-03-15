package de.almightysatan.coins;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Mysql {

	private static final String DATABASE = "`coins`";
	private static final String TABLE = DATABASE + ".`coins`";
	private static final String TABLE_EVENTS = "`coins`.`events`";

	private Connection connection;
	private PreparedStatement checkEventsStatement;

	Mysql(String url, String user, String password) throws SQLException {
		this.connection = DriverManager.getConnection(url + "?" + "user=" + user + "&" + "password=" + password + "&allowMultiQueries=true&autoReconnect=true");

		executeUpdate("CREATE DATABASE IF NOT EXISTS " + DATABASE);
		this.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DATABASE + ";"
				+ "CREATE TABLE IF NOT EXISTS " + TABLE + " (`index` INT NOT NULL AUTO_INCREMENT, `uuid` VARCHAR(36) NOT NULL, `balance` INT NULL, PRIMARY KEY (`index`), UNIQUE KEY `uuid_unique` (`uuid`));"
				+ "CREATE TABLE IF NOT EXISTS " + TABLE_EVENTS + " (`uuid` VARCHAR(36) NOT NULL, PRIMARY KEY (`uuid`), UNIQUE KEY `uuid_unique` (`uuid`));");
		
		this.checkEventsStatement = this.connection.prepareStatement("SELECT * FROM coins.events;");
	}

	private void executeUpdate(String command) throws SQLException {
		PreparedStatement pqs = this.connection.prepareStatement(command);
		pqs.executeUpdate();
	}

	private ResultSet executeQuery(String command) throws SQLException {
		PreparedStatement pqs = this.connection.prepareStatement(command);
		return pqs.executeQuery();
	}

	int getCoins(UUID uuid, boolean removeEvent) throws SQLException {
		ResultSet r = executeQuery("SELECT `balance` from " + TABLE + " WHERE `uuid` = '" + uuid.toString() + "';" + (removeEvent ? ("DELETE FROM `coins`.`events` WHERE (`uuid` = '" + uuid.toString() + "');") : ""));

		if(r.next())
			return r.getInt(1);
		else
			return Coins.STARTER_COINS;
	}

	void setCoins(UUID uuid, int balance, boolean setEvent) throws SQLException {
        this.executeUpdate("INSERT INTO " + TABLE + " (uuid, balance) VALUES ('" + uuid.toString() + "', " + balance + ") ON DUPLICATE KEY UPDATE balance=" + balance + ";" + (setEvent ? ("REPLACE INTO " + TABLE_EVENTS + " (uuid) VALUES ('" + uuid + "');") : ""));
    }
    
    void addCoins(UUID uuid, int balance, boolean setEvent) throws SQLException {
        this.executeUpdate("INSERT INTO " + TABLE + " (uuid, balance) VALUES ('" + uuid.toString() + "', " + balance + ") ON DUPLICATE KEY UPDATE balance=balance+" + balance + ";" + (setEvent ? ("REPLACE INTO " + TABLE_EVENTS + " (uuid) VALUES ('" + uuid + "');") : ""));
    }
    
    List<UUID> getEvents() throws SQLException {
        ResultSet result = this.checkEventsStatement.executeQuery();
        List<UUID> uuids = new ArrayList<UUID>();
        
        while (result.next())
            uuids.add(UUID.fromString(result.getString(1)));
            
        return uuids;
    }
    
    void removeEvents(List<UUID> events) throws SQLException {
        final StringBuilder command = new StringBuilder();
        
        for (final UUID event : events)
            command.append("DELETE FROM " + TABLE_EVENTS + " WHERE (`uuid` = '" + event.toString() + "');");
        
        this.executeUpdate(command.toString());
    }
}
