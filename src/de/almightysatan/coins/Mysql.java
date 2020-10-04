package de.almightysatan.coins;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class Mysql {

	private static final String DATABASE = "`coins`";
	private static final String TABLE = DATABASE + ".`coins`";

	private Connection connection;

	Mysql(String url, String user, String password) throws SQLException {
		this.connection = DriverManager.getConnection(url + "?" + "user=" + user + "&" + "password=" + password);

		executeUpdate("CREATE DATABASE IF NOT EXISTS " + DATABASE);
		executeUpdate("CREATE TABLE IF NOT EXISTS " + TABLE + " (`index` INT NOT NULL AUTO_INCREMENT, `uuid` VARCHAR(36) NOT NULL, `balance` INT NULL, PRIMARY KEY (`index`), UNIQUE KEY uuid_unique (uuid))");
	}

	private void executeUpdate(String command) throws SQLException {
		PreparedStatement pqs = this.connection.prepareStatement(command);
		pqs.executeUpdate();
	}

	private ResultSet executeQuery(String command) throws SQLException {
		PreparedStatement pqs = this.connection.prepareStatement(command);
		return pqs.executeQuery();
	}

	int getCoins(UUID uuid) throws SQLException {
		ResultSet r = executeQuery("SELECT `balance` from " + TABLE + " WHERE `uuid` = '" + uuid.toString() + "'");

		if(r.next())
			return r.getInt(1);
		else
			return 0;
	}

	void setCoins(UUID uuid, int balance) throws SQLException {
		executeUpdate("INSERT INTO " + TABLE + " (uuid, balance) VALUES ('" + uuid.toString() + "', " + balance + ") ON DUPLICATE KEY UPDATE balance=" + balance);
	}
	
	void addCoins(UUID uuid, int balance) throws SQLException {
		executeUpdate("INSERT INTO " + TABLE + " (uuid, balance) VALUES ('" + uuid.toString() + "', " + balance + ") ON DUPLICATE KEY UPDATE balance=balance+" + balance);
	}
}
