package net.pl3x.map.plugin.storage;

import net.pl3x.map.plugin.Logger;
import net.pl3x.map.plugin.Pl3xMapPlugin;
import net.pl3x.map.plugin.configuration.Lang;
import net.pl3x.map.plugin.data.Image;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class FileStorage implements MapDataStorage {

    private Pl3xMapPlugin pl3xMapPlugin;

    public FileStorage(Pl3xMapPlugin pl3xMapPlugin) {
        this.pl3xMapPlugin = pl3xMapPlugin;
    }

    @Override
    public void shutdown() {

    }

    @Override
    public void save(BufferedImage image, String png, File file) {
        try {
            ImageIO.write(image, png, file);
        } catch (IOException e) {
            Logger.severe("Unable to save image " + file.getName());
        }
    }

    @Override
    public BufferedImage load(String dir, int scaledX, int scaledZ, int zoom) {
        String fileName = scaledX + "_" + scaledZ + ".png";
        File file = new File(dir, fileName);
        if (file.exists()) {
            try {
                return ImageIO.read(file);
            } catch (IIOException e) {
                Logger.warn(Lang.LOG_CORRUPTED_PNG.replace("{png}", fileName), e);
                file.delete();
                return new BufferedImage(Image.SIZE, Image.SIZE, BufferedImage.TYPE_INT_ARGB);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new BufferedImage(Image.SIZE, Image.SIZE, BufferedImage.TYPE_INT_ARGB);
    }
}
