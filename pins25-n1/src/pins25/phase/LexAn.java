package pins25.phase;

import java.io.*;
import java.util.*;
import pins25.common.*;

/**
 * Leksikalni analizator z izboljšano strukturo.
 */
public class LexAn implements AutoCloseable {

    /**
     * Izvorna datoteka.
     */
    private final Reader srcFile;

    public LexAn(final String srcFileName) {
        try {
            srcFile = new BufferedReader(new InputStreamReader(new FileInputStream(new File(srcFileName))));
            nextChar();
        } catch (FileNotFoundException __) {
            throw new Report.Error("Source file '" + srcFileName + "' not found.");
        }
    }

    @Override
    public void close() {
        try {
            srcFile.close();
        } catch (IOException __) {
            throw new Report.Error("Cannot close source file.");
        }
    }

    // Konstante za posebne znake
    private static final int EOF = -1;
    private static final int NOT_READ = -2;
    private static final char SINGLE_QUOTE = '\'';
    private static final char DOUBLE_QUOTE = '"';
    private static final char BACKSLASH = '\\';
    private static final char NEWLINE = '\n';
    private static final char CARRIAGE_RETURN = '\r';
    private static final char TAB = '\t';
    private static final char SPACE = ' ';

    // Mapa ključnih besed
    private static final Map<String, Token.Symbol> KEYWORDS = Map.ofEntries(
            Map.entry("fun", Token.Symbol.FUN),
            Map.entry("var", Token.Symbol.VAR),
            Map.entry("if", Token.Symbol.IF),
            Map.entry("then", Token.Symbol.THEN),
            Map.entry("else", Token.Symbol.ELSE),
            Map.entry("while", Token.Symbol.WHILE),
            Map.entry("do", Token.Symbol.DO),
            Map.entry("let", Token.Symbol.LET),
            Map.entry("in", Token.Symbol.IN),
            Map.entry("end", Token.Symbol.END)
    );

    /** Trenutni znak izvorne datoteke (glej {@link #nextChar}). */
    private int buffChar = -2;

    /** Vrstica trenutnega znaka izvorne datoteke (glej {@link #nextChar}). */
    private int buffCharLine = 0;

    /** Stolpec trenutnega znaka izvorne datoteke (glej {@link #nextChar}). */
    private int buffCharColumn = 0;

    /**
     * Prebere naslednji znak izvorne datoteke.
     *
     * Izvorno datoteko beremo znak po znak. Trenutni znak izvorne datoteke je
     * shranjen v spremenljivki {@link #buffChar}, vrstica in stolpec trenutnega
     * znaka izvorne datoteke sta shranjena v spremenljivkah {@link #buffCharLine} in
     * {@link #buffCharColumn}.
     *
     * Začetne vrednosti {@link #buffChar}, {@link #buffCharLine} in
     * {@link #buffCharColumn} so {@code '\n'}, {@code 0} in {@code 0}: branje prvega
     * znaka izvorne datoteke bo na osnovi vrednosti {@code '\n'} spremenljivke
     * {@link #buffChar} prvemu znaku izvorne datoteke priredilo vrstico 1 in stolpec
     * 1.
     *
     * Pri branju izvorne datoteke se predpostavlja, da je v spremenljivki
     * {@link #buffChar} ves čas veljaven znak. Zunaj metode {@link #nextChar} so vse
     * spremenljivke {@link #buffChar}, {@link #buffCharLine} in
     * {@link #buffCharColumn} namenjene le branju.
     *
     * Vrednost {@code -1} v spremenljivki {@link #buffChar} pomeni konec datoteke
     * (vrednosti spremenljivk {@link #buffCharLine} in {@link #buffCharColumn} pa
     * nista več veljavni).
     */

    private void nextChar() {
        try {
            switch (buffChar) {
                case NOT_READ: // Nothing has been read yet
                    buffChar = srcFile.read();
                    // Start -> First line (1, 1)
                    // EOF -> Last char (0, 0)
                    buffCharLine = buffChar == EOF ? 0 : 1;
                    buffCharColumn = buffChar == EOF ? 0 : 1;
                    return;
                case EOF: // End of file  (-1)
                    return;
                case NEWLINE: // Previous character was new line
                    buffChar = srcFile.read();
                    // Line += 1
                    buffCharLine = buffChar == EOF ? buffCharLine : buffCharLine + 1;
                    // Column = 1 (first chracter)
                    buffCharColumn = buffChar == EOF ? buffCharColumn : 1;
                    return;
                case TAB: // Previous character was tab
                    buffChar = srcFile.read();
                    // Alight by 4
                    while (buffCharColumn % 4 != 0)
                        buffCharColumn += 1;
                    buffCharColumn += 1;
                    return;
                default:
                    buffChar = srcFile.read();
                    // Reading char in line
                    buffCharColumn += 1;
            }
        } catch (IOException __) {
            throw new Report.Error("Cannot read source file.");
        }
    }

