package pins25.phase;

import java.io.Console;
import java.util.*;

import pins25.common.*;

/**
 * Izracun pomnilniske predstavitve.
 */
public class Memory {

	@SuppressWarnings({ "doclint:missing" })
	public Memory() {
		throw new Report.InternalError();
	}

	/**
	 * Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
	 * predstavitve.
	 * 
	 * Atributi:
	 * <ol>
	 * <li>({@link Abstr}) lokacija kode, ki pripada posameznemu vozliscu;</li>
	 * <li>({@link SemAn}) definicija uporabljenega imena;</li>
	 * <li>({@link SemAn}) ali je dani izraz levi izraz;</li>
	 * <li>({@link Memory}) klicni zapis funkcije;</li>
	 * <li>({@link Memory}) dostop do parametra;</li>
	 * <li>({@link Memory}) dostop do spremenljivke.</li>
	 * </ol>
	 */
	public static class AttrAST extends SemAn.AttrAST {

		/** Atribut: klicni zapis funkcije. */
		public final Map<AST.FunDef, Mem.Frame> attrFrame;

		/** Atribut: dostop do parametra. */
		public final Map<AST.ParDef, Mem.RelAccess> attrParAccess;

		/** Atribut: dostop do spremenljivke. */
		public final Map<AST.VarDef, Mem.Access> attrVarAccess;

		/**
		 * Ustvari novo abstraktno sintaksno drevo z dodanimi atributi izracuna
		 * pomnilniske predstavitve.
		 * 
		 * @param attrAST       Abstraktno sintaksno drevo z dodanimi atributi
		 *                      semanticne analize.
		 * @param attrFrame     Attribut: klicni zapis funkcije.
		 * @param attrParAccess Attribut: dostop do parametra.
		 * @param attrVarAccess Attribut: dostop do spremenljivke.
		 */
		public AttrAST(final SemAn.AttrAST attrAST, final Map<AST.FunDef, Mem.Frame> attrFrame,
				final Map<AST.ParDef, Mem.RelAccess> attrParAccess, final Map<AST.VarDef, Mem.Access> attrVarAccess) {
			super(attrAST);
			this.attrFrame = attrFrame;
			this.attrParAccess = attrParAccess;
			this.attrVarAccess = attrVarAccess;
		}

		/**
		 * Ustvari novo abstraktno sintaksno drevo z dodanimi atributi izracuna
		 * pomnilniske predstavitve.
		 * 
		 * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi izracuna
		 *                pomnilniske predstavitve.
		 */
		public AttrAST(final AttrAST attrAST) {
			super(attrAST);
			this.attrFrame = attrAST.attrFrame;
			this.attrParAccess = attrAST.attrParAccess;
			this.attrVarAccess = attrAST.attrVarAccess;
		}

		@Override
		public String head(final AST.Node node, final boolean highlighted) {
			final StringBuffer head = new StringBuffer();
			head.append(super.head(node, false));
			head.append(highlighted ? "\033[31m" : "");
			switch (node) {
			case final AST.FunDef funDef:
				Mem.Frame frame = attrFrame.get(funDef);
				head.append(" depth=" + frame.depth);
				head.append(" parsSize=" + frame.parsSize);
				head.append(" varsSize=" + frame.varsSize);
				break;
			case final AST.ParDef parDef: {
				Mem.RelAccess relAccess = attrParAccess.get(parDef);
				head.append(" offset=" + relAccess.offset);
				head.append(" size=" + relAccess.size);
				head.append(" depth=" + relAccess.depth);
				if (relAccess.inits != null)
					initsToString(relAccess.inits, head);
				break;
			}
			case final AST.VarDef varDef: {
				Mem.Access access = attrVarAccess.get(varDef);
				if (access != null)
					switch (access) {
					case final Mem.AbsAccess absAccess:
						head.append(" size=" + absAccess.size);
						if (absAccess.inits != null)
							initsToString(absAccess.inits, head);
						break;
					case final Mem.RelAccess relAccess:
						head.append(" offset=" + relAccess.offset);
						head.append(" size=" + relAccess.size);
						head.append(" depth=" + relAccess.depth);
						if (relAccess.inits != null)
							initsToString(relAccess.inits, head);
						break;
					default:
						throw new Report.InternalError();
					}
				break;
			}
			default:
				break;
			}
			head.append(highlighted ? "\033[30m" : "");
			return head.toString();
		}

