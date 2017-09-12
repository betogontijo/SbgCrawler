package br.com.betogontijo.sbgreader;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class ConnectionManager implements Closeable {

	MongoClient mongoClient;

	Connection mariaDbConnection;

	@SuppressWarnings("rawtypes")
	MongoCollection<Map> domainsDB;

	@SuppressWarnings("rawtypes")
	MongoCollection<Map> documentsDB;

	Properties properties = new Properties();

	ConnectionManager() throws IOException {
		// Load properties
		properties.load(ClassLoader.getSystemResourceAsStream("sbgreader.properties"));
	}

	@SuppressWarnings("rawtypes")
	public MongoCollection<Map> getDomainsConnection() {
		if (domainsDB != null) {
			return domainsDB;
		}
		if (mongoClient == null) {
			mongoClient = new MongoClient(properties.getProperty("mongodb.host"));
		}
		MongoDatabase database = mongoClient.getDatabase(properties.getProperty("mongodb.database"));
		domainsDB = database.getCollection(properties.getProperty("mongodb.colletion.domains"), Map.class);
		return domainsDB;
	}

	@SuppressWarnings("rawtypes")
	public MongoCollection<Map> getDocumentsConnection() {
		if (documentsDB != null) {
			return documentsDB;
		}
		if (mongoClient == null) {
			mongoClient = new MongoClient(properties.getProperty("mongodb.host"));
		}
		MongoDatabase database = mongoClient.getDatabase(properties.getProperty("mongodb.database"));
		documentsDB = database.getCollection(properties.getProperty("mongodb.colletion.documents"), Map.class);
		return documentsDB;
	}

	public Connection getReferencesConnection() {
		if (mariaDbConnection != null) {
			return mariaDbConnection;
		}
		try {
			// Get database connection driver
			String driver = properties.getProperty("maridb.driver");
			Class.forName(driver);

			// Configure connection parameters
			String url = properties.getProperty("maridb.url");
			String username = properties.getProperty("maridb.username");
			String password = properties.getProperty("maridb.password");

			// Try connection
			mariaDbConnection = DriverManager.getConnection(url, username, password);

			return mariaDbConnection;
		} catch (ClassNotFoundException e) {
			System.out.println("O driver expecificado nao foi encontrado.");
			return null;
		} catch (SQLException e) {
			System.out.println("Nao foi possivel conectar ao banco de dados.");
			return null;
		}
	}

	public void close() {
		try {
			mongoClient.close();
			mariaDbConnection.close();
		} catch (Exception e) {
		}
	}
}