    /**
     * Trenutni leksikalni simbol.
     *
     * "Ce vrednost spremenljivke {@code buffToken} ni {@code null}, je simbol "ze
     * prebran iz vhodne datoteke, ni pa "se predan naprej sintaksnemu analizatorju.
     * Ta simbol je dostopen z metodama {@link #peekToken} in {@link #takeToken}.
     */
    private Token buffToken = null;

    // buffer trenutnega niza znakov -> Token
    // if null, v bufferju ni znakov (new token)
    //private String textBuffer = null;

    /**
     * Prebere naslednji leksikalni simbol, ki je nato dostopen preko metod
     * {@link #peekToken} in {@link #takeToken}.
     */

    private void nextToken() {
        // Whitesapce skip
        skipWhitespace();

        // End of file
        if (buffChar == EOF) {
            buffToken = new Token(new Report.Location(0, 0), Token.Symbol.EOF, "");
            return;
        }

        int startLine = buffCharLine;
        int startCol = buffCharColumn;
        StringBuilder lexeme = new StringBuilder();

        // Constants
        // starts with num [+-]1-9 -> int
        // starts wtih ` -> character
        // starts with " -> string

        // Preverimo različne vrste tokenov
        if (isDigit(buffChar)) {
            // 1. INT CONST
            buffToken = readNumber(startLine, startCol, lexeme);
        } else if (buffChar == SINGLE_QUOTE) {
            // 2. CHRACTER CONST
            buffToken = readCharConstant(startLine, startCol, lexeme);
        } else if (buffChar == DOUBLE_QUOTE) {
            // 3. STRING CONST
            buffToken = readStringConstant(startLine, startCol, lexeme);
        } else if (isCommentStart(buffChar)) {
            // Comment
            int next = peekNextChar();
            if (next == '/')
                skipSingleLineComment();
            else if (next == '*')
                skipMultiLineComment();
            // Process next token
            nextToken();
        } else if (isIdentifierStart(buffChar)) {
            // Name Identifier OR Keyword
            buffToken = readIdentifier(startLine, startCol, lexeme);
        } else {
            // Operators
            buffToken = readOperator(startLine, startCol, lexeme);
        }
    }

    private Token readNumber(int startLine, int startCol, StringBuilder lexeme) {
        // Read all digits
        while (isDigit(buffChar)) {
            lexeme.append((char) buffChar);
            nextChar();
        }

        String number = lexeme.toString();

        // Check for leading zero (numbers like 01, 02 are illegal)
        if (number.length() > 1 && number.charAt(0) == '0') {
            throw new Report.Error(
                    new Report.Location(startLine, startCol),
                    "Illegal leading zero in number: " + number
            );
        }

        return createToken(startLine, startCol, lexeme, Token.Symbol.INTCONST);
    }

    private Token readCharConstant(int startLine, int startCol, StringBuilder lexeme) {
        // Start or character - single quote
        lexeme.append((char) buffChar); // '
        nextChar();

        // Process body of chracter
        readCharacterContent(lexeme, startLine, startCol, false);

        // Closing of character - single quote
        if (buffChar != SINGLE_QUOTE) {
            throw new Report.Error(new Report.Location(startLine, startCol),
                    "Unterminated string constant"
            );
        }

        lexeme.append((char) buffChar);
        nextChar();

        return createToken(startLine, startCol, lexeme, Token.Symbol.CHARCONST);
    }

