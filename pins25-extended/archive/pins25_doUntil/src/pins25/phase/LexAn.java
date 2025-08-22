package pins25.phase;

import java.io.*;
import pins25.common.*;

/**
 * Leksikalni analizator.
 */
public class LexAn implements AutoCloseable {

	/** Izvorna datoteka. */
	private final Reader srcFile;

	/**
	 * Ustvari nov leksikalni analizator.
	 * 
	 * @param srcFileName Ime izvorne datoteke.
	 */
	public LexAn(final String srcFileName) {
		try {
			srcFile = new BufferedReader(new InputStreamReader(new FileInputStream(new File(srcFileName))));
			nextChar(); // Pripravi prvi znak izvorne datoteke (glej {@link nextChar}).
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

	/** Trenutni znak izvorne datoteke (glej {@link nextChar}). */
	private int buffChar = -2;

	/** Vrstica trenutnega znaka izvorne datoteke (glej {@link nextChar}). */
	private int buffCharLine = 0;

	/** Stolpec trenutnega znaka izvorne datoteke (glej {@link nextChar}). */
	private int buffCharColumn = 0;

	/**
	 * Prebere naslednji znak izvorne datoteke.
	 * 
	 * Izvorno datoteko beremo znak po znak. Trenutni znak izvorne datoteke je
	 * shranjen v spremenljivki {@link buffChar}, vrstica in stolpec trenutnega
	 * znaka izvorne datoteke sta shranjena v spremenljivkah {@link buffCharLine} in
	 * {@link buffCharColumn}.
	 * 
	 * Zacetne vrednosti {@link buffChar}, {@link buffCharLine} in
	 * {@link buffCharColumn} so {@code '\n'}, {@code 0} in {@code 0}: branje prvega
	 * znaka izvorne datoteke bo na osnovi vrednosti {@code '\n'} spremenljivke
	 * {@link buffChar} prvemu znaku izvorne datoteke priredilo vrstico 1 in stolpec
	 * 1.
	 * 
	 * Pri branju izvorne datoteke se predpostavlja, da je v spremenljivki
	 * {@link buffChar} ves "cas veljaven znak. Zunaj metode {@link nextChar} so vse
	 * spremenljivke {@link buffChar}, {@link buffCharLine} in
	 * {@link buffCharColumn} namenjene le branju.
	 * 
	 * Vrednost {@code -1} v spremenljivki {@link buffChar} pomeni konec datoteke
	 * (vrednosti spremenljivk {@link buffCharLine} in {@link buffCharColumn} pa
	 * nista ve"c veljavni).
	 */

	private void nextChar() {
		try {
			switch (buffChar) {
			case -2: // Noben znak "se ni bil prebran.
				buffChar = srcFile.read();
				buffCharLine = buffChar == -1 ? 0 : 1; // buffChar == -1 -> EOF
				buffCharColumn = buffChar == -1 ? 0 : 1;
				return;
			case -1: // Konec datoteke je bil ze viden.
				return;
			case '\n': // Prejsnji znak je koncal vrstico, zacne se nova vrstica.
				buffChar = srcFile.read();
				buffCharLine = buffChar == -1 ? buffCharLine : buffCharLine + 1;
				buffCharColumn = buffChar == -1 ? buffCharColumn : 1;
				return;
			case '\t': // Prejsnji znak je tabulator, ta znak je morda potisnjen v desno.
				buffChar = srcFile.read();
				while (buffCharColumn % 4 != 0)
					buffCharColumn += 1;
				buffCharColumn += 1;
				return;
			default: // Prejsnji znak je brez posebnosti.
				buffChar = srcFile.read();
				buffCharColumn += 1;
				return;
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
	 * Ta simbol je dostopen z metodama {@link peekToken} in {@link takeToken}.
	 */
	private Token buffToken = null;

	// buffer trenutnega niza znakov -> Token
	// if null, v bufferju ni znakov (new token)
	//private String textBuffer = null;

	/**
	 * Prebere naslednji leksikalni simbol, ki je nato dostopen preko metod
	 * {@link peekToken} in {@link takeToken}.
	 */
	private void nextToken() {
		//System.out.println("Char: " + buffChar);
		StringBuilder sb = new StringBuilder();

		Token.Symbol symbol = null;
		Report.Location location;

		int startLine = buffCharLine;
		int startCol = buffCharColumn;
		int startChar = buffChar;
		sb.append((char) startChar);

		boolean isComment = false;

		// Constants
		// starts with num [+-]1-9 -> int 
		// starts wtih ` -> character
		// starts with " -> string

		// INTCONST
		if (isDigit(startChar)) {
			nextChar();
			
			while (isDigit(buffChar)) {
				sb.append((char) buffChar);
				nextChar();
			}

			// Check leading zero
			if (sb.length() > 1 && sb.charAt(0) == '0') {
				throw new Report.Error(
					new Report.Location(startLine, startCol),
					"Illegal prefix 0"
				);
			}

			symbol = Token.Symbol.INTCONST;
		}

		// CHARCONST ''
		if (startChar == 39) { 
			nextChar();
			sb.append((char) buffChar);

			if (buffChar == 92) {
				// Special char \
				nextChar();
				sb.append((char) buffChar);

				if (buffChar == 92 || buffChar == 39 || buffChar == 'n') {
					// \\ \' \n
				} else if (validHexChar(buffChar)) {
					// Hex character
					nextChar();
					sb.append((char) buffChar);

					if (!validHexChar(buffChar)) {
						throw new Report.Error(
							new Report.Location(startLine, startCol, buffCharLine, buffCharColumn),
							"Hex character unvalid: " + sb.toString()
						);
					}
				} else {
					throw new Report.Error(
						new Report.Location(startLine, startCol, buffCharLine, buffCharColumn),
						"Special character unvalid: " + sb.toString()
					);
				}
			} else if (buffChar >= 32 && buffChar <= 126) {
				// Normal character from {32...126}
			} else {
				throw new Report.Error(
					new Report.Location(startLine, startCol, buffCharLine, buffCharColumn),
					"Character unvalid: " + sb.toString()
				);
			}
			nextChar();
			sb.append((char) buffChar);

			// Closing '
			if (buffChar != 39) {
				throw new Report.Error(
					new Report.Location(startLine, startCol, buffCharLine, buffCharColumn),
					"Unfinished character: " + sb.toString()
				);
			}

			// Ready next char for new token
			nextChar();

			symbol = Token.Symbol.CHARCONST;
		}

		// STRINGCONST ""
		if (startChar == 34) { 
			nextChar();
			
			// 1. Normal characters (ASCII 32-126) - [\x20-\x7E]
			// 2. Escape sequences: \", \\, \n
			// 3. Hex escape sequences: \xx where x is 0-9, a-f (lowercase only) - \\[0-9a-f]

			while (buffChar != 34 && !isEndOfLine(buffChar) && buffChar != -1) {
				// Read characters 1 by 1 unil end of string or end of file (error)
				sb.append((char) buffChar);

				if (buffChar == 92) {
					// Special char \
					nextChar();
					sb.append((char) buffChar);

					if (buffChar == 92 || buffChar == 34 || buffChar == 'n') {
						// \\ \' \n
					} else if (validHexChar(buffChar)) {
						// Hex character
						nextChar();
						sb.append((char) buffChar);
						
						if (!validHexChar(buffChar)) {
							throw new Report.Error(
								new Report.Location(startLine, startCol, buffCharLine, buffCharColumn),
								"Hex character unvalid: " + sb.toString()
							);
						}
					} else {
						throw new Report.Error(
							new Report.Location(startLine, startCol, buffCharLine, buffCharColumn),
							"Special character unvalid: " + sb.toString()
						);
					}
				} else if (buffChar >= 32 && buffChar <= 126) {
					// Normal character from {32...126}
				} else {
					throw new Report.Error(
						new Report.Location(startLine, startCol, buffCharLine, buffCharColumn),
						"String unvalid: " + sb.toString()
					);
				}

				nextChar();
			}

			// Closing "
			if (buffChar != 34) {
				throw new Report.Error(
					new Report.Location(startLine, startCol, buffCharLine, buffCharColumn),
					"Unfinished string: " + sb.toString()
				);
			}
			
			sb.append((char) buffChar);

			// Ready next char for new token
			nextChar();

			symbol = Token.Symbol.STRINGCONST;
		}

		// Symbols
		switch (startChar) {
			case '=':
				// Could be ASSIGN or EQU
				nextChar();
				if (buffChar == '=') {
					sb.append((char) buffChar);
					symbol = Token.Symbol.EQU;
					nextChar();
				} else {
					symbol = Token.Symbol.ASSIGN;
				}
				break;
				
			case ',':
				symbol = Token.Symbol.COMMA;
				nextChar();
				break;
				
			case '&':
				// Looking for AND (&&)
				nextChar();
				if (buffChar == '&') {
					sb.append((char) buffChar);
					symbol = Token.Symbol.AND;
					nextChar();
				} else {
					throw new Report.Error(
						new Report.Location(startLine, startCol),
						"Operator unvalid: " + (char) startChar
					);
				}
				break;
				
			case '|':
				// Looking for OR (||)
				nextChar();
				if (buffChar == '|') {
					sb.append((char) buffChar);
					symbol = Token.Symbol.OR;
					nextChar();
				} else {
					throw new Report.Error(
						new Report.Location(startLine, startCol),
						"Operator unvalid: " + (char) startChar
					);
				}
				break;
				
			case '!':
				// Could be NOT or NEQ (!=)
				nextChar();
				if (buffChar == '=') {
					sb.append((char) buffChar);
					symbol = Token.Symbol.NEQ;
					nextChar();
				} else {
					symbol = Token.Symbol.NOT;
				}
				break;
				
			case '>':
				// Could be GTH or GEQ (>=)
				nextChar();
				if (buffChar == '=') {
					sb.append((char) buffChar);
					symbol = Token.Symbol.GEQ;
					nextChar();
				} else {
					symbol = Token.Symbol.GTH;
				}
				break;
				
			case '<':
				// Could be LTH or LEQ (<=)
				nextChar();
				if (buffChar == '=') {
					sb.append((char) buffChar);
					symbol = Token.Symbol.LEQ;
					nextChar();
				} else {
					symbol = Token.Symbol.LTH;
				}
				break;
				
			case '+':
				symbol = Token.Symbol.ADD;
				nextChar();
				break;
				
			case '-':
				symbol = Token.Symbol.SUB;
				nextChar();
				break;
				
			case '*':
				symbol = Token.Symbol.MUL;
				nextChar();
				break;
				
			case '/':
				// Could be comment
				nextChar();
				if (buffChar == '/') {
					isComment = true;
					//System.out.println("Comment");
				} else {
					symbol = Token.Symbol.DIV;
				}
				break;
				
			case '%':
				symbol = Token.Symbol.MOD;
				nextChar();
				break;
				
			case '^':
				symbol = Token.Symbol.PTR;
				nextChar();
				break;
				
			case '(':
				symbol = Token.Symbol.LPAREN;
				nextChar();
				break;
				
			case ')':
				symbol = Token.Symbol.RPAREN;
				nextChar();
				break;
		}
		

		// Comment
		if (isComment) {
			// Read until new line
			nextChar();
			while (!isEndOfLine(buffChar)) {
				nextChar();
			}
			nextChar();
			nextToken();
			return;
		}

		// Identifiers
		// Starts with A-Za-z_, continue with A-Za-z0-9_
		// Check if name if key word

		if (validIdChar(startChar) && !isDigit(startChar)) {
			nextChar();

			while (validIdChar(buffChar)) {
				// Read characters 1 by 1 unil end of identifier valid chars
				sb.append((char) buffChar);
				nextChar();
			}

			// Check if identifier is key word
			String keyword = sb.toString();
			symbol = switch (keyword) {
				case "fun" -> Token.Symbol.FUN;
				case "var" -> Token.Symbol.VAR;
				case "if" -> Token.Symbol.IF;
				case "then" -> Token.Symbol.THEN;
				case "else" -> Token.Symbol.ELSE;
				case "while" -> Token.Symbol.WHILE;
				case "do" -> Token.Symbol.DO;
				case "let" -> Token.Symbol.LET;
				case "in" -> Token.Symbol.IN;
				case "end" -> Token.Symbol.END;
				case "break" -> Token.Symbol.BREAK;     // NEW
    			case "until" -> Token.Symbol.UNTIL;     // NEW
				default -> Token.Symbol.IDENTIFIER;
			};
		}

		// Whitespaces
		if (startChar == ' ' || startChar == '\t' || startChar == '\n' || startChar == '\r') {
            // Skip space, tab, new line, carrige return
            nextChar();
			nextToken();
			return;
        }

		String lexeme = sb.toString();
		location = new Report.Location(startLine, startCol, startLine, startCol + lexeme.length() - 1);

		// System.out.println("String: " + lexeme);
		// System.out.println("Length: " + (lexeme.length() - 1));

		// EOF
		if (startChar == -1) {
			location = new Report.Location(0, 0);
			symbol = Token.Symbol.EOF;
		}

		// System.out.println(symbol);
		// System.out.println(location);
		
		if (symbol != null) {
			// Create token 
			buffToken = new Token(location, symbol, lexeme);
		} else {
			throw new Report.Error(
				new Report.Location(startLine, startCol),
				"Undefined character: " + (char) startChar
			);
		}
	}

	private boolean isEndOfLine(int character) {
		if (character == '\n') {
			return true;
		}	
		// handling CR+LF sequence (Windows style)
		if (character == '\r') {
			nextChar(); // Consume the \n after \r
			return true;
		}
		return false;
	}

	private boolean validIdChar(int character) {
		return isDigit(character) || isLetter(character) || character == '_';
	}

	private boolean validHexChar(int character) {
		return isDigit(character) || (character >= 'a' && character <= 'f');
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
			// Izpis opisa napake.
			System.err.println(error.getMessage());
			System.exit(1);
		}
	}

}