		/**
		 * Pripravi znakovno predstavitev zacetne vrednosti spremenmljivke.
		 * 
		 * @param inits Zacetna vrednost spremenljivke.
		 * @param head  Znakovno predstavitev zacetne vrednosti spremenmljivke.
		 */
		private void initsToString(final List<Integer> inits, final StringBuffer head) {
			head.append(" inits=");
			int numPrintedVals = 0;
			int valPtr = 1;
			for (int init = 0; init < inits.get(0); init++) {
				final int num = inits.get(valPtr++);
				final int len = inits.get(valPtr++);
				int oldp = valPtr;
				for (int n = 0; n < num; n++) {
					valPtr = oldp;
					for (int l = 0; l < len; l++) {
						if (numPrintedVals == 10) {
							head.append("...");
							return;
						}
						head.append((numPrintedVals > 0 ? "," : "") + inits.get(valPtr++));
						numPrintedVals++;
					}
				}
			}
		}

	}

	/**
	 * Opravi izracun pomnilniske predstavitve.
	 * 
	 * @param semanAttrAST Abstraktno sintaksno drevo z dodanimi atributi izracuna
	 *                     pomnilniske predstavitve.
	 * @return Abstraktno sintaksno drevo z atributi po fazi pomnilniske
	 *         predstavitve.
	 */
	public static AttrAST organize(SemAn.AttrAST semanAttrAST) {
		AttrAST attrAST = new AttrAST(semanAttrAST, new HashMap<AST.FunDef, Mem.Frame>(),
				new HashMap<AST.ParDef, Mem.RelAccess>(), new HashMap<AST.VarDef, Mem.Access>());
		(new MemoryOrganizer(attrAST)).organize();
		return attrAST;
	}

	/**
	 * Organizator pomnilniske predstavitve.
	 */
	private static class MemoryOrganizer {

		/**
		 * Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
		 * predstavitve.
		 */
		private final AttrAST attrAST;

		/**
		 * Ustvari nov organizator pomnilniske predstavitve.
		 * 
		 * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi izracuna
		 *                pomnilniske predstavitve.
		 */
		public MemoryOrganizer(final AttrAST attrAST) {
			this.attrAST = attrAST;
		}

		/**
		 * Sprozi nov izracun pomnilniske predstavitve.
		 * 
		 * @return Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
		 *         predstavitve.
		 */
		public AttrAST organize() {
			attrAST.ast.accept(new MemoryVisitor(), null);
			return new AttrAST(attrAST, Collections.unmodifiableMap(attrAST.attrFrame),
					Collections.unmodifiableMap(attrAST.attrParAccess),
					Collections.unmodifiableMap(attrAST.attrVarAccess));
		}

		// Context class to pass information between visitor methods
		private static class FrameContext {
			public final int frameDepth;  // Current function's frame depth
			public int varOffset;         // Current offset for variables
			public final List<Mem.RelAccess> vars;  // List to collect variables
			public int totalVarSize;      // Total size of variables
			
			public FrameContext(int frameDepth, int varOffset, List<Mem.RelAccess> vars, int totalVarSize) {
				this.frameDepth = frameDepth;
				this.varOffset = varOffset;
				this.vars = vars;
				this.totalVarSize = totalVarSize;
			}
			// Just for passing depth into nested functins
			public FrameContext(int frameDepth) {
				this.frameDepth = frameDepth;
				this.varOffset = 0;
				this.vars = null;
				this.totalVarSize = 0;
			}
		}

		/** Obiskovalec, ki izracuna pomnilnisko predstavitev. */
		private class MemoryVisitor implements AST.FullVisitor<Object, FrameContext> {