    private Token readStringConstant(int startLine, int startCol, StringBuilder lexeme) {
        // Start of string - double quote
        lexeme.append((char) buffChar); // "
        nextChar();

        // Process body of string
        while (buffChar != DOUBLE_QUOTE && buffChar != EOF && !isEndOfLine(buffChar)) {
            readCharacterContent(lexeme, startLine, startCol, true);
        }

        if (buffChar != DOUBLE_QUOTE) {
            throw new Report.Error(new Report.Location(startLine, startCol),
                    "Unterminated string constant"
            );
        }

        lexeme.append((char) buffChar);
        nextChar();

        return createToken(startLine, startCol, lexeme, Token.Symbol.STRINGCONST);
    }
    private void readCharacterContent(StringBuilder lexeme, int startLine, int startCol, boolean isString) {
        // 1. Normal characters (ASCII 32-126) - [\x20-\x7E]
        // 2. Escape sequences: \\, \n, \" (string), \' (char)
        // 3. Hex escape sequences: \xx where x is 0-9, a-f (lowercase only) - \\[0-9a-f]

        String context = isString ? "string" : "character";
        // Char is backslash - special chracter
        if (buffChar == BACKSLASH) {
            lexeme.append((char) buffChar);
            nextChar();

            if (buffChar == BACKSLASH || buffChar == 'n') {
                // Valid in both (string and char): \\, \n
                lexeme.append((char) buffChar);
                nextChar();
            } else if (buffChar == SINGLE_QUOTE && !isString) {
                // \' is only valid in character constants
                lexeme.append((char) buffChar);
                nextChar();
            } else if (buffChar == DOUBLE_QUOTE && isString) {
                // \" is only valid in string constants
                lexeme.append((char) buffChar);
                nextChar();
            } else if (isHexDigit(buffChar)) {
                // Hex escape: \xx (valid in both)

                // 1. Hex digit
                lexeme.append((char) buffChar);
                nextChar();
                if (!isHexDigit(buffChar)) {
                    // Invalid Hex character
                    throw new Report.Error(new Report.Location(startLine, startCol),
                            "Invalid hex escape sequence - expected two hex digits"
                    );
                }
                // 2. Hex digit
                lexeme.append((char) buffChar);
                nextChar();
            } else {
                // Invalid Specail (esacpe sequence) character
                throw new Report.Error(new Report.Location(startLine, startCol),
                        "Invalid escape sequence in " + context + " constant: \\" + (char)buffChar
                );
            }
        } else if (buffChar >= 32 && buffChar <= 126) {
            // Regular ASCII characters
            lexeme.append((char) buffChar);
            nextChar();
        } else {
            // Invalid ASCII chracter
            throw new Report.Error(new Report.Location(startLine, startCol),
                    "Invalid character in " + context + " constant"
            );
        }
    }

    private Token readIdentifier(int startLine, int startCol, StringBuilder lexeme) {
        while (isIdentifierPart(buffChar)) {
            lexeme.append((char) buffChar);
            nextChar();
        }

        String identifier = lexeme.toString();

        // Check if identifier is keyword or name
        Token.Symbol symbol = KEYWORDS.getOrDefault(identifier, Token.Symbol.IDENTIFIER);

        return createToken(startLine, startCol, lexeme, symbol);
    }

    private Token readOperator(int startLine, int startCol, StringBuilder lexeme) {
        char firstChar = (char) buffChar;
        lexeme.append(firstChar);
        // Second char (current)
        nextChar();

        Token.Symbol symbol = switch (firstChar) {
            // 1. Single/Double operators
            case '=' -> {
                // Could be ASSIGN or EQU
                if (buffChar == '=') {
                    lexeme.append((char) buffChar);
                    nextChar();
                    yield Token.Symbol.EQU;
                }
                yield Token.Symbol.ASSIGN;
            }
            case '!' -> {
                // Could be NOT or NEQ (!=)
                if (buffChar == '=') {
                    lexeme.append((char) buffChar);
                    nextChar();
                    yield Token.Symbol.NEQ;
                }
                yield Token.Symbol.NOT;
            }
            case '>' -> {
                // Could be GTH or GEQ (>=)
                if (buffChar == '=') {
                    lexeme.append((char) buffChar);
                    nextChar();
                    yield Token.Symbol.GEQ;
                }
                yield Token.Symbol.GTH;
            }
            case '<' -> {
                // Could be LTH or LEQ (<=)
                if (buffChar == '=') {
                    lexeme.append((char) buffChar);
                    nextChar();
                    yield Token.Symbol.LEQ;
                }
                yield Token.Symbol.LTH;
            }
            // 2. Double operators
            case '&' -> {
                // Looking for AND (&&)
                if (buffChar == '&') {
                    lexeme.append((char) buffChar);
                    nextChar();
                    yield Token.Symbol.AND;
                }
                throw new Report.Error(new Report.Location(startLine, startCol),
                        "Expected && for logical AND"
                );
            }
            case '|' -> {
                // Looking for OR (||)
                if (buffChar == '|') {
                    lexeme.append((char) buffChar);
                    nextChar();
                    yield Token.Symbol.OR;
                }
                throw new Report.Error(new Report.Location(startLine, startCol),
                        "Expected || for logical OR"
                );
            }
            // 3. Single operators
            case ',' -> Token.Symbol.COMMA;
            case '+' -> Token.Symbol.ADD;
            case '-' -> Token.Symbol.SUB;
            case '*' -> Token.Symbol.MUL;
            case '/' -> Token.Symbol.DIV;
            case '%' -> Token.Symbol.MOD;
            case '^' -> Token.Symbol.PTR;
            case '(' -> Token.Symbol.LPAREN;
            case ')' -> Token.Symbol.RPAREN;
            default -> throw new Report.Error("Unknown character: " + firstChar);
        };

        return createToken(startLine, startCol, lexeme, symbol);
    }

