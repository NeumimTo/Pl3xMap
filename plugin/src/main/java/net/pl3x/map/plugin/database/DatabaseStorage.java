package net.pl3x.map.plugin.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.pl3x.map.plugin.Logger;
import net.pl3x.map.plugin.Pl3xMapPlugin;
import net.pl3x.map.plugin.configuration.Config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseStorage {

    private final Pl3xMapPlugin plugin;
    private HikariDataSource ds;

    public DatabaseStorage(Pl3xMapPlugin plugin) {
        this.plugin = plugin;
    }

    public void shutdown() {
        if (ds != null) {
            ds.close();
        }
    }

    public void setup() {
        shutdown();

        File file = new File(plugin.getDataFolder(), "database.properties");
        if (!file.exists()) {
            try (InputStream stream = getClass().getClassLoader().getResourceAsStream("database.properties");) {
                Files.write(file.toPath(), stream.readAllBytes(), StandardOpenOption.CREATE_NEW);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        HikariConfig config = new HikariConfig(file.getAbsolutePath());
        ds = new HikariDataSource(config);
    }

    public void updatePlayers(String json) {
        updateRealtimeData(json, "P");
    }

    public void updateMarkers(String json) {
        updateRealtimeData(json, "M");
    }

    public String getPlayers() {
        return getRealtimeData("P");
    }

    public String getMarkers() {
        return getRealtimeData("M");
    }

    public void updateOrInsertTile(byte[] blob, String world, int x, int z, int zoom) {
        try (Connection connection = ds.getConnection();
             PreparedStatement statement = connection.prepareStatement("");
             ResultSet rs = statement.executeQuery()){

            while (rs.next()) {

            }

        } catch (SQLException ex) {

        }
    }

    private void updateRealtimeData(String json, String markerType) {
        String sql = "update %table% set data = ? where type = ?".replaceAll("%table%", Config.DB_TABLE_NAME_MARKERS);

        runUpdateQuery(sql, preparedStatement -> {
            preparedStatement.setString(1, json);
            preparedStatement.setString(2, markerType);
        });
    }

    private String getRealtimeData(String markerType) {
        String sql = "select data from %table% where type = ?".replaceAll("%table%", Config.DB_TABLE_NAME_MARKERS);

        return stringRsQuery(sql, preparedStatement -> {
            preparedStatement.setString(1, markerType);
        });
    }

    private void runUpdateQuery(String query, Callback paramCallback) {
        try (Connection connection = ds.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            paramCallback.call(statement);
            statement.executeUpdate();
        } catch (SQLException ex) {
            Logger.severe("Unable to execute query" + query ,ex);
        }
    }

    private String stringRsQuery(String query, Callback paramCallback) {
        try (Connection connection = ds.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            paramCallback.call(statement);

            try (ResultSet resultSet = statement.getResultSet()) {
                if (resultSet.next()) {
                    return resultSet.getString(1);
                }
            } catch (SQLException ex) {
                Logger.severe("Unable to read query result" + query ,ex);
            }

        } catch (SQLException ex) {
            Logger.severe("Unable to execute query" + query ,ex);
        }
        return "";
    }

    @FunctionalInterface
    private interface Callback {
        void call(PreparedStatement preparedStatement) throws SQLException;
    }
}
