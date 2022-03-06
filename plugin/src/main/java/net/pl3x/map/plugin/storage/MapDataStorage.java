package net.pl3x.map.plugin.storage;

import java.awt.image.BufferedImage;
import java.io.File;

public interface MapDataStorage {
    void shutdown();

    void save(BufferedImage image, String png, File file);

    //todo cache lookup
    BufferedImage load(String dir, int scaledX, int scaledZ, int zoom);
}