    private void skipWhitespace() {
        while (buffChar == SPACE || buffChar == TAB || buffChar == NEWLINE || buffChar == CARRIAGE_RETURN) {
            nextChar();
        }
    }

    private void skipSingleLineComment() {
        // Skip //
        nextChar(); // 1. /
        nextChar(); // 2. /

        while (buffChar != EOF && !isEndOfLine(buffChar)) {
            nextChar();
        }

        if (isEndOfLine(buffChar)) {
            nextChar(); // Skip newline
        }
    }
    private void skipMultiLineComment() {
        // Skip /*
        nextChar(); // 1. /
        nextChar(); // 2. *

        // Read until we find */
        while (buffChar != EOF) {
            if (buffChar == '*' && peekNextChar() == '/') {
                nextChar(); // Skip *
                nextChar(); // Skip /
                return;
            }
            nextChar(); // Skip current character
        }

        // If we reach here, we hit EOF without finding */
        throw new Report.Error("Unterminated multi-line comment");
    }

    private int peekNextChar() {
        try {
            srcFile.mark(1);  // Mark current position in file
            int next = srcFile.read();      // Read next char (advances file)
            srcFile.reset();                // Reset file back to marked position
            return next;
        } catch (IOException __) {
            return EOF;
        }
    }

    private Token createToken(int startLine, int startCol, StringBuilder lexeme, Token.Symbol symbol) {
        String lexemeStr = lexeme.toString();
        Report.Location location = new Report.Location(
                startLine, startCol,
                startLine, startCol + lexemeStr.length() - 1
        );
        return new Token(location, symbol, lexemeStr);
    }

    // Helper metode
    private boolean isEndOfLine(int character) {
        return character == NEWLINE || character == CARRIAGE_RETURN;
    }

    private boolean isCommentStart(int character) {
        int next = peekNextChar();
        return character == '/' && (next == '/' || next == '*');
    }

    private boolean isIdentifierStart(int character) {
        return isLetter(character) || character == '_';
    }

    private boolean isIdentifierPart(int character) {
        return isLetter(character) || isDigit(character) || character == '_';
    }

    private boolean isHexDigit(int character) {
        return isDigit(character) || (character >= 'a' && character <= 'f') || (character >= 'A' && character <= 'F');
    }

    private boolean isLetter(int character) {
        return (character >= 'A' && character <= 'Z') || (character >= 'a' && character <= 'z');
    }

    private boolean isDigit(int character) {
        return character >= '0' && character <= '9';
    }

    /**
     * Vrne trenutni leksikalni simbol, ki ostane v lastnistvu leksikalnega
     * analizatorja.
     *
     * @return Leksikalni simbol.
     */
    public Token peekToken() {
        if (buffToken == null)
            nextToken();
        return buffToken;
    }

    /**
     * Vrne trenutni leksikalni simbol, ki preide v lastnistvo klicoce kode.
     *
     * @return Leksikalni simbol.
     */
    public Token takeToken() {
        if (buffToken == null)
            nextToken();
        final Token thisToken = buffToken;
        buffToken = null;
        return thisToken;
    }

    // --- ZAGON ---

    /**
     * Zagon leksikalnega analizatorja kot samostojnega programa.
     *
     * @param cmdLineArgs Argumenti v ukazni vrstici.
     */
    public static void main(final String[] cmdLineArgs) {
        System.out.println("This is PINS'25 compiler (lexical analysis):");

        try {
            if (cmdLineArgs.length == 0)
                throw new Report.Error("No source file specified in the command line.");
            if (cmdLineArgs.length > 1)
                Report.warning("Unused arguments in the command line.");

            try (LexAn lexAn = new LexAn(cmdLineArgs[0])) {
                while (lexAn.peekToken().symbol() != Token.Symbol.EOF)
                    System.out.println(lexAn.takeToken());
                System.out.println(lexAn.takeToken()); // EOF token
            }

            // Upajmo, da kdaj pridemo to te tocke.
            // A zavedajmo se sledecega:
            // 1. Prevod je zaradi napak v programu lahko napacen :-o
            // 2. Izvorni program se zdalec ni tisto, kar je programer hotel, da bi bil ;-)

            Report.info("Done.");
        } catch (Report.Error error) {
            System.err.println(error.getMessage());
            System.exit(1);
        }
    }
}