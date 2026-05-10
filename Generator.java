/*
 * Adam Graybill
 * June 11, 2025
 * QR Code Generator
 * 
 * Sources:
 * https://www.thonky.com/qr-code-tutorial
 * https://www.youtube.com/watch?v=w5ebcowAJD8
 * https://docs.google.com/spreadsheets/d/1kjJSg-Fgdyz4vBqU8iEvL2JYPhMWcL73otAI9s4gj-k/edit?gid=1916601463#gid=1916601463
 * https://www.geeksforgeeks.org/how-to-print-colored-text-in-java-console/
 * https://github.com/yansikeim/QR-Code/blob/master/ISO%20IEC%2018004%202015%20Standard.pdf
 */

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

class Generator {

    private String url;
    private int length;
    private int size;
    private int version;
    private boolean[][] urlBytes;
    private boolean[][] codeArray;
    private boolean[][] marked;
    private int numMarked;
    private boolean up;
    private int lastRow;
    private int lastCol;
    private boolean smallEnough;
    private int offset;
    private boolean[][][] blocks;

    public static final int MAXLENGTH = 331;
    public static final int MAXVERSION = 13;

    private String on = "\u001B[47m";
    private String off = "\u001B[40m";

    /**
     * Construct with a starting URL
     *
     * @param url The url you want to make a QR code of
     */
    public Generator(String url) {
        this.url = url;
    }

    /**
     * Construct without a starting URL
     */
    public Generator() {
    }

    /**
     * Change the URL. You must call create() again to update the code
     * internally
     *
     * @param newUrl
     */
    public void setUrl(String newUrl) {
        this.url = newUrl;
    }

    /**
     * Get a copy of the 2D boolean array representing your QR code
     *
     * @return 2D boolean array representing your QR code
     */
    public boolean[][] getCodeArray() {
        return this.codeArray.clone();
    }

