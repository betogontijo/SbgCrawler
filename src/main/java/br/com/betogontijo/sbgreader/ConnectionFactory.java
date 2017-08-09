package br.com.betogontijo.sbgreader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionFactory {
	public static java.sql.Connection getConnection() {
		Connection connection = null;
		try {
			// Get BD driver connection
			Class.forName("com.mysql.jdbc.Driver");
			// Connection parameters
			String url = "jdbc:mysql:ip:porta/schema";
			String username = "root";
			String password = "";
			// Test connection
			connection = DriverManager.getConnection(url, username, password);

			return connection;
		} catch (ClassNotFoundException e) {
			System.out.println("Specified driver not found.");
			return null;
		} catch (SQLException e) {
			System.out.println("Could not connect to database.");
			return null;
		}
	}
}