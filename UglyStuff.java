
import java.io.*;
import java.util.*;

public class UglyStuff {

    private static final HashMap<Integer, Integer> expToInt;
    private static final HashMap<Integer, Integer> intToExp;

    static {
        expToInt = new HashMap<>();
        intToExp = new HashMap<>();

        try {
            Scanner s = new Scanner(new File("gf255table.csv"));
            while (s.hasNext()) {
                String[] line = s.nextLine().split(",");
                expToInt.put(Integer.parseInt(line[0]), Integer.parseInt(line[1]));
                if (line.length > 2) {
                    intToExp.put(Integer.parseInt(line[2]), Integer.parseInt(line[3]));
                }
            }
            s.close();
        } catch (FileNotFoundException e) {
            System.out.println(e);
        }
    }

    /**
     * Draws the top left, top right, and bottom left alignment squares, as well
     * as the Timing Strips
     *
     * @param code Your QR Code 2D boolean array
     * @return Your updated 2D boolean array
     */
    public static boolean[][] alignment(boolean[][] code) {
        int size = code.length - 1;

        // outer squares
        for (int i = 0; i <= 6; i++) {
            code[0][i] = true;
            code[i][0] = true;
            code[6][i] = true;
            code[i][6] = true;
            code[size][i] = true;
            code[i][size] = true;
            code[size - 6][i] = true;
            code[i][size - 6] = true;
            code[size - i][0] = true;
            code[0][size - i] = true;
            code[size - i][6] = true;
            code[6][size - i] = true;
        }

        // inner squares
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                code[2 + i][2 + j] = true;
                code[size - 2 - i][2 + j] = true;
                code[2 + i][size - 2 - j] = true;
            }
        }

        // strips
        for (int i = 8; i < size - 7; i += 2) {
            code[6][i] = true;
            code[i][6] = true;
        }

        // Extra dark square
        code[size - 7][8] = true;

        return code;
    }

    /**
     * Draws the fourth alignment square in the bottom right corner
     *
     * @param code Your QR Code 2D boolean array
     * @param version The version/size of your QR Code
     * @return Your updated 2D boolean array
     */
    public static boolean[][] fourthSquare(boolean[][] code, int version) {
        int start = 16 + (4 * (version - 2));
        for (int i = 0; i < 5; i++) {
            code[start + i][start] = true;
            code[start][start + i] = true;
            code[start + i][start + 4] = true;
            code[start + 4][start + i] = true;

        }
        code[start + 2][start + 2] = true;

        return code;
    }

    /**
     * Marks every square on the Marked array that would be affected by the
     * bottom right alignmnet square
     *
     * @param marked Your marked array
     * @param version The version/size of your QR Code
     * @return Your updated marked array
     */
    public static boolean[][] markedFourthSquare(boolean[][] marked, int version) {
        int start = 16 + (4 * (version - 2));
        for (int i = start; i < start + 5; i++) {
            for (int j = start; j < start + 5; j++) {
                marked[i][j] = true;
            }
        }

        return marked;
    }

    /**
     * Marks every square on the Marked array that would be affected by the top
     * right, top left, and bottom left alignment squares, as well as the timing
     * strips
     *
     * @param marked Your marked array
     * @param version The version/size of your QR Code
     * @return Your updated marked array
     */
    public static boolean[][] markedAlignment(boolean[][] marked) {
        int size = marked.length - 1;
        for (int i = 0; i <= 7; i++) {
            for (int j = 0; j <= 7; j++) {
                marked[i][j] = true;
                marked[size - i][j] = true;
                marked[i][size - j] = true;
            }
        }

        // stripes
        for (int i = 8; i < size - 7; i++) {
            marked[6][i] = true;
            marked[i][6] = true;
        }

        for (int i = 0; i <= 8; i++) {
            marked[i][8] = true;
            marked[8][i] = true;
            if (i < 8) {
                marked[size - i][8] = true;
                marked[8][size - i] = true;
            }
        }

        return marked;
    }

    public static int getVersion(int length) {
        if (length <= 14) {
            return 1;
        }
        if (length <= 26) {
            return 2;
        }
        if (length <= 42) {
            return 3;
        }
        if (length <= 62) {
            return 4;
        }
        if (length <= 84) {
            return 5;
        }
        if (length <= 106) {
            return 6;
        }
        return -1;
    }

    public static int getLength(int version) {
        return switch (version) {
            case (1) ->
                10; // TODO Figure out the problem
            case (2) ->
                16;
            case (3) ->
                26;
            case (4) ->
                36;
            case (5) ->
                48;
            case (6) ->
                64;
            default ->
                0;
        };
    }

    public static boolean[][] drawFormatString(boolean[][] code, int mask) {
        int size = code.length - 1;
        boolean[] formatString = bitStringToBoolArray(getFormatString(mask));

        // Top left: 6 _ 3 _ 6
        // Bottom left: 7 Top right: 8
        for (int i = 0; i < 6; i++) {
            code[8][i] = formatString[i];
            code[i][8] = formatString[14 - i];
            code[size - i][8] = formatString[i];
            code[8][size - i] = formatString[14 - i];
        }
        code[8][7] = formatString[6];
        code[8][8] = formatString[7];
        code[7][8] = formatString[8];
        code[size - 6][8] = formatString[6];
        code[8][size - 6] = formatString[8];
        code[8][size - 7] = formatString[7];

        return code;
    }

    private static boolean[] bitStringToBoolArray(String input) {
        boolean[] ret = new boolean[input.length()];
        for (int i = 0; i < input.length(); i++) {
            ret[i] = input.charAt(i) == '1';
        }
        return ret;
    }

    private static String getFormatString(int mask) throws IndexOutOfBoundsException {
        switch (mask) {
            case (0) -> {
                return "101010000010010";
            }
            case (1) -> {
                return "101000100100101";
            }
            case (2) -> {
                return "101111001111100";
            }
            case (3) -> {
                return "101101101001011";
            }
            case (4) -> {
                return "100010111111001";
            }
            case (5) -> {
                return "100000011001110";
            }
            case (6) -> {
                return "100111110010111";
            }
            case (7) -> {
                return "100101010100000";
            }
            default ->
                throw new IndexOutOfBoundsException("Invalid Input: Must Give Mask between 0-7 inclusive");
        }
    }

    private static int[] divisors(int version) {
        return switch (version) {
            case (1) ->
                new int[]{0, 251, 67, 46, 61, 118, 70, 64, 94, 32, 45};
            case (2) ->
                new int[]{0, 120, 104, 107, 109, 102, 161, 76, 3, 91, 191, 147, 169, 182, 194, 225, 120};
            case (3) ->
                new int[]{0, 173, 125, 158, 2, 103, 182, 118, 17, 145, 201, 111, 28, 165, 53, 161, 21, 245, 142, 13, 102, 48, 227, 153, 145, 218, 70};
            case (4) ->
                new int[]{0, 200, 183, 98, 16, 172, 31, 246, 234, 60, 152, 115, 0, 167, 152, 113, 248, 238, 107, 18, 63, 218, 37, 87, 210, 105, 177, 120, 74, 121, 196, 117, 251, 113, 233, 30, 120};
            default ->
                new int[]{};
        };
    }

    private static int goodNumbers(int[] coefficients) {
        int ret = coefficients.length;
        for (int i : coefficients) {
            if (i == 0) {
                ret--;
            } else {
                break;
            }
        }
        return ret;
    }

    public static int[] longDivision(List<Boolean> input, int version) {
        int[] divisor = divisors(version);
        int[] dividend = new int[input.size() / 8 + divisor.length];
        int tracer = 0;
        while (!input.isEmpty()) {
            int count = 0;
            for (int i = 0; i < 8; i++) {
                count += input.remove(0) ? (int) Math.pow(2, 7 - i) : 0;
            }
            dividend[tracer] = count;
            tracer++;
        }

        int term = 0; // tracks which term we're looking at
        while (goodNumbers(dividend) > divisor.length) {
            if (dividend[term] == 0) {
                term++;
                continue;
            }
            int leadExp = intToExp.get(dividend[term]);
            int[] alteredGenPolynomial = new int[divisor.length];
            for (int i = 0; i < divisor.length; i++) {
                alteredGenPolynomial[i] = expToInt.get((divisor[i] + leadExp) % 255);
            }
            for (int i = term; i < term + divisor.length; i++) {
                dividend[i] ^= alteredGenPolynomial[i - term];
            }
            term++;
        }

        int[] ret = new int[divisor.length - 1];
        for (int i = term; i < dividend.length - 1; i++) {
            ret[i - term] = dividend[i];
        }

        return ret;
    }

    // i -> row
    // j -> col
    public static boolean maskPatternEval(int pattern, int row, int col) {
        return switch (pattern) {
            case (0) ->
                (row + col) % 2 == 0;
            case (1) ->
                row % 2 == 0;
            case (2) ->
                col % 3 == 0;
            case (3) ->
                (row + col) % 3 == 0;
            case (4) ->
                (row / 2 + col / 3) % 2 == 0;
            case (5) ->
                ((row * col) % 2) + ((row * col) % 3) == 0;
            case (6) ->
                (((row * col) % 2) + ((row * col) % 3)) % 2 == 0;
            case (7) ->
                (((row + col) % 2) + ((row * col)) % 3) % 2 == 0;
            default ->
                false;
        };
    }


    /*
     * Number of error correction codewords needed by version: 
     * 10 -- x**10 + (a**251)*(x**9) + (a**67)*(x**8) + (a**46)*(x**7) + (a**61)*(x**6) + 
     *                  (a**118)*(x**5) + (a**70)*(x**4) + (a**64)*(x**3) + (a**94)*(x**2) + (a**32)x + a**45
     * 16 -- x**16 + (a**120)*(x**15) + (a**104)*(x**14) + (a**107)*(x**13) + (a**109)*(x**12) + (a**102)*(x**11) + (a**161)*(x**10) + (a**76)*(x**9) +
     *                      (a**3)*(x**8) + (a**91)*(x**7) + (a**191)*(x**6) + (a**147×5) + (a**169)*(x**4) + 
     *                          (a**182)*(x**3) + (a**194)*(x**2) + (a**225)*(x) + (a**120)
     * 3: 26
     * 4: 36
     * 5: 48
     * 6: 64
     */
}