    /**
     * Main function that does everything needed to create a QR Code
     */
    public void create() {
        if (this.url == null) {
            return;
        }
        this.length = this.url.length();
        if (length > MAXLENGTH) {
            this.smallEnough = false;
            return;
        }
        this.smallEnough = true;
        this.version = UglyStuff.getVersion(this.length);
        this.blocks = UglyStuff.initializeBlocks(this.version);
        this.urlBytes = new boolean[this.length][8];
        this.size = 17 + (4 * this.version);
        this.codeArray = new boolean[this.size][this.size]; // [y][x] starting in top left
        this.marked = new boolean[this.size][this.size];
        this.up = true;
        this.lastRow = this.size - 1;
        this.lastCol = this.size - 1;
        this.numMarked = 0; // avoid double counting
        this.offset = 0; // avoid messing up the writing when you hit the vertial timing strip
        // Start Drawing things onto the square
        alignmentSquares();
        if (this.version >= 7) {
            UglyStuff.drawVersionInformation(this.codeArray, this.marked, this.version);
            if (this.version > MAXVERSION) {
                return;
            }
        }

        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                if (this.marked[row][col]) {
                    this.numMarked++;
                }
            }
        }
        this.urlBytes = new boolean[UglyStuff.totBlockWords(this.version)][8];
        writeToBlocks();
        interleave(this.blocks);
        writeUrl();
        errorCorrection();
        mask();
    }

    /**
     * Draws the alignment squares onto the code
     */
    private void alignmentSquares() {
        UglyStuff.finderPattern(this.codeArray); // technically these are position squares
        UglyStuff.markedAlignment(this.marked);
        if (this.version > 1) { // these are alignment squares. 
            UglyStuff.alignmentSquares(this.codeArray, this.marked, this.version);
        }
    }

    /**
     * Writes the URL message (including everything before and after) onto the
     * code
     */
    private void writeUrl() {
        for (boolean[] input : this.urlBytes) {
            writeNextByte(input);
        }
    }

    /**
     * Finds the next square to go to given current position, direction, bit,
     * etc
     */
    private void getNextSquare() {
        while (this.marked[this.lastRow][this.lastCol]) {
            if (this.lastCol == 6) {
                this.offset = 1;
                this.lastCol--;
            }
            if (this.lastCol % 2 == 0 + this.offset) {
                this.lastCol--;
            } else {
                this.lastCol++;
                this.lastRow += this.up ? -1 : 1;
            }
            // too far down
            if (this.lastRow == this.size) {
                this.lastRow--;
                this.lastCol -= 2;
                this.up = !this.up;
            }
            // too far up
            if (this.lastRow <= -1) {
                this.lastRow = 0;
                this.lastCol -= 2;
                this.up = !this.up;
            }
        }
    }

    /**
     * Writes the next byte onto the code
     *
     * @param input Byte to write
     */
    private void writeNextByte(boolean[] input) {
        for (int i = input.length - 1; i >= 0; i--) {
            getNextSquare();
            this.codeArray[this.lastRow][this.lastCol] = input[i];
            this.marked[this.lastRow][this.lastCol] = true;
            this.numMarked++;
        }
    }

    /**
     * Draws a format string onto the QR Code
     *
     * @param mask The number of the mask in use
     *
     */
    private void formatString(int mask) {
        UglyStuff.drawFormatString(this.codeArray, mask);
    }

    /**
     * Calulates the score for a given mask as specified by the ISO IEC 18004
     *
     * @param code A QR Code to grade
     * @return The score as given by the criteria in the handbook
     */
    private int score(boolean[][] code) {
        int ret = 0;

        // Feature 1: strings of five or more same color
        int counterx = 1;
        int countery = 1;
        for (int row = 0; row < this.size; row++) {
            for (int col = 0; col < this.size; col++) {
                if (col + 1 < this.size && code[row][col] == code[row][col + 1]) {
                    counterx++;
                } else {
                    if (counterx >= 5) {
                        ret += counterx - 2;
                    }
                    counterx = 1;
                }
                if (col + 1 < this.size && code[col][row] == code[col + 1][row]) {
                    countery++;
                } else {
                    if (countery >= 5) {
                        ret += countery - 2;
                    }
                    countery = 1;
                }
            }

        }

        // Feature 2: 2x2 squares
        for (int row = 0; row < this.size - 1; row++) {
            for (int col = 0; col < this.size - 1; col++) {
                if (code[row][col] == code[row + 1][col] && code[row][col] == code[row][col + 1] && code[row][col] == code[row + 1][col + 1]) {
                    ret += 3;
                }
            }
        }

        // feature 3: 1:1:3:1:1 (d:l:d:l:d) followed/proceeded by 4 light
        for (boolean[][] codeCheck : new boolean[][][]{code, rotate(code)}) {
            boolean lightBefore;
            int[] patternConsts = new int[]{1, 1, 3, 1, 1};
            int patternIndex;
            int constantMult = 1;
            int consecutiveEqual;
            for (int row = 0; row < this.size; row++) {
                patternIndex = 0;
                consecutiveEqual = 1;
                lightBefore = false;
                for (int col = 1; col < this.size; col++) {
                    if (codeCheck[row][col] == codeCheck[row][col - 1]) {
                        consecutiveEqual++;
                    } else {
                        if (!codeCheck[row][col] && patternIndex == 0) {
                            constantMult = consecutiveEqual;
                            patternIndex++;
                            consecutiveEqual = 1;
                            continue;
                        }
                        if (patternIndex != 0) {
                            if (patternConsts[patternIndex] == constantMult * consecutiveEqual) {
                                patternIndex++;
                                if (patternIndex == 4) {
                                    int scale = 0;
                                    scale += lightBefore ? 1 : 0;
                                    scale += col + 4 < this.size && nextFourFalse(row, col, codeCheck) ? 1 : 0;
                                    ret += scale * 40;

                                    patternIndex = 0;
                                }
                            } else {
                                if (!codeCheck[row][col]) {
                                    constantMult = consecutiveEqual;
                                    patternIndex = 1;
                                    lightBefore = false;
                                } else {
                                    patternIndex = 0;
                                    lightBefore = consecutiveEqual >= 4;
                                }
                            }
                        }
                        consecutiveEqual = 1;
                    }
                }
            }
        }

        // feature 4: Ratio of light to dark squares
        double dark = 0;
        for (int row = 0; row < this.size; row++) {
            for (int col = 0; col < this.size; col++) {
                dark += code[row][col] ? 1 : 0;
            }
        }
        int percentage = (int) ((dark / (this.size * this.size)) * 100);
        percentage = Math.abs(percentage - 50);
        ret += 10 * (percentage / 5);
        return ret;
    }

    /**
     * Returns if the next four square are all light Must not give input such
     * that four additional squares would go out of bounds
     *
     * @param row Row index of the starting square
     * @param col Column index of the starting square
     * @param code QR Code as a 2D boolean array
     * @return True if the next four squares are light. false if not
     */
    private boolean nextFourFalse(int row, int col, boolean[][] code) {
        for (int i = 1; i <= 4; i++) {
            if (code[row][col + i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Draws a mask onto an array
     *
     * @param array Given boolean[][] representation of a QR Code
     * @param pattern Mask number as given by the ISO IEC 18004
     * @param untouched Array of squares not to touch (alignment patters, timing
     * strips, etc)
     * @return Array with the mask drawn on
     */
    private boolean[][] maskArray(boolean[][] array, int pattern, boolean[][] untouched) {
        for (int row = 0; row < this.size; row++) {
            for (int col = 0; col < this.size; col++) {
                if (untouched[row][col]) {
                    continue;
                }
                if (UglyStuff.maskPatternEval(pattern, row, col)) {
                    array[row][col] = !codeArray[row][col];
                } else {
                    array[row][col] = codeArray[row][col];
                }
            }
        }
        return array;
    }

    /**
     * Calulates the mask with the lowest score, implements it onto the code,
     * and adds the format string
     */
    private void mask() {
        boolean[][] untouched = new boolean[this.size][this.size];
        boolean[][] blankTestArray = new boolean[this.size][this.size];
        UglyStuff.markedAlignment(untouched);
        UglyStuff.finderPattern(blankTestArray);
        if (this.version > 1) {
            UglyStuff.alignmentSquares(blankTestArray, untouched, this.version);
        }
        if (this.version >= 7) {
            UglyStuff.markVersionInformation(untouched);
            UglyStuff.drawVersionInformation(blankTestArray, untouched, this.version);
        }
        int[] scores = new int[8];

        for (int pattern = 0; pattern < 8; pattern++) {
            boolean[][] arrayForScoring = maskArray(blankTestArray, pattern, untouched);
            UglyStuff.drawFormatString(arrayForScoring, pattern);
            scores[pattern] = score(arrayForScoring);
        }

        int lowestPattern = 0;
        int lowestScore = Integer.MAX_VALUE;

        for (int i = 0; i < 8; i++) {
            if (scores[i] < lowestScore) {
                lowestScore = scores[i];
                lowestPattern = i;
            }
        }
        this.codeArray = maskArray(this.codeArray, lowestPattern, untouched);
        formatString(lowestPattern);
    }

    /**
     * Writes the URL into blocks as specified by the version
     */
    private void writeToBlocks() {
        ArrayList<Boolean> allbits = new ArrayList<>(Arrays.asList(false, true, false, false)); // byte mode
        for (boolean b : reverse(intToBoolArray(this.length))) {
            allbits.add(b);
        }
        for (int c = 0; c < this.length; c++) {
            int character = this.url.charAt(c);
            boolean[] charBoolArray = reverse(intToBoolArray(character));
            for (boolean b : charBoolArray) {
                allbits.add(b);
            }
        }
        for (int i = 0; i < 4; i++) {
            allbits.add(false);
        }
        boolean parity = true;
        while (allbits.size() <= 8 * UglyStuff.totBlockWords(this.version)) {
            for (boolean b : reverse(intToBoolArray(parity ? 236 : 17))) {
                allbits.add(b);
            }
            parity = !parity;
        }

        boolean[] hold = new boolean[8];
        int blockNum = 0;
        int blockLength = blocks[0].length;
        int numBlocksAdded = 0;
        int holdIndex = 7;
        while (!allbits.isEmpty()) {
            if (holdIndex == -1) {
                this.blocks[blockNum][numBlocksAdded] = hold;
                numBlocksAdded++;
                hold = new boolean[8];
                holdIndex = 7;
            }
            hold[holdIndex] = allbits.removeFirst();
            holdIndex--;

            if (numBlocksAdded == blockLength) {
                blockNum++;
                if (blockNum < blocks.length) {
                    blockLength = this.blocks[blockNum].length;
                }
                numBlocksAdded = 0;
            }

        }
    }

    /**
     * Interleaves bytes
     *
     * @param toInterleave Blocks of boolean[] representations of bytes
     */
    private void interleave(boolean[][][] toInterleave) {
        int tracer = 0;
        for (int i = 0; i < toInterleave[toInterleave.length - 1].length; i++) { // same number of loops as codewords per block
            for (boolean[][] block : toInterleave) { // loop through each block
                if (i >= block.length) {
                    continue;
                }
                this.urlBytes[tracer] = block[i];
                tracer++;
            }
        }
    }

    /**
     * Interleaves the remainders one byte from each block after another
     *
     * @param remainders Coefficient remainders broken into blocks as
     * specificied by the version
     * @return Remainders interwoven into one stream of integers
     */
    private int[] interleaveRemainders(int[][] remainders) {
        int[] ret = new int[remainders.length * remainders[0].length];
        int tracer = 0;
        for (int i = 0; i < remainders[0].length; i++) {
            for (int[] remainder : remainders) {
                ret[tracer] = remainder[i];
                tracer++;
            }
        }
        return ret;
    }

    /**
     * Writes the error correction bits onto the Code
     *
     * @param remainder Integer coefficients of the remainder as given by the
     * polynomial division
     */
    private void writeErrorCorrection(int[] remainder) {
        for (int i = 0; i < remainder.length; i++) {
            int input = remainder[i];
            if (input == -1) {
                continue;
            }
            boolean[] booleanRepresentation = intToBoolArray(input);
            writeNextByte(booleanRepresentation);
        }

        while (numMarked < this.size * this.size) {
            writeNextByte(new boolean[]{false});
        }
    }

    /**
     * Adds calculates and writes the error correction bytes onto the code
     */
    private void errorCorrection() {
        int[][] remainders = UglyStuff.initializeRemainderBlocks(this.version);
        for (int i = 0; i < this.blocks.length; i++) {
            int[] coefficients = new int[this.blocks[i].length];
            for (int j = 0; j < coefficients.length; j++) {
                coefficients[j] = boolArrayToint(this.blocks[i][j]);
            }
            remainders[i] = UglyStuff.longDivisionRemainders(coefficients, this.version);
        }
        int[] mergedRemainders = interleaveRemainders(remainders);
        writeErrorCorrection(mergedRemainders);
    }

    // \u001B[0m reset
    // \u001B[40m black
    // \u001B[41m red
    // \u001B[42m green
    // \u001B[43m yellow
    // \u001B[44m blue
    // \u001B[45m purple
    // \u001B[46m cyan
    // \u001B[47m white 
    /**
     * Change the color of either the on or off. Options are red, yellow, green,
     * blue, cyan, purple, black, and white
     *
     * @param light True if you want the light color (typically white) to
     * change. False if you want the dark color (typically black) to change
     * @param color The color you want to change to
     */
    public void changeColor(boolean light, String color) {
        if (color == null) {
            return;
        }
        String colorCode = switch (color.toLowerCase()) {
            case ("black") ->
                "\u001B[40m";
            case ("red") ->
                "\u001B[41m";
            case ("green") ->
                "\u001B[42m";
            case ("yellow") ->
                "\u001B[43m";
            case ("blue") ->
                "\u001B[44m";
            case ("purple") ->
                "\u001B[45m";
            case ("cyan") ->
                "\u001B[46m";
            case ("white") ->
                "\u001B[47m";
            default ->
                null;
        };
        if (colorCode == null) {
            return;
        }
        if (light) {
            this.on = colorCode;
        } else {
            this.off = colorCode;
        }
    }

    /**
     * Turns a boolean[][] into a string that shows a QR code when printed
     *
     * @param toPrint Your boolean[][] representation of a QR Code
     * @return String representation of a QR Code
     */
    protected String printArray(boolean[][] toPrint) {
        return Output.printArray(toPrint, this.marked, on, off);
    }

    // Static Methods:
    /**
     * Rotates an array 90˚ Counter-Clockwise
     *
     * @param code
     * @return
     */
    public static boolean[][] rotate(boolean[][] code) {
        boolean[][] ret = new boolean[code[0].length][code.length];
        for (int i = 0; i < code.length; i++) {
            for (int j = 0; j < code[0].length; j++) {
                ret[i][j] = code[j][code.length - i - 1];
            }
        }
        return ret;
    }

    /**
     * Reverses a boolean[]
     *
     * @param toReverse Input array
     * @return Reversed array
     */
    public static boolean[] reverse(boolean[] toReverse) {
        boolean[] ret = new boolean[toReverse.length];
        for (int i = 0; i < toReverse.length; i++) {
            ret[i] = toReverse[toReverse.length - i - 1];
        }
        return ret;
    }

    /**
     * Turns a byte (representated by a boolean array) into an integer
     *
     * @param input Byte to convert
     * @return Integer representation
     */
    public static int boolArrayToint(boolean[] input) {
        int ret = 0;
        for (int i = 0; i < 8; i++) {
            ret += input[i] ? (int) Math.pow(2, i) : 0;
        }
        return ret;
    }

    /**
     * Converts an integer into a boolean[] byte
     *
     * @param input Integer to convert
     * @return Byte representation
     */
    public static boolean[] intToBoolArray(int input) {
        boolean[] ret;
        int start;
        if (input > 255) {
            ret = new boolean[16];
            start = 15;
        } else {
            ret = new boolean[8];
            start = 7;
        }
        for (int tracer = start; tracer >= 0; tracer--) {
            int check = (int) Math.pow(2, tracer);
            if (input >= check) {
                input -= check;
                ret[tracer] = true;
            }
        }
        return ret;
    }

    @Override
    public String toString() {
        if (!smallEnough) {
            return String.format("That URL is too long. This generator only supports URLs up to %d characters long", MAXLENGTH);
        }
        return printArray(this.codeArray);
    }

    public static void main(String[] args) throws IOException {
        String inputUrl;
        try (Scanner s = new Scanner(System.in)) {
            if (args.length > 0) {
                inputUrl = args[0];
            } else {
                System.out.print("Enter your URL: ");
                inputUrl = s.nextLine();

            }

            Generator g = new Generator(inputUrl);
            g.create();

            if (args.length <= 1 || args[1].equalsIgnoreCase("print")) {
                System.out.println(g);
            } else {
                boolean givenFileName = args.length >= 3;
                String fileName;
                if (givenFileName) {
                    fileName = args[2];
                } else {
                    System.out.print("Enter a filename: ");
                    fileName = s.nextLine();
                }
                if (args[1].equalsIgnoreCase("csv")) {
                    Output.convertToCSV(g.getCodeArray(), Path.of(""), fileName);
                } else if (args[1].equalsIgnoreCase("png")) {
                    Output.convertToPNG(g.getCodeArray(), Path.of(""), fileName);
                } else {
                    System.out.println("Unknown output instruction");
                }
            }
        }
    }
}
