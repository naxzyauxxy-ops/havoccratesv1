package net.havoc.crates.data.storage;

import net.havoc.crates.CratesPlugin;
import net.havoc.crates.data.Profile;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

/**
 * SQLite / MySQL backend. Falls back to flatfile in {@link net.havoc.crates.data.ProfileManager}
 * when the JDBC driver is not shipped by the server.
 */
public class SqlStorage implements KeyStorage {

    private final CratesPlugin plugin;
    private final boolean mysql;
    private Connection connection;

    public SqlStorage(CratesPlugin plugin, boolean mysql) {
        this.plugin = plugin;
        this.mysql = mysql;
    }

    private Connection connection() throws SQLException {
        if (this.connection == null || this.connection.isClosed()) {
            if (this.mysql) {
                String host = this.plugin.getMainConfig().getString("DATABASE.MYSQL.HOST", "localhost");
                String database = this.plugin.getMainConfig().getString("DATABASE.MYSQL.DATABASE", "havoc_crates");
                String user = this.plugin.getMainConfig().getString("DATABASE.MYSQL.USER", "root");
                String password = this.plugin.getMainConfig().getString("DATABASE.MYSQL.PASSWORD", "");
                this.connection = DriverManager.getConnection(
                        "jdbc:mysql://" + host + "/" + database + "?autoReconnect=true&useSSL=false",
                        user, password);
            } else {
                this.connection = DriverManager.getConnection(
                        "jdbc:sqlite:" + this.plugin.getDataFolder().getAbsolutePath() + "/keys.db");
            }
        }
        return this.connection;
    }

    @Override
    public void init() {
        try (Statement statement = connection().createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS havoc_crate_keys ("
                    + "uuid VARCHAR(36) NOT NULL, "
                    + "crate VARCHAR(64) NOT NULL, "
                    + "amount INT NOT NULL, "
                    + "PRIMARY KEY (uuid, crate))");
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not initialise the SQL storage", exception);
        }
    }

    @Override
    public void load(Profile profile) {
        profile.getKeys().clear();
        try (PreparedStatement statement = connection()
                .prepareStatement("SELECT crate, amount FROM havoc_crate_keys WHERE uuid = ?")) {
            statement.setString(1, profile.getUuid().toString());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    profile.getKeys().put(result.getString("crate").toLowerCase(), result.getInt("amount"));
                }
            }
        } catch (SQLException exception) {
            this.plugin.getLogger().severe("Could not load keys for " + profile.getUuid() + ": " + exception.getMessage());
        }
    }

    @Override
    public void save(Profile profile) {
        try {
            try (PreparedStatement delete = connection()
                    .prepareStatement("DELETE FROM havoc_crate_keys WHERE uuid = ?")) {
                delete.setString(1, profile.getUuid().toString());
                delete.executeUpdate();
            }
            try (PreparedStatement insert = connection()
                    .prepareStatement("INSERT INTO havoc_crate_keys (uuid, crate, amount) VALUES (?, ?, ?)")) {
                for (Map.Entry<String, Integer> entry : profile.getKeys().entrySet()) {
                    insert.setString(1, profile.getUuid().toString());
                    insert.setString(2, entry.getKey());
                    insert.setInt(3, entry.getValue());
                    insert.addBatch();
                }
                insert.executeBatch();
            }
        } catch (SQLException exception) {
            this.plugin.getLogger().severe("Could not save keys for " + profile.getUuid() + ": " + exception.getMessage());
        }
    }

    @Override
    public void close() {
        try {
            if (this.connection != null && !this.connection.isClosed()) {
                this.connection.close();
            }
        } catch (SQLException exception) {
            this.plugin.getLogger().warning("Could not close the SQL connection: " + exception.getMessage());
        }
    }
}
