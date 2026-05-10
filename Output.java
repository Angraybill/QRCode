
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;

public class Output {

    private Output() {
    }

    /**
     * Turns a boolean[][] into a string that shows a QR code when printed
     *
     * @param toPrint Your square boolean[][] representation of a QR Code
     * @param markedArray A square boolean[][] of the same size with every
     * position of the toPrint array you want printed marked as true
     * @return String represenation of a QR Code
     */
    public static String printArray(boolean[][] toPrint, boolean[][] markedArray, final String on, final String off) {
        if (toPrint.length != markedArray.length || toPrint[0].length != markedArray[0].length) {
            return "Invalid markedArray; Both arrays must be of the same size";
        }
        final String reset = "\u001B[0m";
        String ret = "";
        String blank = on + "  " + reset;

        for (int i = 0; i < toPrint.length + 2; i++) {
            ret += blank;
        }
        ret += "\n";

        int rowIndex = 0;
        for (boolean[] row : toPrint) {
            ret += blank;
            int colIndex = 0;
            for (boolean square : row) {
                String color = "";
                if (markedArray[rowIndex][colIndex]) {
                    color = square ? off : on;
                }
                ret += color + "  " + reset;
                colIndex++;
            }
            ret += blank + "\n";
            rowIndex++;
        }
        for (int i = 0; i < toPrint.length + 2; i++) {
            ret += blank;
        }
        return ret;
    }

    /**
     * Creates a PNG of your QR Code
     *
     * @param code Boolean array representation of your QR Code
     * @param filePath Relative filepath from the directory you run this program
     * in
     * @param name The name you want for the png. If you don't end the name with
     * ".png", it will append it automatically
     * @throws IOException Consequence of calling a python program internally
     */
    public static void convertToPNG(boolean[][] code, Path filePath, String name) throws IOException {
        String path = System.getProperty("user.dir") + filePath.toString();
        if (path.charAt(path.length() - 1) != '/') {
            path += "/";
        }
        path += name;
        path += name.substring(name.length() - 4).equalsIgnoreCase(".png") ? "" : ".png";
        System.out.println(path);
        ImageGenerator ig = new ImageGenerator(path, code);
        ig.drawCode();
    }

    public static void convertToCSV(boolean[][] code, Path filePath, String name) throws IOException {
        String newFilePath = "/newCode" + name + (name.substring(name.length() - 4).equalsIgnoreCase(".csv") ? "" : ".csv");
        File newFile = new File(System.getProperty("user.dir") + filePath.toString(), newFilePath);
        try (FileWriter newFileWriter = new FileWriter(newFile)) {
            for (boolean[] line : code) {
                String thisLine = "";
                for (boolean b : line) {
                    thisLine += (b ? '1' : '0') + ",";
                }
                newFileWriter.write(thisLine + '\n');
            }
        }
    }
}
