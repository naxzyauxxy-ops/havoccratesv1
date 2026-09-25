/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  java.lang.IllegalStateException
 *  java.lang.Integer
 *  java.lang.Object
 *  java.lang.Override
 *  java.lang.String
 *  java.lang.Throwable
 *  java.sql.Connection
 *  java.sql.DriverManager
 *  java.sql.PreparedStatement
 *  java.sql.ResultSet
 *  java.sql.SQLException
 *  java.sql.Statement
 *  java.util.Map$Entry
 */
package net.havoc.crates.data.storage;

import java.lang.IllegalStateException;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.Throwable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import net.havoc.crates.CratesPlugin;
import net.havoc.crates.data.Profile;
import net.havoc.crates.data.storage.KeyStorage;

public class SqlStorage
implements KeyStorage {
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
                this.connection = DriverManager.getConnection((String)("jdbc:mysql://" + host + "/" + database + "?autoReconnect=true&useSSL=false"), (String)user, (String)password);
            } else {
                this.connection = DriverManager.getConnection((String)("jdbc:sqlite:" + this.plugin.getDataFolder().getAbsolutePath() + "/keys.db"));
            }
        }
        return this.connection;
    }

    @Override
    public void init() {
        try (Statement statement = this.connection().createStatement();){
            statement.execute("CREATE TABLE IF NOT EXISTS havoc_crate_keys (uuid VARCHAR(36) NOT NULL, crate VARCHAR(64) NOT NULL, amount INT NOT NULL, PRIMARY KEY (uuid, crate))");
            statement.execute("CREATE TABLE IF NOT EXISTS havoc_crate_settings (uuid VARCHAR(36) NOT NULL, alerts INT NOT NULL, PRIMARY KEY (uuid))");
        }
        catch (SQLException exception) {
            throw new IllegalStateException("Could not initialise the SQL storage", (Throwable)exception);
        }
    }

    @Override
    public void load(Profile profile) {
        ResultSet result;
        PreparedStatement statement;
        profile.setAlerts(this.plugin.getMainConfig().getBoolean("ALERTS.DEFAULT", true));
        try {
            statement = this.connection().prepareStatement("SELECT alerts FROM havoc_crate_settings WHERE uuid = ?");
            try {
                statement.setString(1, profile.getUuid().toString());
                result = statement.executeQuery();
                try {
                    if (result.next()) {
                        profile.setAlerts(result.getInt("alerts") != 0);
                    }
                }
                finally {
                    if (result != null) {
                        result.close();
                    }
                }
            }
            finally {
                if (statement != null) {
                    statement.close();
                }
            }
        }
        catch (SQLException exception) {
            this.plugin.getLogger().severe("Could not load settings for " + String.valueOf(profile.getUuid()) + ": " + exception.getMessage());
        }
        profile.getKeys().clear();
        try {
            statement = this.connection().prepareStatement("SELECT crate, amount FROM havoc_crate_keys WHERE uuid = ?");
            try {
                statement.setString(1, profile.getUuid().toString());
                result = statement.executeQuery();
                try {
                    while (result.next()) {
                        profile.getKeys().put(result.getString("crate").toLowerCase(), result.getInt("amount"));
                    }
                }
                finally {
                    if (result != null) {
                        result.close();
                    }
                }
            }
            finally {
                if (statement != null) {
                    statement.close();
                }
            }
        }
        catch (SQLException exception) {
            this.plugin.getLogger().severe("Could not load keys for " + String.valueOf(profile.getUuid()) + ": " + exception.getMessage());
        }
    }

    @Override
    public void save(Profile profile) {
        try {
            try (PreparedStatement settings = this.connection().prepareStatement(this.mysql ? "REPLACE INTO havoc_crate_settings (uuid, alerts) VALUES (?, ?)" : "INSERT OR REPLACE INTO havoc_crate_settings (uuid, alerts) VALUES (?, ?)");){
                settings.setString(1, profile.getUuid().toString());
                settings.setInt(2, profile.isAlerts() ? 1 : 0);
                settings.executeUpdate();
            }
            try (PreparedStatement delete = this.connection().prepareStatement("DELETE FROM havoc_crate_keys WHERE uuid = ?");){
                delete.setString(1, profile.getUuid().toString());
                delete.executeUpdate();
            }
            try (PreparedStatement insert = this.connection().prepareStatement("INSERT INTO havoc_crate_keys (uuid, crate, amount) VALUES (?, ?, ?)");){
                for (Map.Entry entry : profile.getKeys().entrySet()) {
                    insert.setString(1, profile.getUuid().toString());
                    insert.setString(2, (String)entry.getKey());
                    insert.setInt(3, ((Integer)entry.getValue()).intValue());
                    insert.addBatch();
                }
                insert.executeBatch();
            }
        }
        catch (SQLException exception) {
            this.plugin.getLogger().severe("Could not save keys for " + String.valueOf(profile.getUuid()) + ": " + exception.getMessage());
        }
    }

    @Override
    public void close() {
        try {
            if (this.connection != null && !this.connection.isClosed()) {
                this.connection.close();
            }
        }
        catch (SQLException exception) {
            this.plugin.getLogger().warning("Could not close the SQL connection: " + exception.getMessage());
        }
    }
}
