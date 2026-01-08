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
 * ^ (I have used that source for so many projects. I owe geeksforgeeks my life)
 * https://github.com/yansikeim/QR-Code/blob/master/ISO%20IEC%2018004%202015%20Standard.pdf
 */

import java.util.*;

class Generator {

    private final String url;
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

    private static final int MAXLENGTH = 106;

    public Generator(String url) {
        this.url = url;
    }

    /**
     * Main function that does everything needed to create a QR Code
     */
    public void create() {

        length = url.length();
        if (length > MAXLENGTH) {
            smallEnough = false;
            return;
        }
        smallEnough = true;
        version = UglyStuff.getVersion(length);
        blocks = UglyStuff.initializeBlocks(version);
        urlBytes = new boolean[length][8];
        size = 17 + (4 * version);
        codeArray = new boolean[size][size]; // [y][x] starting in top left
        marked = new boolean[size][size];
        up = true;
        lastRow = size - 1;
        lastCol = size - 1;
        numMarked = 0; // avoid double counting
        offset = 0; // avoid messing up the writing when you hit the vertial timing strip
        // Start Drawing things onto the square
        alignmentSquares();
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                if (marked[row][col]) {
                    numMarked++;
                }
            }
        }
        urlBytes = new boolean[UglyStuff.totBlockWords(version)][8];
        writeToBlocks();
        interleave(blocks);
        writeUrl();
        errorCorrection();
        mask();

    }

    /**
     * Draws the alignment squares onto the code
     */
    private void alignmentSquares() {
        UglyStuff.alignment(codeArray); // technically these are position squares
        UglyStuff.markedAlignment(marked);
        if (version > 1) { // these are alignment squares. 
            UglyStuff.fourthSquare(codeArray, version);
            UglyStuff.markedFourthSquare(marked, version);
        }
    }

    /**
     * Writes the URL message (including everything before and after) onto the
     * code
     */
    private void writeUrl() {
        for (boolean[] input : urlBytes) {
            writeNextByte(input);
        }
    }

    /**
     * Converts an integer into a boolean[] byte
     *
     * @param input Integer to convert
     * @return Byte representation
     */
    private boolean[] intToBoolArray(int input) {
        boolean[] ret = new boolean[8];
        for (int tracer = 7; tracer >= 0; tracer--) {
            int check = (int) Math.pow(2, tracer);
            if (input >= check) {
                input -= check;
                ret[tracer] = true;
            }
        }
        return ret;
    }

    /**
     * Turns a byte (representated by a boolean array) into an integer
     *
     * @param input Byte to convert
     * @return Integer representation
     */
    private int boolArrayToint(boolean[] input) {
        int ret = 0;
        for (int i = 0; i < 8; i++) {
            ret += input[i] ? (int) Math.pow(2, i) : 0;
        }
        return ret;
    }

    /**
     * Finds the next square to go to given current position, direction, bit,
     * etc
     */
    private void getNextSquare() {
        while (marked[lastRow][lastCol]) {
            if (lastCol == 6) {
                offset = 1;
                lastCol--;
            }
            if (lastCol % 2 == 0 + offset) {
                lastCol--;
            } else {
                lastCol++;
                lastRow += up ? -1 : 1;
            }
            // too far down
            if (lastRow == size) {
                lastRow--;
                lastCol -= 2;
                up = !up;
            }
            // too far up
            if (lastRow <= -1) {
                lastRow = 0;
                lastCol -= 2;
                up = !up;
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
            codeArray[lastRow][lastCol] = input[i];
            marked[lastRow][lastCol] = true;
            numMarked++;
        }
    }

    /**
     * Draws a format string onto the QR Code
     *
     * @param mask The number of the mask in use
     *
     */
    private void formatString(int mask) {
        UglyStuff.drawFormatString(codeArray, mask);
    }

    /**
     * Calulates the score for a given mask as specified by the ISO IEC 18004
     *
     * @param code A QR Code to grade
     * @return The score as given by the criteria in the handbook
     */
    private int score(boolean[][] code) { // it would appear something in here doesn't work
        int ret = 0;

        // Feature 1: strings of five or more same color
        int counterx = 1;
        int countery = 1;
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                if (col + 1 < size && code[row][col] == code[row][col + 1]) {
                    counterx++;
                } else {
                    if (counterx >= 5) {
                        ret += counterx - 2;
                    }
                    counterx = 1;
                }
                if (col + 1 < size && code[col][row] == code[col + 1][row]) {
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
        for (int row = 0; row < size - 2; row++) {
            for (int col = 0; col < size - 2; col++) {
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
            for (int row = 1; row < size; row++) {
                patternIndex = 0;
                consecutiveEqual = 1;
                lightBefore = false;
                for (int col = 1; col < size; col++) {
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
                                    if (lightBefore || col + 4 < size && nextFourFalse(row, col, codeCheck)) {
                                        ret += 40;
                                    }
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
        int dark = 0;
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                dark += code[row][col] ? 1 : 0;
            }
        }
        double benchmark = (size * size) / 2.0;
        double ratio = dark / benchmark;
        ret += (int) (ratio - 1.0) * 10;

        return ret;
    }

    /**
     * Returns if the next four square are all light Must not give input such
     * that four additional squares would go out of bounds
     *
     * @param row row index of the starting square
     * @param col column index of the starting square
     * @param code QR Code as a 2D boolean array
     * @return true if the next four squares are light. false if not
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
     * Rotates an array 90˚ Counter-Clockwise
     *
     * @param code
     * @return
     */
    private boolean[][] rotate(boolean[][] code) {
        boolean[][] ret = new boolean[code.length][code.length];
        for (int i = 0; i < code.length; i++) {
            for (int j = 0; j < code.length; j++) {
                ret[i][j] = code[j][code.length - i - 1];
            }
        }
        return ret;
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
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
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
        boolean[][] untouched = new boolean[size][size];
        boolean[][] blankTestArray = new boolean[size][size];
        UglyStuff.markedAlignment(untouched);
        UglyStuff.alignment(blankTestArray);
        if (version > 1) {
            UglyStuff.markedFourthSquare(untouched, version);
            UglyStuff.fourthSquare(blankTestArray, version);
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
        codeArray = maskArray(codeArray, lowestPattern, untouched);
        formatString(lowestPattern);
    }

    /**
     * Reverses a boolean[]
     *
     * @param toReverse Input array
     * @return Reversed array
     */
    private boolean[] reverse(boolean[] toReverse) {
        boolean[] ret = new boolean[toReverse.length];
        for (int i = 0; i < toReverse.length; i++) {
            ret[i] = toReverse[toReverse.length - i - 1];
        }
        return ret;
    }

    /**
     * Writes the URL into blocks as specified by the version
     */
    private void writeToBlocks() {
        ArrayList<Boolean> allbits = new ArrayList<>();
        allbits.add(false);
        allbits.add(true);
        allbits.add(false);
        allbits.add(false);
        for (boolean b : reverse(intToBoolArray(length))) {
            allbits.add(b);
        }

        for (int c = 0; c < length; c++) {
            int character = url.charAt(c);
            boolean[] charBoolArray = reverse(intToBoolArray(character));
            for (boolean b : charBoolArray) {
                allbits.add(b);
            }
        }
        for (int i = 0; i < 4; i++) {
            allbits.add(false);
        }
        boolean parity = true;
        while (allbits.size() <= 8 * UglyStuff.totBlockWords(version)) {
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
                blocks[blockNum][numBlocksAdded] = hold;
                numBlocksAdded++;
                hold = new boolean[8];
                holdIndex = 7;
            }
            hold[holdIndex] = allbits.remove(0);
            holdIndex--;

            if (numBlocksAdded == blockLength) {
                blockNum++;
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

        for (int i = 0; i < toInterleave[0].length; i++) { // same number of loops as codewords per block
            for (boolean[][] block : toInterleave) { // loop through each block
                urlBytes[tracer] = block[i];
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

        while (numMarked < Math.pow(size, 2)) {
            writeNextByte(new boolean[]{false});
        }
    }

    /**
     * Adds calculates and writes the error correction bytes onto the code
     */
    private void errorCorrection() {
        int[][] remainders = UglyStuff.initializeRemainderBlocks(version);
        for (int i = 0; i < blocks.length; i++) {
            int[] coefficients = new int[blocks[0].length];
            for (int j = 0; j < coefficients.length; j++) {
                coefficients[j] = boolArrayToint(blocks[i][j]);
            }
            remainders[i] = UglyStuff.longDivision(coefficients, version);
        }
        int[] mergedRemainders = interleaveRemainders(remainders);
        writeErrorCorrection(mergedRemainders);

    }

    // \u001B[47m white 
    // \u001B[0m reset
    // \u001B[42m green
    // \u001B[43m yellow
    // \u001B[44m blue
    /**
     * Turns a boolean[][] into a string that shows a QR code when printed
     *
     * @param toPrint Your boolean[][] representation of a Code
     * @return String representation of a QR Code
     */
    private String printArray(boolean[][] toPrint) {
        final String on = "\u001B[47m";
        final String off = "\u001B[40m";
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
                if (marked[rowIndex][colIndex]) {
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

    @Override
    public String toString() {
        if (!smallEnough) {
            return String.format("That URL is too long. This generator only supports URLs up to %d characters long", MAXLENGTH);
        }
        return printArray(codeArray);
    }

    public static void main(String[] args) {
        try (Scanner s = new Scanner(System.in)) {
            System.out.print("Enter your URL: ");
            Generator g = new Generator(s.nextLine());
            g.create();
            System.out.println(g);
        }
    }
}
