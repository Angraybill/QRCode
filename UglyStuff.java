
import java.io.*;
import java.util.*;

public class UglyStuff {

    private static final HashMap<Integer, Integer> expToInt;
    private static final HashMap<Integer, Integer> intToExp;

    static {
        expToInt = new HashMap<>();
        intToExp = new HashMap<>();

        try (Scanner s = new Scanner(new File("gf255table.csv"))) {
            while (s.hasNext()) {
                String[] line = s.nextLine().split(",");
                expToInt.put(Integer.valueOf(line[0]), Integer.valueOf(line[1]));
                if (line.length > 2) {
                    intToExp.put(Integer.valueOf(line[2]), Integer.valueOf(line[3]));
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println(e);
        }
    }

    private UglyStuff() {
    }

    /**
     * Draws the top left, top right, and bottom left finder patterns, as well
     * as the Timing Strips
     *
     * @param code Your QR Code 2D boolean array
     */
    protected static void finderPattern(boolean[][] code) {
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

    }

    /**
     * Draws the small alignment squares
     *
     * @param code Your QR Code 2D boolean array
     * @param version The version/size of your QR Code
     */
    protected static void alignmentSquares(boolean[][] code, boolean[][] marked, int version) {
        int[] indicies = getAlignmentIndicies(version);
        // do all combination of each index in indicies except (1,1) (1,-1) (-1,1)
        for (int i = 0; i < indicies.length; i++) {
            for (int j = 0; j < indicies.length; j++) {
                if ((i == 0 || i == indicies.length - 1) && (j == 0 || j == indicies.length - 1) && (i != indicies.length - 1 || j != indicies.length - 1)) {
                    continue;
                }
                int y = indicies[i];
                int x = indicies[j];

                code[y][x] = true;

                for (int k = -2; k <= 2; k++) {
                    for (int l = -2; l <= 2; l++) {
                        marked[y + k][x + l] = true;
                        if (Math.abs(k) + Math.abs(l) < 2 || (Math.abs(k) == 1 && Math.abs(l) == 1)) {
                            continue;
                        }
                        code[y + k][x + l] = true;

                    }
                }
            }
        }

    }

    private static int[] getAlignmentIndicies(int version) {
        if (version == 1) {
            return new int[]{};
        }
        if (version <= 6) {
            return new int[]{6, 10 + (4 * version)};
        }
        if (version <= 13) { // Coming soon, maybe
            return new int[]{6, 8 + (2 * version), 10 + (4 * version)};
        }

        return new int[]{};
    }

    /**
     * Marks every square on the Marked array that would be affected by the top
     * right, top left, and bottom left alignment squares, as well as the timing
     * strips
     *
     * @param marked Your marked array
     * @param version The version/size of your QR Code
     */
    protected static void markedAlignment(boolean[][] marked) {
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

    }

    /**
     * Gives the version for a QR code with a URL of given length given byte
     * mode and medium error correction
     *
     *
     * @param length Length of your URL
     * @return The number of the version of QR Code you should use
     */
    protected static int getVersion(int length) {
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
        if (length <= 122) {
            return 7;
        }
        if (length <= 152) {
            return 8;
        }
        if (length <= 180) {
            return 9;
        }
        if (length <= 213) {
            return 10;
        }
        if (length <= 251) {
            return 11;
        }
        if (length <= 287) {
            return 12;
        }
        if (length <= 331) {
            return 13;
        }
        return -1;
    }

    protected static void drawVersionInformation(boolean[][] code, boolean[][] marked, int version) {
        int size = code.length - 1;
        String versionString = getVersionInformationString(version);
        boolean[] boolArray = bitStringToBoolArray(versionString);
        int stringLength = versionString.length() - 1;

        int trace = 0;
        for (int j = 0; j < 6; j++) {
            for (int i = 10; i > 7; i--) {
                code[size - i][j] = boolArray[stringLength - trace];
                code[j][size - i] = boolArray[stringLength - trace];
                trace++;
            }
        }
        markVersionInformation(marked);
    }

    protected static void markVersionInformation(boolean[][] marked) {
        int size = marked.length - 1;
        for (int j = 0; j < 6; j++) {
            for (int i = 10; i > 7; i--) {
                marked[size - i][j] = true;
                marked[j][size - i] = true;
            }
        }
    }

    private static String getVersionInformationString(int version) {
        return switch (version) {
            case (7) ->
                "000111110010010100";
            case (8) ->
                "001000010110111100";
            case (9) ->
                "001001101010011001";
            case (10) ->
                "001010010011010011";
            case (11) ->
                "001011101111110110";
            case (12) ->
                "001100011101100010";
            case (13) ->
                "001101100001000111";
            default ->
                "";
        };
    }

    /**
     * Draws the format string onto your QR Code array
     *
     * @param code Your QR Code array
     * @param mask The number of mask used as specified by the ISO IEC 18004
     */
    protected static void drawFormatString(boolean[][] code, int mask) {
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

    }

    /**
     * Turns a string of '1's and '0's into a boolean array
     *
     * @param input String of '1's and '0's
     * @return boolean array representation
     */
    private static boolean[] bitStringToBoolArray(String input) {
        boolean[] ret = new boolean[input.length()];
        for (int i = 0; i < input.length(); i++) {
            ret[i] = input.charAt(i) == '1';
        }
        return ret;
    }

    /**
     * Returns block sizes for QR code messages
     *
     * @param version The version number of your QR Code
     * @return An empty boolean array of the correct size for your message
     */
    protected static boolean[][][] initializeBlocks(int version) {
        return switch (version) {
            case (1) ->
                new boolean[1][16][8];
            case (2) ->
                new boolean[1][28][8];
            case (3) ->
                new boolean[1][44][8];
            case (4) ->
                new boolean[2][32][8];
            case (5) ->
                new boolean[2][43][8];
            case (6) ->
                new boolean[4][27][8];
            case (7) ->
                new boolean[4][31][8];
            case (8) ->
                new boolean[][][]{new boolean[38][8], new boolean[38][8], new boolean[39][8], new boolean[39][8]};
            default ->
                new boolean[1][1][1];
        };
    }

    /**
     * Return block sizes for QR code error correction
     *
     * @param version The version number of your QR Code
     * @return An empty boolean array of the correct size for your error
     * corrections
     */
    protected static int[][] initializeRemainderBlocks(int version) {
        return switch (version) {
            case (1) ->
                new int[1][10];
            case (2) ->
                new int[1][16];
            case (3) ->
                new int[1][26];
            case (4) ->
                new int[2][18];
            case (5) ->
                new int[2][24];
            case (6) ->
                new int[4][16];
            case (7) ->
                new int[4][18];
            case (8) ->
                new int[4][22];
            default ->
                new int[1][1];
        };
    }

    /**
     * Return the total number of bytes that a given version can hold within the
     * message
     *
     * @param version The version number of your QR Code
     * @return The total number of bytes your message can hold
     */
    protected static int totBlockWords(int version) {
        boolean[][][] hold = initializeBlocks(version);
        int sum = 0;
        for (boolean[][] b : hold) {
            sum += b.length;
        }
        return sum;
    }

    /**
     * Returns the format string for a given mask (Medium error correction)
     *
     * @param mask The number of your mask
     * @return Your format string as a string of '1's and '0's
     */
    private static String getFormatString(int mask) {
        return switch (mask) {
            case (0) ->
                "101010000010010";

            case (1) ->
                "101000100100101";

            case (2) ->
                "101111001111100";

            case (3) ->
                "101101101001011";

            case (4) ->
                "100010111111001";

            case (5) ->
                "100000011001110";

            case (6) ->
                "100111110010111";

            case (7) ->
                "100101010100000";

            default ->
                "";

        };
    }

    /**
     * Returns the divisors for error correction
     *
     * @param version The version number of your QR Code
     * @return An array of integers used as coefficients for the long division
     * to get the error correction byte. The resulting polynomial, given int[]
     * r, and int a = r.length, is given by: r[0]*x^a + r[1]*x^(a-1) +
     * r[2]*x^(a-2) + ... + r[-1]
     */
    private static int[] divisors(int version) {
        return switch (version) {
            case (1) ->
                new int[]{0, 251, 67, 46, 61, 118, 70, 64, 94, 32, 45};
            case 2, 6 ->
                new int[]{0, 120, 104, 107, 109, 102, 161, 76, 3, 91, 191, 147, 169, 182, 194, 225, 120};
            case (3) ->
                new int[]{0, 173, 125, 158, 2, 103, 182, 118, 17, 145, 201, 111, 28, 165, 53, 161, 21, 245, 142, 13, 102, 48, 227, 153, 145, 218, 70};
            case 4, 7 ->
                new int[]{0, 215, 234, 158, 94, 184, 97, 118, 170, 79, 187, 152, 148, 252, 179, 5, 98, 96, 153};
            case (5) ->
                new int[]{0, 229, 121, 135, 48, 211, 117, 251, 126, 159, 180, 169, 152, 192, 226, 228, 218, 111, 0, 117, 232, 87, 96, 227, 21};
            case (8) ->
                new int[]{0, 210, 171, 247, 242, 93, 230, 14, 109, 221, 53, 200, 74, 8, 172, 98, 80, 219, 134, 160, 105, 165, 231};

            default ->
                new int[]{};
        };
    }

    /**
     * Used for long division, determines how many terms have been eliminated
     * through the process of division.
     *
     * @param coefficients Array of coefficients to the dividend polynomial
     * @return The number of terms in the array after any leading 0s
     */
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

    /**
     * Returns an array of coefficients to a polynomial resulting from dividing
     * a given array of polynomial coefficients by a polynomial as given by the
     * ISO IEC 18004. Polynomials, given int[] r and int a = r.length, are given
     * by r[0]*x^a + r[1]*x^(a-1) + r[2]*x^(a-2) + ... + r[-1]
     *
     * @param input Iterable list of booleans,
     * @param version QR Code version
     * @return Result of the long division
     */
    protected static int[] longDivision(int[] input, int version) {
        int[] divisor = divisors(version);
        int[] dividend = new int[input.length + divisor.length];
        System.arraycopy(input, 0, dividend, 0, input.length);
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

    /**
     * Evaluates if a certain bit should be flipped given position and mask. The
     * ISO IEC 18004 uses ordered pairs (i,j) representing (row, col)
     *
     * @param pattern The number of your mask pattern (0-7)
     * @param row The row index of the given bit
     * @param col The column index of the given bit
     * @return true if the bit should be flipped, false if not
     */
    protected static boolean maskPatternEval(int pattern, int row, int col) {
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
                (((row + col) % 2) + (row * col) % 3) % 2 == 0;
            default ->
                false;
        };
    }

}