			@SuppressWarnings({ "doclint:missing" })
			public MemoryVisitor() {
			}
	

			// Size of integers and addresses (32 bits = 4 bytes)
			private static final int INT_SIZE = 4; 

			// Visitor method for variable definition
			@Override
			public Object visit(final AST.VarDef varDef, final FrameContext ctx) {
				// Process only top level defs 
				// If ctx == null -> root level
				// Absolute access
				if (ctx == null) {
					Mem.AbsAccess staticVar = new Mem.AbsAccess(
						varDef.name,
						calculateSize(varDef), // number of vlaues * INT_SIZE
						convertInits(varDef.inits)
					);
					attrAST.attrVarAccess.put(varDef, staticVar);
				}
				return null;
			}

			// Visitor method for function definition
			@Override
			public Object visit(final AST.FunDef funDef, final FrameContext ctx) {
				// If depth == null -> root level
				int frameDepth = (ctx == null) ? 1 : ctx.frameDepth;
				
				// Addresses and numbers size of 32 bits (4 Bytes)
				int parOffset = 0; // In Bytes
				List<Mem.RelAccess> pars = new ArrayList<>();
				int totalParSize = 1 * INT_SIZE; // +1 -> Static link

				// Process parameters
				for (AST.ParDef par : funDef.pars) {
					// Create and store the RelAccess
					parOffset += INT_SIZE; // Next address
					Mem.RelAccess localPar = new Mem.RelAccess(
						parOffset,
						frameDepth,
						INT_SIZE,
						null,
						par.name
					);
					attrAST.attrParAccess.put(par, localPar);
					pars.add(localPar);
					totalParSize += INT_SIZE;
				}
				
				// Start after old FramePointer and Return Address value (-8)
				int varOffset = INT_SIZE * -2; 
				List<Mem.RelAccess> vars = new ArrayList<>();
				int totalVarSize = 2 * INT_SIZE; // +2 -> Old FP + RA

				// Create a context object to pass information for processing variables
				FrameContext frameCtx = new FrameContext(frameDepth, varOffset, vars, totalVarSize);
				
				// Process statements - visit all to find let statements
				for (AST.Stmt stmt : funDef.stmts) {
					stmt.accept(this, frameCtx);
				}
				
				// Get updated values from context
				int finalVarSize = frameCtx.totalVarSize;
				List<Mem.RelAccess> finalVars = frameCtx.vars;

				Mem.Frame frame = new Mem.Frame(funDef.name, frameDepth, totalParSize, finalVarSize, pars, finalVars);
				attrAST.attrFrame.put(funDef, frame);

				return null;
			}

			// Visitor method for let statements
			@Override
			public Object visit(final AST.LetStmt letStmt, final FrameContext ctx) {
				// Process definitions
				for (AST.MainDef def : letStmt.defs) {
					switch (def) {
						case AST.VarDef varDef -> {
							// Process var def (relativeAccess)
							int varSize = calculateSize(varDef);
							ctx.varOffset -= varSize;
							Mem.RelAccess localVar = new Mem.RelAccess(
								ctx.varOffset, 
								ctx.frameDepth,
								varSize,
								convertInits(varDef.inits),
								varDef.name
							);
							attrAST.attrVarAccess.put(varDef, localVar);
							ctx.vars.add(localVar);
							ctx.totalVarSize += varSize;
						}
						case AST.FunDef nestedFunDef -> {
							// Process new function (newFrame)
							// Create a new context just for depth 
							FrameContext frameCtx = new FrameContext(ctx.frameDepth + 1);
							nestedFunDef.accept(this, frameCtx);
						}
						default -> {
							// Do nothing
						}
					}
				}
				
				// Process statements inside let (to find nested lets)
				for (AST.Stmt stmt : letStmt.stmts) {
					stmt.accept(this, ctx);
				}
				
				return null;
			}

