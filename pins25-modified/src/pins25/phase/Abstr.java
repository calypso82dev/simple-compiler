package pins25.phase;

import java.util.*;

import pins25.common.*;

/**
 * Abstraktna sintaksa.
 */
public class Abstr {

	@SuppressWarnings({ "doclint:missing" })
	public Abstr(SynAn synAn) {
		throw new Report.InternalError();
	}

	/**
	 * Abstraktno sintaksno drevo z dodanimi atributi abstraktne sintakse.
	 * 
	 * Dodani atributi:
	 * <ol>
	 * <li>({@link Abstr}) lokacija kode, ki pripada posameznemu vozliscu.</li>
	 * </ol>
	 */
	public static class AttrAST extends AST.AttrAST {

		/** Atribut: lokacija kode, ki priprada posameznemu vozliscu. */
		public final Map<AST.Node, Report.Locatable> attrLoc;

		/**
		 * Ustvari novo abstraktno sintaksno drevo z dodanimi atributi abstraktne
		 * sintakse.
		 * 
		 * @param attrAST Abstraktno sintaksno drevo.
		 * @param attrLoc Atribut: lokacija kode, ki priprada posameznemu vozliscu.
		 */
		public AttrAST(final AST.AttrAST attrAST, final Map<AST.Node, Report.Locatable> attrLoc) {
			super(attrAST.ast);
			this.attrLoc = attrLoc;
		}

		/**
		 * Ustvari novo abstraktno sintaksno drevo z dodanimi atribuObjectti abstraktne
		 * sintakse.
		 * 
		 * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi abstraktne
		 *                sintakse.
		 */
		public AttrAST(final AttrAST attrAST) {
			super(attrAST.ast);
			this.attrLoc = attrAST.attrLoc;
		}

		@Override
		public String head(final AST.Node node, final boolean highlighted) {
			switch (node) {
			case AST.Nodes<?> nodes:
				return "";
			default:
				final Report.Locatable loc = attrLoc.get(node);
				return (" ") + (loc == null ? "???" : loc.location().toString());
			}
		}

	}

	/**
	 * S klicem sintaksnega analizatorja zgradi abstraktno sintaksno drevo.
	 * 
	 * @param synAn Sintaksni analizator.
	 * @return Abstraktno sintaksno drevo z dodanimi atributi abstraktne sintakse.
	 */
	public static AttrAST constructAST(SynAn synAn) {
		final HashMap<AST.Node, Report.Locatable> attrLoc = new HashMap<AST.Node, Report.Locatable>();
		final AST.Node ast = synAn.parse(attrLoc);
        // Base abstract tree
        final AttrAST abstrAST = new AttrAST(new AST.AttrAST(ast), Collections.unmodifiableMap(attrLoc));
        // Optimize constants
//        final AttrAST optimizedAST = new Optimizer(abstrAST).optimize();
		return abstrAST;
	}

    // Automatic calculate constats BinExpr (10 + 5) -> AtomExpr -> 15
    private static class Optimizer {
        private final AttrAST attrAST;

        public Optimizer(final AttrAST attrAST) { this.attrAST = attrAST; }

        public AttrAST optimize() {
            // Create new AST witj folded constants
            AST.Node optimizedAST = foldConstants(attrAST.ast);
            return new AttrAST(new AST.AttrAST(optimizedAST), attrAST.attrLoc);
        }

        private AST.Node foldConstants(AST.Node ast){
            return ast.accept(new FoldingVisitor(), null);
        }

        private class FoldingVisitor implements AST.FullVisitor<AST.Node, Void> {
            @SuppressWarnings({ "doclint:missing" })
            public FoldingVisitor() {
            }

            // Woud need to rebuild whole tree!!!

            @Override
            public AST.Node visit(AST.BinExpr binExpr, Void arg) {
                // Recursively fold operands first
                AST.Expr fstFolded = (AST.Expr) binExpr.fstExpr.accept(this, arg);
                AST.Expr sndFolded = (AST.Expr) binExpr.sndExpr.accept(this, arg);

                // If first and second expr is AtomExpr - INTCONST, CHARCONST?
                // Calculate and reaplce BinExpr with AtomExpr??

                if (isConstValue(fstFolded) && isConstValue(sndFolded)) {
                    int fstConst = getConstValue(fstFolded);
                    int sndConst = getConstValue(sndFolded);
                    String constVal = String.valueOf(calculateValue(binExpr.oper, fstConst, sndConst));

                    AST.AtomExpr foldedExpr = new AST.AtomExpr(AST.AtomExpr.Type.INTCONST, constVal);
                    return foldedExpr;
                }

                // Return original BinExpr with folded operands (fst, snd)
                return new AST.BinExpr(binExpr.oper, fstFolded, sndFolded);
            }

            private boolean isConstValue(AST.Expr expr) {
                return expr instanceof AST.AtomExpr atomExpr && atomExpr.type == AST.AtomExpr.Type.INTCONST;
            }
            private Integer getConstValue(AST.Expr expr) {
                AST.AtomExpr atomExpr = (AST.AtomExpr)expr;
                return Integer.parseInt(atomExpr.value);
            }
            private Integer calculateValue(AST.BinExpr.Oper oper, int first, int second) {
                return switch(oper) {
                    case OR -> (first != 0 || second != 0) ? 1 : 0;  // Convert boolean to int
                    case AND -> (first != 0 && second != 0) ? 1 : 0; // Convert boolean to int
                    case EQU -> (first == second) ? 1 : 0;           // Convert boolean to int
                    case NEQ -> (first != second) ? 1 : 0;
                    case GTH -> (first > second) ? 1 : 0;
                    case LTH -> (first < second) ? 1 : 0;
                    case GEQ -> (first >= second) ? 1 : 0;
                    case LEQ -> (first <= second) ? 1 : 0;
                    case ADD -> first + second;
                    case SUB -> first - second;
                    case MUL -> first * second;
                    case DIV -> first / second;
                    case MOD -> first % second;
                };
            }
        }
    }

	// --- ZAGON ---

	/**
	 * Zagon gradnje abstraktnega sintaksnega drevesa kot samostojnega programa.
	 * 
	 * @param cmdLineArgs Argumenti v ukazni vrstici.
	 */
	public static void main(final String[] cmdLineArgs) {
		System.out.println("This is PINS'25 compiler (abstract syntax):");

		try {
			if (cmdLineArgs.length == 0)
				throw new Report.Error("No source file specified in the command line.");
			if (cmdLineArgs.length > 1)
				Report.warning("Unused arguments in the command line.");

			try (final SynAn synAn = new SynAn(cmdLineArgs[0])) {
				// abstraktna sintaksa:
				final AttrAST abstrAttrAST = Abstr.constructAST(synAn);

				(new AST.Logger(abstrAttrAST)).log();
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