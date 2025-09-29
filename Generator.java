/*
 * Adam Graybill
 * June 11, 2025
 * QR Code Generator
 * 
 * Sources:
 * https://www.thonky.com/qr-code-tutorial/format-version-tables
 * https://www.thonky.com/qr-code-tutorial/log-antilog-table
 * https://www.thonky.com/qr-code-tutorial/show-division-steps?
 * https://www.youtube.com/watch?v=w5ebcowAJD8
 * https://docs.google.com/spreadsheets/d/1kjJSg-Fgdyz4vBqU8iEvL2JYPhMWcL73otAI9s4gj-k/edit?gid=1916601463#gid=1916601463
 * https://www.geeksforgeeks.org/how-to-print-colored-text-in-java-console/
 * ^ (I have used that source for so many projects. I owe geeksforgeeks my life)
 * https://github.com/yansikeim/QR-Code/blob/master/ISO%20IEC%2018004%202015%20Standard.pdf
 */

import java.util.ArrayList;
import java.util.Scanner;

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
    private ArrayList<Boolean> written;
    private int offset;

    public Generator(String url) {
        this.url = url;
        written = new ArrayList<>();
    }

    public void create() {
        length = url.length();
        if (length > 106) {
            smallEnough = false;
            return;
        }
        smallEnough = true;
        version = UglyStuff.getVersion(length);
        urlBytes = new boolean[length][8];
        size = 17 + (4 * version);
        codeArray = new boolean[size][size]; // [y][x] starting in top left
        marked = new boolean[size][size];
        up = true;
        lastRow = size - 1;
        lastCol = size - 1;
        numMarked = -4; // avoid double counting
        offset = 0; // avoid messing up the writing when you hit the vertial timing strip

        writeNextByte(new boolean[]{false, false, true, false}); // bytes but the code works backwards

        // Start Drawing things onto the square
        alignmentSquares();
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                if (marked[row][col]) {
                    numMarked++;
                }
            }
        }
        urlLength();
        writeUrl();
        //  System.out.println(this);
        pad();
        //  System.out.println(this);
        errorCorrection(); // The problem is somewhere in here
        //  System.out.println(this + "\n\n\n\n\n\n\n\n\n\n\n");
        easyMask();
        // formatString(2); // 2 is just what veritasium used because it's simple
    }

    public void setUrl(String url) {
        this.url = url;
    }

    private void alignmentSquares() {
        codeArray = UglyStuff.alignment(codeArray);
        marked = UglyStuff.markedAlignment(marked);
        if (version > 1) {
            codeArray = UglyStuff.fourthSquare(codeArray, version);
            marked = UglyStuff.markedFourthSquare(marked, version);
        }
    }

    private void pad() {
        boolean parity = true;
        while ((int) Math.pow(size, 2) - (8 * (UglyStuff.getLength(version) + 1)) - 4 > numMarked) {
            writeNextByte(intToBoolArray(parity ? 236 : 17));
            parity = !parity;
        }
        // pad with alternating: 11101100 (0xEC) 00010001 (0x11)
    }

    private void urlLength() {
        boolean[] urlLengthBytes = intToBoolArray(length);
        writeNextByte(urlLengthBytes);
    }

    private void writeUrl() {
        for (int c = 0; c < length; c++) {
            int character = url.charAt(c);
            urlBytes[c] = intToBoolArray(character);
        }
        for (boolean[] input : urlBytes) {
            writeNextByte(input);
        }
        writeNextByte(new boolean[]{false, false, false, false});
    }

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

    private void getNextSquare() {
        while (marked[lastRow][lastCol]) {
            if (lastCol == 6) {
                offset = 1;
                lastCol--;
            }
            // traverse
            /*
            if (lastCol == 0) {
                lastRow += up ? -1 : 1;
                continue;
            }*/
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

    private void writeNextByte(boolean[] input) {
        Scanner s = new Scanner(System.in);
        for (int i = input.length - 1; i >= 0; i--) {
            // System.out.println(this);
            // s.nextLine();
            getNextSquare();
            codeArray[lastRow][lastCol] = input[i];
            written.add(input[i]);
            marked[lastRow][lastCol] = true;
            numMarked++;
        }
    }

    public boolean[][] getArray() {
        return codeArray;
    }

    public boolean[][] getMarked() {
        return marked;
    }

    public String getUrl() {
        return url;
    }

    private void formatString(int mask) {
        codeArray = UglyStuff.drawFormatString(codeArray, mask);
    }

    private int score(boolean[][] code) {
        int ret = 0;

        // Feature 1: strings of five or more same color
        int counterx = 1;
        int countery = 1;
        for (int row = 0; row < size; row++) {
            for (int col = 1; col < size; col++) {
                if (code[row][col] == code[row][col + 1]) {
                    counterx++;
                } else {
                    if (counterx >= 5) {
                        ret += counterx - 2;
                        counterx = 1;
                    }
                }
                if (code[col][row] == code[col][row + 1]) {
                    countery++;
                } else {
                    if (countery >= 5) {
                        ret += countery - 2;
                        countery = 1;
                    }
                }
            }

        }
        // Feature 2: 2x2 squares
        for (int row = 1; row < size; row++) {
            for (int col = 1; col < size; col++) {
                if (code[row][col] == code[row + 1][col] && code[row][col] == code[row][col + 1] && code[row][col] == code[row + 1][col + 1]) {
                    ret += 3;
                }
            }
        }
        // feature 3: 1:1:3:1:1 followed/proceeded by 4 whites
        return ret;
    }

    private boolean[][] maskArray(boolean[][] array, int pattern, boolean[][] untouched) {
        boolean[][] test = new boolean[size][size];
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                if (untouched[row][col]) {
                    continue;
                }
                if (UglyStuff.maskPatternEval(pattern, row, col)) {
                    array[row][col] = !codeArray[row][col];
                    test[row][col] = true;
                } else {
                    array[row][col] = codeArray[row][col];
                }
            }
        }
        //www.instagram.com/angraybillSystem.out.println(printArray(test));
        return array;
    }

    private void easyMask() {
        boolean[][] untouched = new boolean[size][size];
        untouched = UglyStuff.markedAlignment(untouched);
        if (version > 1) {
            untouched = UglyStuff.markedFourthSquare(untouched, version);
        }
        codeArray = maskArray(codeArray, 2, untouched);
        formatString(2);
    }

    private void mask() {
        boolean[][] untouched = new boolean[size][size];
        untouched = UglyStuff.markedAlignment(untouched);
        if (version > 1) {
            untouched = UglyStuff.markedFourthSquare(untouched, version);
        }
        int implementedMasks = 8;
        int[] scores = new int[implementedMasks];
        for (int pattern = 0; pattern < implementedMasks; pattern++) {
            boolean[][] arrayForScoring = maskArray(new boolean[size][size], pattern, untouched);
            scores[pattern] = score(arrayForScoring);
        }
        int lowestPattern = 0;
        int lowestScore = Integer.MAX_VALUE;

        for (int i = 0; i < implementedMasks; i++) {
            if (scores[i] < lowestScore) {
                lowestScore = scores[i];
                lowestPattern = i;
            }
        }
        codeArray = maskArray(codeArray, lowestPattern, untouched);
        formatString(lowestPattern);
        // TODO make this actually variable
        // TODO: implement masks using the scoring system given here: Page 54
        // https://github.com/yansikeim/QR-Code/blob/master/ISO%20IEC%2018004%202015%20Standard.pdf
    }

    ;

    private void errorCorrection() {
        int[] remainder = UglyStuff.longDivision(written, version);
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

    // \u001B[47m white 
    // \u001B[0m reset
    // \u001B[42m green
    // \u001B[43m yellow
    // \u001B[44m blue
    public String printArray(boolean[][] toPrint) {
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
            return "That URL is too long. This generator only supports URLs up to 106 characters long";
        }
        return printArray(codeArray);
    }

    public static void main(String[] args) {
        Scanner s = new Scanner(System.in);
        System.out.print("Enter your URL: ");
        Generator g = new Generator(s.nextLine());
        g.create();
        System.out.println(g);
        s.close();
    }
}