			private Vector<Integer> convertInits(AST.Nodes<AST.Init> inits) {
				if (inits == null || inits.size() == 0) {
					return null;  // No initializers
				}

				// Create the output vector
				Vector<Integer> result = new Vector<>();
				result.add(0);  // Count of initializers (placeholder)

				int initCount = 0;
				// Process each initializer
				for (AST.Init init : inits) {
					AST.AtomExpr initValue = init.value;
					// Add the number of repetitions
					Integer numOfRepetition = decodeIntConst(init.num, null);

					Report.Locatable loc = attrAST.attrLoc.get(initValue);
					if (numOfRepetition > 0)
					{
						result.add(numOfRepetition);
						initCount++;
						switch (initValue.type) {
							case INTCONST -> {
								// Integer constant - single value
								Integer value = decodeIntConst(initValue, loc);
								result.add(1);  // Length is 1 for an integer
								result.add(value);  // Add the integer value
							}
							case CHRCONST -> {
								// Character constant - single value
								Integer value = decodeChrConst(initValue, loc);
								result.add(1);  // Length is 1 for a character
								result.add(value);  // Add the character value
							}
							case STRCONST -> {
								// String constant - multiple values
								Vector<Integer> values = decodeStrConst(initValue, loc);
								result.add(values.size());  // Length is the string length
								// Add all the character values
								result.addAll(values);  // This adds all elements from the values vector
							}
						}
					}
				}
				// Check if no inits
				if (initCount == 0) {
					return null;
				}
				// Set number of inits
				result.set(0, initCount);
				return result;
			}
			private int calculateSize(AST.VarDef varDef) {
				// If no initializers, assume basic int size
				if (varDef.inits == null || varDef.inits.size() == 0) {
					return INT_SIZE;
				}

				int totalSize = 0;

				for (AST.Init init : varDef.inits) {
					AST.AtomExpr initValue = init.value;

					Integer numOfRepetition = decodeIntConst(init.num, null);
					if (numOfRepetition > 0)
					{
						switch (initValue.type) {
							case INTCONST, CHRCONST -> {
								// For int and char, each element is INT_SIZE
								totalSize += numOfRepetition * INT_SIZE;
							}
							case STRCONST -> {
								// For string, calculate based on characters
								Vector<Integer> chars = decodeStrConst(initValue, null);
								totalSize += numOfRepetition * chars.size() * INT_SIZE;
							}
						}
					}

				}
				// Check if empty var
				if (totalSize == 0) {
					return INT_SIZE;
				}
				return totalSize;
			}
		}
	}

	/**
	 * Izracuna vrednost celostevilske konstante.
	 * 
	 * @param intAtomExpr Celostevilska konstanta.
	 * @param loc         Lokacija celostevilske konstante.
	 * @return Vrednost celostevilske konstante.
	 */
	public static Integer decodeIntConst(final AST.AtomExpr intAtomExpr, final Report.Locatable loc) {
        try {
            String value = intAtomExpr.value;
            if (value.startsWith("0x") || value.startsWith("0X")) {
                return Integer.parseInt(value.substring(2), 16);
            } else if (value.startsWith("0b") || value.startsWith("0B")) {
                return Integer.parseInt(value.substring(2), 2);
            } else if (value.startsWith("0o") || value.startsWith("0O")) {
                return Integer.parseInt(value.substring(2), 8);
            } else {
                return Integer.parseInt(value, 10);
            }
        } catch (NumberFormatException __) {
            throw new Report.Error(loc, "Illegal integer value.");
        }
	}

