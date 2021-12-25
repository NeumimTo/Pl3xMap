package net.pl3x.map.plugin.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.pl3x.map.plugin.Logger;
import net.pl3x.map.plugin.MapDataStorage;
import net.pl3x.map.plugin.Pl3xMapPlugin;
import net.pl3x.map.plugin.configuration.Config;
import net.pl3x.map.plugin.data.Image;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.sql.*;
import java.util.Locale;

public class DatabaseStorage implements MapDataStorage {

    private final Pl3xMapPlugin plugin;
    private HikariDataSource ds;

    public DatabaseStorage(Pl3xMapPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void shutdown() {
        if (ds != null) {
            ds.close();
        }
    }

    @Override
    public void save(BufferedImage image, String png, File file) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            byte[] data = baos.toByteArray();
            updateOrInsertTile(data, );
        } catch (IOException e) {
            e.printStackTrace();
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

    //todo cache lookup
    @Override
    public BufferedImage load(String dir, int scaledX, int scaledZ, int zoom) {
        String sql = "select tile from %table% where world = ? and x = ? and z = ? and zoom = ?".replaceAll("%table%", Config.DB_TABLE_NAME_MARKERS);
        try (Connection connection = ds.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, dir);
            statement.setInt(2, scaledX);
            statement.setInt(3, scaledZ);
            statement.setInt(4, zoom);

            try (ResultSet resultSet = statement.getResultSet()) {
                if (resultSet.next()) {
                    byte[] blob = resultSet.getBytes(1);
                    BufferedImage image = new BufferedImage(Image.SIZE, Image.SIZE, BufferedImage.TYPE_INT_ARGB);
                    image.setData(Raster.createRaster(image.getSampleModel(), new DataBufferByte(blob, blob.length), new Point()));
                    return image;
                }
            } catch (SQLException ex) {
                Logger.severe("Unable to read query result" + query ,ex);
            }

        } catch (SQLException ex) {
            Logger.severe("Unable to update or inesrt tile " + world + " " + x + "," + z);
        }
        return new BufferedImage(Image.SIZE, Image.SIZE, BufferedImage.TYPE_INT_ARGB);
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

    public void updateOrInsertTile(byte[] tile, String world, int x, int z, int zoom) {
        try (Connection connection = ds.getConnection();
             PreparedStatement statement = connection.prepareStatement(updateOrInsertQuery(connection).replaceAll("%table%", Config.DB_TABLE_NAME_TILES))) {

            statement.setString(1, world);
            statement.setInt(2, x);
            statement.setInt(3, z);
            statement.setInt(4, zoom);
            statement.setBytes(5, tile);
            statement.setBytes(6, tile);

            statement.executeUpdate();

        } catch (SQLException ex) {
            Logger.severe("Unable to update or inesrt tile " + world + " " + x + "," + z);
        }
    }

    private String updateOrInsertQuery(Connection connection) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        String databaseProductName = metaData.getDatabaseProductName();
        switch (databaseProductName.toLowerCase(Locale.ROOT)) {
            case "mysql":
            case "mariadb":
                return "insert into %table%(world,x,z,zoom,tile) values(?,?,?,?,?) on duplicate key update tile = ?";
        }
        throw new RuntimeException("not supported database " + databaseProductName + " " + metaData.getDatabaseMajorVersion() + "." + metaData.getDatabaseMinorVersion());
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
