
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class ImageGenerator {

    private String fileName;
    private boolean[][] code;

    private Color light;
    private Color dark;

    private static final int SQUARE_SIZE = 25;

    /**
     * Image generator with colors automatically set to black and white
     *
     * @param fileName Name you ultimately want for your png
     * @param code 2D boolean array representing your QR Code
     */
    public ImageGenerator(String fileName, boolean[][] code) {
        this.fileName = fileName;
        this.code = code;
        this.light = Color.WHITE;
        this.dark = Color.BLACK;
    }

    /**
     *
     * @param fileName Name you ultimately want for your png
     * @param code 2D boolean array representing your QR Code
     * @param light Light color (typically white)
     * @param dark Dark color (typically black)
     */
    public ImageGenerator(String fileName, boolean[][] code, Color light, Color dark) {
        this.fileName = fileName;
        this.code = code;
        this.light = light;
        this.dark = dark;
    }

    /**
     * Generates the QR Code png with the file name and 2D boolean array within
     * the object
     *
     * @throws IOException If the ImageIO cannot write the png. Likely due to an
     * invalid path
     */
    public void drawCode() throws IOException {
        int width = SQUARE_SIZE * (this.code.length + 2);
        int height = width;

        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();

        Rectangle background = new Rectangle(0, 0, width, height);
        g.setColor(this.light);
        g.fill(background);

        Rectangle square = new Rectangle(SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE);

        for (int c = 0; c < this.code.length; c++) {
            for (int r = 0; r < this.code.length; r++) {
                square.setLocation((c + 1) * SQUARE_SIZE, (r + 1) * SQUARE_SIZE);
                g.setColor(this.code[r][c] ? this.dark : this.light);
                g.fill(square);
            }
        }

        ImageIO.write(bi, "png", new File(fileName));
    }

    /**
     * Change the file name to output the png to
     *
     * @param fileName The new file name
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Change the 2D boolean array representing the QR code to be turned into a
     * png
     *
     * @param code The new 2D boolean array representing your QR Code
     */
    public void setCode(boolean[][] code) {
        this.code = code;
    }

    /**
     * Change the light color (typically white)
     *
     * @param light The new light color
     */
    public void setLight(Color light) {
        this.light = light;
    }

    /**
     * Change the dark color (typically black)
     *
     * @param dark The new dark color
     */
    public void setDark(Color dark) {
        this.dark = dark;
    }
}