	/**
	 * Izracuna vrednost znakovna konstante.
	 * 
	 * @param chrAtomExpr Znakovna konstanta.
	 * @param loc         Lokacija znakovne konstante.
	 * @return Vrednost znakovne konstante.
	 */
	public static Integer decodeChrConst(final AST.AtomExpr chrAtomExpr, final Report.Locatable loc) {
        String value = chrAtomExpr.value;
        if (value.charAt(1) == '\\') {
            // Escape sequence
            if (value.charAt(2) == 'n') {
                return 10;
            } else if (value.charAt(2) == '\'') {
                return (int) '\'';
            } else if (value.charAt(2) == '\\') {
                return (int) '\\';
            } else if (value.charAt(2) == 'x') {
                // New format: \x41
                String hexDigits = value.substring(3, 5);
                return Integer.parseInt(hexDigits, 16);
            } else if (value.charAt(2) == 'b') {
                // New format: \b01000001
                String binaryDigits = value.substring(3, 11);
                return Integer.parseInt(binaryDigits, 2);
            } else if (value.charAt(2) == 'o') {
                // New format: \o101
                String octalDigits = value.substring(3, 6);
                return Integer.parseInt(octalDigits, 8);
            } else {
                // Legacy format: \41
                String hexDigits = value.substring(2, 4);
                return Integer.parseInt(hexDigits, 16);
            }
        } else {
            // Regular character
            return (int) value.charAt(1);
        }
	}

	/** ,
	 * Izracuna vrednost konstantnega niza.
	 * 
	 * @param strAtomExpr Konstantni niz.
	 * @param loc         Lokacija konstantnega niza.
	 * @return Vrendnost konstantega niza.
	 */
    public static Vector<Integer> decodeStrConst(final AST.AtomExpr strAtomExpr, final Report.Locatable loc) {
        final Vector<Integer> value = new Vector<Integer>();
        for (int c = 1; c < strAtomExpr.value.length() - 1; c++) {
            switch (strAtomExpr.value.charAt(c)) {
                case '\\':
                    switch (strAtomExpr.value.charAt(c + 1)) {
                        case 'n':
                            value.addLast(10);
                            c += 1;
                            break;
                        case '\"':
                            value.addLast((int) '\"');
                            c += 1;
                            break;
                        case '\\':
                            value.addLast((int) '\\');
                            c += 1;
                            break;
                        case 'x':
                            // New format: \x41
                            String hexDigits = strAtomExpr.value.substring(c + 2, c + 4);
                            value.addLast(Integer.parseInt(hexDigits, 16));
                            c += 3;
                            break;
                        case 'b':
                            // New format: \b01000001
                            String binaryDigits = strAtomExpr.value.substring(c + 2, c + 10);
                            value.addLast(Integer.parseInt(binaryDigits, 2));
                            c += 9;
                            break;
                        case 'o':
                            // New format: \o101
                            String octalDigits = strAtomExpr.value.substring(c + 2, c + 5);
                            value.addLast(Integer.parseInt(octalDigits, 8));
                            c += 4;
                            break;
                        default:
                            // Legacy format: \41
                            value.addLast(16 * (((int) strAtomExpr.value.charAt(c + 1)) - ((int) '0'))
                                    + (((int) strAtomExpr.value.charAt(c + 2)) - ((int) '0')));
                            c += 2;
                            break;
                    }
                    break;
                default:
                    value.addLast((int) strAtomExpr.value.charAt(c));
                    break;
            }
        }
        return value;
    }

	// --- ZAGON ---

	/**
	 * Zagon izracuna pomnilniske predstavitve kot samostojnega programa.
	 * 
	 * @param cmdLineArgs Argumenti v ukazni vrstici.
	 */
	public static void main(final String[] cmdLineArgs) {
		System.out.println("This is PINS'25 compiler (memory):");

		try {
			if (cmdLineArgs.length == 0)
				throw new Report.Error("No source file specified in the command line.");
			if (cmdLineArgs.length > 1)
				Report.warning("Unused arguments in the command line.");

			try (SynAn synAn = new SynAn(cmdLineArgs[0])) {
				// abstraktna sintaksa:
				final Abstr.AttrAST abstrAttrAST = Abstr.constructAST(synAn);
				// semanticna analiza:
				final SemAn.AttrAST semanAttrAST = SemAn.analyze(abstrAttrAST);
				// pomnilniska predstavitev:
				final Memory.AttrAST memoryAttrAST = Memory.organize(semanAttrAST);

				(new AST.Logger(memoryAttrAST)).log();
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
