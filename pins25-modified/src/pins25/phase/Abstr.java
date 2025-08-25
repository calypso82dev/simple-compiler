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
		return abstrAST;
	}
    public static void analyzeAST(Abstr.AttrAST attrAST) {
        // Pass abstract tree
        ASTAnalyzer astAnalyzer = new ASTAnalyzer(attrAST);
        astAnalyzer.analyze();
    }


    private static class ASTAnalyzer {
        private final AttrAST attrAST;

        public ASTAnalyzer(final AttrAST attrAST) { this.attrAST = attrAST; }

        public void analyze() {
            ASTAnalyzerVisitor analyzerVisitor = new ASTAnalyzerVisitor();
            System.out.println("Running AST Analysis...");
            Map<Class<? extends AST.Node>, Integer> nodeCount = analyzerVisitor.countNodes();

            int totalNodes = 0;
            // Print nodes
            for (Map.Entry<Class<? extends AST.Node>, Integer> entry : nodeCount.entrySet()) {
                System.out.println(entry.getKey().getSimpleName() + " -> " + entry.getValue());
                totalNodes += entry.getValue();
            }
            System.out.println("All nodes: " + totalNodes);


        }

        private class ASTAnalyzerVisitor implements AST.FullVisitor<Void, Void> {
            @SuppressWarnings({ "doclint:missing" })
            public ASTAnalyzerVisitor() {
            }

            private final Map<Class<? extends AST.Node>, Integer> nodeCount = new HashMap<>();

            private Map<Class<? extends AST.Node>, Integer> countNodes() {
                // Count Nodes of ast
                attrAST.ast.accept(this, null);
                return nodeCount;
            }

            private void count(AST.Node node) {
                int val = nodeCount.getOrDefault(node.getClass(), 0);
                nodeCount.put(node.getClass(), val + 1);
            }

            /* Definitions */
            @Override
            public Void visit(AST.FunDef funDef, Void arg) {
                count(funDef);
                // visit parameters
                for (AST.ParDef par : funDef.pars) {
                    par.accept(this, arg);
                }
                // visit statements
                for (AST.Stmt stmt : funDef.stmts) {
                    stmt.accept(this, arg);
                }
                return null;
            }
            @Override
            public Void visit(AST.ParDef parDef, Void arg) {
                count(parDef); // leaf node, nothing nested
                return null;
            }

            @Override
            public Void visit(AST.VarDef varDef, Void arg) {
                count(varDef);
                for (AST.Init init : varDef.inits) {
                    init.accept(this, arg);
                }
                return null;
            }

            @Override
            public Void visit(AST.Init init, Void arg) {
                count(init);
                if (init.num != null) init.num.accept(this, arg);
                if (init.value != null) init.value.accept(this, arg);
                return null;
            }

            /* Statements */
            @Override
            public Void visit(AST.ExprStmt stmt, Void arg) {
                count(stmt);
                stmt.expr.accept(this, arg);
                return null;
            }

            @Override
            public Void visit(AST.AssignStmt stmt, Void arg) {
                count(stmt);
                stmt.dstExpr.accept(this, arg);
                stmt.srcExpr.accept(this, arg);
                return null;
            }

            @Override
            public Void visit(AST.IfStmt stmt, Void arg) {
                count(stmt);
                stmt.cond.accept(this, arg);
                for (AST.Stmt s : stmt.thenStmts) s.accept(this, arg);
                for (AST.Stmt s : stmt.elseStmts) s.accept(this, arg);
                return null;
            }

            @Override
            public Void visit(AST.WhileStmt stmt, Void arg) {
                count(stmt);
                stmt.cond.accept(this, arg);
                for (AST.Stmt s : stmt.stmts) s.accept(this, arg);
                return null;
            }

            @Override
            public Void visit(AST.LetStmt stmt, Void arg) {
                count(stmt);
                for (AST.MainDef def : stmt.defs) def.accept(this, arg);
                for (AST.Stmt s : stmt.stmts) s.accept(this, arg);
                return null;
            }

            /* Expressions */
            @Override
            public Void visit(AST.AtomExpr expr, Void arg) {
                count(expr); // leaf node, nothing nested
                return null;
            }

            @Override
            public Void visit(AST.UnExpr expr, Void arg) {
                count(expr);
                expr.expr.accept(this, arg);
                return null;
            }

            @Override
            public Void visit(AST.BinExpr expr, Void arg) {
                count(expr);
                expr.fstExpr.accept(this, arg);
                expr.sndExpr.accept(this, arg);
                return null;
            }

            @Override
            public Void visit(AST.VarExpr expr, Void arg) {
                count(expr); // leaf node, nothing nested
                return null;
            }

            @Override
            public Void visit(AST.CallExpr expr, Void arg) {
                count(expr);
                for (AST.Expr a : expr.args) a.accept(this, arg);
                return null;
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

                // Run ASTAnlysis
                System.out.println();
                Abstr.analyzeAST(abstrAttrAST);
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