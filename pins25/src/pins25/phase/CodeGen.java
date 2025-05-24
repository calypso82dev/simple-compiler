package pins25.phase;

import java.util.*;

import pins25.common.*;

/**
 * Generiranje kode.
 */
public class CodeGen {

	@SuppressWarnings({ "doclint:missing" })
	public CodeGen() {
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
	 * <li>({@link Memory}) dostop do spremenljivke;</li>
	 * <li>({@link CodeGen}) seznam ukazov, ki predstavljajo kodo programa;</li>
	 * <li>({@link CodeGen}) seznam ukazov, ki predstavljajo podatke programa.</li>
	 * </ol>
	 */
	public static class AttrAST extends Memory.AttrAST {

		/** Atribut: seznam ukazov, ki predstavljajo kodo programa. */
		public final Map<AST.Node, List<PDM.CodeInstr>> attrCode;

		/** Atribut: seznam ukazov, ki predstavljajo podatke programa. */
		public final Map<AST.Node, List<PDM.DataInstr>> attrData;

		/**
		 * Ustvari novo abstraktno sintaksno drevo z dodanimi atributi generiranja kode.
		 * 
		 * @param attrAST  Abstraktno sintaksno drevo z dodanimi atributi pomnilniske
		 *                 predstavitve.
		 * @param attrCode Attribut: seznam ukazov, ki predstavljajo kodo programa.
		 * @param attrData Attribut: seznam ukazov, ki predstavljajo podatke programa.
		 */
		public AttrAST(final Memory.AttrAST attrAST, final Map<AST.Node, List<PDM.CodeInstr>> attrCode,
				final Map<AST.Node, List<PDM.DataInstr>> attrData) {
			super(attrAST);
			this.attrCode = attrCode;
			this.attrData = attrData;
		}

		/**
		 * Ustvari novo abstraktno sintaksno drevo z dodanimi atributi generiranja kode.
		 * 
		 * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi generiranja
		 *                kode.
		 */
		public AttrAST(final AttrAST attrAST) {
			super(attrAST);
			this.attrCode = attrAST.attrCode;
			this.attrData = attrAST.attrData;
		}

		@Override
		public String head(final AST.Node node, final boolean highlighted) {
			final StringBuffer head = new StringBuffer();
			head.append(super.head(node, false));
			return head.toString();
		}

		@Override
		public void desc(final int indent, final AST.Node node, final boolean highlighted) {
			super.desc(indent, node, false);
			System.out.print(highlighted ? "\033[31m" : "");
			if (attrCode.get(node) != null) {
				List<PDM.CodeInstr> instrs = attrCode.get(node);
				if (instrs != null) {
					if (indent > 0)
						System.out.printf("%" + indent + "c", ' ');
					System.out.printf("--- Code: ---\n");
					for (final PDM.CodeInstr instr : instrs) {
						if (indent > 0)
							System.out.printf("%" + indent + "c", ' ');
						System.out.println((instr instanceof PDM.LABEL ? "" : "  ") + instr.toString());
					}
				}
			}
			if (attrData.get(node) != null) {
				List<PDM.DataInstr> instrs = attrData.get(node);
				if (instrs != null) {
					if (indent > 0)
						System.out.printf("%" + indent + "c", ' ');
					System.out.printf("--- Data: ---\n");
					for (final PDM.DataInstr instr : instrs) {
						if (indent > 0)
							System.out.printf("%" + indent + "c", ' ');
						System.out.println((instr instanceof PDM.LABEL ? "" : "  ") + instr.toString());
					}
				}
			}
			System.out.print(highlighted ? "\033[30m" : "");
			return;
		}

	}

	/**
	 * Izracuna kodo programa
	 * 
	 * @param memoryAttrAST Abstraktno sintaksno drevo z dodanimi atributi izracuna
	 *                      pomnilniske predstavitve.
	 * @return Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
	 *         predstavitve.
	 */
	public static AttrAST generate(final Memory.AttrAST memoryAttrAST) {
		AttrAST attrAST = new AttrAST(memoryAttrAST, new HashMap<AST.Node, List<PDM.CodeInstr>>(),
				new HashMap<AST.Node, List<PDM.DataInstr>>());
		(new CodeGenerator(attrAST)).generate();
		return attrAST;
	}

	/**
	 * Generiranje kode v abstraktnem sintaksnem drevesu.
	 */
	private static class CodeGenerator {

		/**
		 * Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
		 * predstavitve.
		 */
		private final AttrAST attrAST;

		/** Stevec anonimnih label. */
		private int labelCounter = 0;

		/**
		 * Ustvari nov generator kode v abstraktnem sintaksnem drevesu.
		 * 
		 * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi izracuna
		 *                pomnilniske predstavitve.
		 */
		public CodeGenerator(final AttrAST attrAST) {
			this.attrAST = attrAST;
		}

		/**
		 * Sprozi generiranje kode v abstraktnem sintaksnem drevesu.
		 * 
		 * @return Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
		 *         predstavitve.
		 */
		public AttrAST generate() {
			attrAST.ast.accept(new Generator(), null);
			return new AttrAST(attrAST, Collections.unmodifiableMap(attrAST.attrCode),
					Collections.unmodifiableMap(attrAST.attrData));
		}

		/** Obiskovalec, ki generira kodo v abstraktnem sintaksnem drevesu. */
		private class Generator implements AST.FullVisitor<List<PDM.CodeInstr>, Mem.Frame> {

			@SuppressWarnings({ "doclint:missing" })
			public Generator() {
			}
			private static final int INT_SIZE = 4; 

			/* TODO */
			// Generate unique labels
			private String nextLabel() {
				return "L" + (labelCounter++);
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.FunDef funDef, Mem.Frame frame) {
				List<PDM.CodeInstr> funInstr = new ArrayList<>();
				Report.Locatable funLoc = attrAST.attrLoc.get(funDef);
				Mem.Frame funFrame = attrAST.attrFrame.get(funDef);

				// Function label
				PDM.LABEL funLabel = new PDM.LABEL(funDef.name, funLoc);
				funInstr.add(funLabel);

				// Generate code for function body (statements)
				for (AST.Stmt stmt : funDef.stmts) {
					List<PDM.CodeInstr> stmtCode = stmt.accept(this, funFrame);
					if (stmtCode != null) {
						funInstr.addAll(stmtCode);
					}
				}

				// Return instruction - return 0 by default
				funInstr.add(new PDM.PUSH(0, funLoc)); // Push return value
				funInstr.add(new PDM.PUSH(funFrame.parsSize, funLoc)); // Push parameter size
				funInstr.add(new PDM.RETN(funFrame, funLoc)); // Return

				// Store generated code
				attrAST.attrCode.put(funDef, funInstr);
				return funInstr;
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.VarDef varDef, Mem.Frame frame) {
				List<PDM.CodeInstr> varInstr = new ArrayList<>();
				Report.Locatable varLoc = attrAST.attrLoc.get(varDef);
				Mem.Access varAccess = attrAST.attrVarAccess.get(varDef);

				if (varAccess instanceof Mem.AbsAccess absAccess) {
					// Static variable initialization
					if (absAccess.inits != null) {
						// Push address of variable
						varInstr.add(new PDM.NAME(varDef.name, varLoc));
						// Push address of initialization data
						varInstr.add(new PDM.NAME(varDef.name + "_init", varLoc));
						// Initialize
						varInstr.add(new PDM.INIT(varLoc));
					}
				}
				// Local variables are handled in LetStmt

				attrAST.attrCode.put(varDef, varInstr);
				return varInstr;
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.ExprStmt exprStmt, Mem.Frame frame) {
				List<PDM.CodeInstr> stmtInstr = new ArrayList<>();
				Report.Locatable stmtLoc = attrAST.attrLoc.get(exprStmt);

				// Generate code for expression
				List<PDM.CodeInstr> exprCode = exprStmt.expr.accept(this, frame);
				if (exprCode != null) {
					stmtInstr.addAll(exprCode);
				}

				// Pop the result (expression statement doesn't use the value)
				stmtInstr.add(new PDM.PUSH(INT_SIZE, stmtLoc));
				stmtInstr.add(new PDM.POPN(stmtLoc));

				return stmtInstr;
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.AssignStmt assignStmt, Mem.Frame frame) {
				List<PDM.CodeInstr> assignInstr = new ArrayList<>();
				Report.Locatable assignLoc = attrAST.attrLoc.get(assignStmt);

				// Generate code for destination address (left side)
				List<PDM.CodeInstr> dstCode = generateAddress(assignStmt.dstExpr, frame);
				if (dstCode != null) {
					assignInstr.addAll(dstCode);
				}

				// Generate code for source value (right side)
				List<PDM.CodeInstr> srcCode = assignStmt.srcExpr.accept(this, frame);
				if (srcCode != null) {
					assignInstr.addAll(srcCode);
				}

				// Save: address is on stack, value is on top
				assignInstr.add(new PDM.SAVE(assignLoc));

				return assignInstr;
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.IfStmt ifStmt, Mem.Frame frame) {
				List<PDM.CodeInstr> ifInstr = new ArrayList<>();
				Report.Locatable ifLoc = attrAST.attrLoc.get(ifStmt);

				String elseLabel = nextLabel();
				String endLabel = nextLabel();

				// Generate condition code
				List<PDM.CodeInstr> condCode = ifStmt.cond.accept(this, frame);
				if (condCode != null) {
					ifInstr.addAll(condCode);
				}

				// Conditional jump: if condition is 0, jump to else
				ifInstr.add(new PDM.NAME(elseLabel, ifLoc));
				ifInstr.add(new PDM.NAME(endLabel, ifLoc));
				ifInstr.add(new PDM.CJMP(ifLoc));

				// Then statements
				for (AST.Stmt stmt : ifStmt.thenStmts) {
					List<PDM.CodeInstr> stmtCode = stmt.accept(this, frame);
					if (stmtCode != null) {
						ifInstr.addAll(stmtCode);
					}
				}

				// Jump to end
				ifInstr.add(new PDM.NAME(endLabel, ifLoc));
				ifInstr.add(new PDM.UJMP(ifLoc));

				// Else label
				ifInstr.add(new PDM.LABEL(elseLabel, ifLoc));

				// Else statements
				for (AST.Stmt stmt : ifStmt.elseStmts) {
					List<PDM.CodeInstr> stmtCode = stmt.accept(this, frame);
					if (stmtCode != null) {
						ifInstr.addAll(stmtCode);
					}
				}

				// End label
				ifInstr.add(new PDM.LABEL(endLabel, ifLoc));

				return ifInstr;
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.WhileStmt whileStmt, Mem.Frame frame) {
				List<PDM.CodeInstr> whileInstr = new ArrayList<>();
				Report.Locatable whileLoc = attrAST.attrLoc.get(whileStmt);

				String startLabel = nextLabel();
				String endLabel = nextLabel();

				// Start label
				whileInstr.add(new PDM.LABEL(startLabel, whileLoc));

				// Generate condition code
				List<PDM.CodeInstr> condCode = whileStmt.cond.accept(this, frame);
				if (condCode != null) {
					whileInstr.addAll(condCode);
				}

				// Conditional jump: if condition is 0, jump to end
				whileInstr.add(new PDM.NAME(endLabel, whileLoc));
				whileInstr.add(new PDM.NAME(startLabel, whileLoc));
				whileInstr.add(new PDM.CJMP(whileLoc));

				// Loop body
				for (AST.Stmt stmt : whileStmt.stmts) {
					List<PDM.CodeInstr> stmtCode = stmt.accept(this, frame);
					if (stmtCode != null) {
						whileInstr.addAll(stmtCode);
					}
				}

				// Jump back to start
				whileInstr.add(new PDM.NAME(startLabel, whileLoc));
				whileInstr.add(new PDM.UJMP(whileLoc));

				// End label
				whileInstr.add(new PDM.LABEL(endLabel, whileLoc));

				return whileInstr;
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.LetStmt letStmt, Mem.Frame frame) {
				List<PDM.CodeInstr> letInstr = new ArrayList<>();
				Report.Locatable letLoc = attrAST.attrLoc.get(letStmt);

				// Initialize local variables
				for (AST.MainDef def : letStmt.defs) {
					if (def instanceof AST.VarDef varDef) {
						Mem.Access varAccess = attrAST.attrVarAccess.get(varDef);
						if (varAccess instanceof Mem.RelAccess relAccess && relAccess.inits != null) {
							// Generate address of local variable
							letInstr.add(new PDM.REGN(PDM.REGN.Reg.FP, letLoc));
							letInstr.add(new PDM.PUSH(relAccess.offset, letLoc));
							letInstr.add(new PDM.OPER(PDM.OPER.Oper.ADD, letLoc));
							
							// Generate initialization data reference
							letInstr.add(new PDM.NAME(varDef.name + "_init", letLoc));
							
							// Initialize
							letInstr.add(new PDM.INIT(letLoc));
						}
					}
				}

				// Generate code for statements
				for (AST.Stmt stmt : letStmt.stmts) {
					List<PDM.CodeInstr> stmtCode = stmt.accept(this, frame);
					if (stmtCode != null) {
						letInstr.addAll(stmtCode);
					}
				}

				return letInstr;
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.AtomExpr atomExpr, Mem.Frame frame) {
				List<PDM.CodeInstr> atomInstr = new ArrayList<>();
				Report.Locatable atomLoc = attrAST.attrLoc.get(atomExpr);

				switch (atomExpr.type) {
					case INTCONST -> {
						Integer value = Memory.decodeIntConst(atomExpr, atomLoc);
						atomInstr.add(new PDM.PUSH(value, atomLoc));
					}
					case CHRCONST -> {
						Integer value = Memory.decodeChrConst(atomExpr, atomLoc);
						atomInstr.add(new PDM.PUSH(value, atomLoc));
					}
					case STRCONST -> {
						// Push address of string constant
						String strLabel = "STR_" + labelCounter++;
						atomInstr.add(new PDM.NAME(strLabel, atomLoc));
						
						// Generate data for string
						List<PDM.DataInstr> strData = new ArrayList<>();
						strData.add(new PDM.LABEL(strLabel, atomLoc));
						Vector<Integer> chars = Memory.decodeStrConst(atomExpr, atomLoc);
						for (Integer ch : chars) {
							strData.add(new PDM.DATA(ch, atomLoc));
						}
						attrAST.attrData.put(atomExpr, strData);
					}
				}

				return atomInstr;
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.UnExpr unExpr, Mem.Frame frame) {
				List<PDM.CodeInstr> unInstr = new ArrayList<>();
				Report.Locatable unLoc = attrAST.attrLoc.get(unExpr);

				switch (unExpr.oper) {
					case MEMADDR -> {
						// Generate address of operand
						List<PDM.CodeInstr> addrCode = generateAddress(unExpr.expr, frame);
						if (addrCode != null) {
							unInstr.addAll(addrCode);
						}
					}
					case VALUEAT -> {
						// Generate code for address expression
						List<PDM.CodeInstr> exprCode = unExpr.expr.accept(this, frame);
						if (exprCode != null) {
							unInstr.addAll(exprCode);
						}
						// Load value at address
						unInstr.add(new PDM.LOAD(unLoc));
					}
					default -> {
						// Generate code for operand
						List<PDM.CodeInstr> exprCode = unExpr.expr.accept(this, frame);
						if (exprCode != null) {
							unInstr.addAll(exprCode);
						}
						
						// Apply unary operator
						switch (unExpr.oper) {
							case NOT -> unInstr.add(new PDM.OPER(PDM.OPER.Oper.NOT, unLoc));
							case SUB -> unInstr.add(new PDM.OPER(PDM.OPER.Oper.NEG, unLoc));
							case ADD -> { /* No operation needed for unary plus */ }
						}
					}
				}

				return unInstr;
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.BinExpr binExpr, Mem.Frame frame) {
				List<PDM.CodeInstr> binInstr = new ArrayList<>();
				Report.Locatable binLoc = attrAST.attrLoc.get(binExpr);

				// Generate code for first operand
				List<PDM.CodeInstr> fstCode = binExpr.fstExpr.accept(this, frame);
				if (fstCode != null) {
					binInstr.addAll(fstCode);
				}

				// Generate code for second operand
				List<PDM.CodeInstr> sndCode = binExpr.sndExpr.accept(this, frame);
				if (sndCode != null) {
					binInstr.addAll(sndCode);
				}

				// Apply binary operator
				PDM.OPER.Oper oper = switch (binExpr.oper) {
					case OR -> PDM.OPER.Oper.OR;
					case AND -> PDM.OPER.Oper.AND;
					case EQU -> PDM.OPER.Oper.EQU;
					case NEQ -> PDM.OPER.Oper.NEQ;
					case GTH -> PDM.OPER.Oper.GTH;
					case LTH -> PDM.OPER.Oper.LTH;
					case GEQ -> PDM.OPER.Oper.GEQ;
					case LEQ -> PDM.OPER.Oper.LEQ;
					case ADD -> PDM.OPER.Oper.ADD;
					case SUB -> PDM.OPER.Oper.SUB;
					case MUL -> PDM.OPER.Oper.MUL;
					case DIV -> PDM.OPER.Oper.DIV;
					case MOD -> PDM.OPER.Oper.MOD;
				};

				binInstr.add(new PDM.OPER(oper, binLoc));
				return binInstr;
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.VarExpr varExpr, Mem.Frame frame) {
				List<PDM.CodeInstr> varInstr = new ArrayList<>();
				Report.Locatable varLoc = attrAST.attrLoc.get(varExpr);

				// Search through all variable definitions to find matching name
				for (Map.Entry<AST.VarDef, Mem.Access> entry : attrAST.attrVarAccess.entrySet()) {
					AST.VarDef vDef = entry.getKey();
					if (vDef.name.equals(varExpr.name)) {
						Mem.Access varAccess = entry.getValue();
						if (varAccess instanceof Mem.AbsAccess) {
							// Static variable - load by name
							varInstr.add(new PDM.NAME(varExpr.name, varLoc));
							varInstr.add(new PDM.LOAD(varLoc));
						} else if (varAccess instanceof Mem.RelAccess relAccess) {
							// Local variable - load relative to frame pointer
							varInstr.add(new PDM.REGN(PDM.REGN.Reg.FP, varLoc));
							varInstr.add(new PDM.PUSH(relAccess.offset, varLoc));
							varInstr.add(new PDM.OPER(PDM.OPER.Oper.ADD, varLoc));
							varInstr.add(new PDM.LOAD(varLoc));
						}
						return varInstr;
					}
				}

				// Search through all parameter definitions to find matching name
				for (Map.Entry<AST.ParDef, Mem.RelAccess> entry : attrAST.attrParAccess.entrySet()) {
					AST.ParDef pDef = entry.getKey();
					if (pDef.name.equals(varExpr.name)) {
						Mem.RelAccess parAccess = entry.getValue();
						varInstr.add(new PDM.REGN(PDM.REGN.Reg.FP, varLoc));
						varInstr.add(new PDM.PUSH(parAccess.offset, varLoc));
						varInstr.add(new PDM.OPER(PDM.OPER.Oper.ADD, varLoc));
						varInstr.add(new PDM.LOAD(varLoc));
						return varInstr;
					}
				}

				// If not found, it might be an error or built-in function
				throw new Report.Error(varLoc, "Unknown variable: " + varExpr.name);
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.CallExpr callExpr, Mem.Frame frame) {
				List<PDM.CodeInstr> callInstr = new ArrayList<>();
				Report.Locatable callLoc = attrAST.attrLoc.get(callExpr);

				// Find the function definition by searching through frames
				Mem.Frame calledFrame = null;
				for (Map.Entry<AST.FunDef, Mem.Frame> entry : attrAST.attrFrame.entrySet()) {
					AST.FunDef fDef = entry.getKey();
					if (fDef.name.equals(callExpr.name)) {
						calledFrame = entry.getValue();
						break;
					}
				}

				if (calledFrame != null) {
					// Push static link (current frame pointer for nested functions)
					if (calledFrame.depth > frame.depth + 1) {
						// Need to traverse static links
						callInstr.add(new PDM.REGN(PDM.REGN.Reg.FP, callLoc));
						// Add code to follow static links if needed
					} else {
						callInstr.add(new PDM.REGN(PDM.REGN.Reg.FP, callLoc));
					}

					// Push arguments in reverse order
					for (int i = callExpr.args.size() - 1; i >= 0; i--) {
						List<PDM.CodeInstr> argCode = callExpr.args.get(i).accept(this, frame);
						if (argCode != null) {
							callInstr.addAll(argCode);
						}
					}

					// Push function address and call
					callInstr.add(new PDM.NAME(callExpr.name, callLoc));
					callInstr.add(new PDM.CALL(calledFrame, callLoc));
				} else {
					// Built-in function or error
					throw new Report.Error(callLoc, "Unknown function: " + callExpr.name);
				}

				return callInstr;
			}

			// Helper method to generate address of an expression (for assignments and references)
			private List<PDM.CodeInstr> generateAddress(AST.Expr expr, Mem.Frame frame) {
				List<PDM.CodeInstr> addrInstr = new ArrayList<>();
				Report.Locatable loc = attrAST.attrLoc.get(expr);

				if (expr instanceof AST.VarExpr varExpr) {
					// Search through variables
					for (Map.Entry<AST.VarDef, Mem.Access> entry : attrAST.attrVarAccess.entrySet()) {
						AST.VarDef vDef = entry.getKey();
						if (vDef.name.equals(varExpr.name)) {
							Mem.Access varAccess = entry.getValue();
							if (varAccess instanceof Mem.AbsAccess) {
								// Static variable address
								addrInstr.add(new PDM.NAME(varExpr.name, loc));
							} else if (varAccess instanceof Mem.RelAccess relAccess) {
								// Local variable address
								addrInstr.add(new PDM.REGN(PDM.REGN.Reg.FP, loc));
								addrInstr.add(new PDM.PUSH(relAccess.offset, loc));
								addrInstr.add(new PDM.OPER(PDM.OPER.Oper.ADD, loc));
							}
							return addrInstr;
						}
					}
					
					// Search through parameters
					for (Map.Entry<AST.ParDef, Mem.RelAccess> entry : attrAST.attrParAccess.entrySet()) {
						AST.ParDef pDef = entry.getKey();
						if (pDef.name.equals(varExpr.name)) {
							Mem.RelAccess parAccess = entry.getValue();
							addrInstr.add(new PDM.REGN(PDM.REGN.Reg.FP, loc));
							addrInstr.add(new PDM.PUSH(parAccess.offset, loc));
							addrInstr.add(new PDM.OPER(PDM.OPER.Oper.ADD, loc));
							return addrInstr;
						}
					}
				} else if (expr instanceof AST.UnExpr unExpr && unExpr.oper == AST.UnExpr.Oper.VALUEAT) {
					// Dereference - the address is the value of the expression
					List<PDM.CodeInstr> exprCode = unExpr.expr.accept(this, frame);
					if (exprCode != null) {
						addrInstr.addAll(exprCode);
					}
				}

				return addrInstr;
			}

			// Visit methods that don't generate code directly
			@Override
			public List<PDM.CodeInstr> visit(AST.Nodes<? extends AST.Node> nodes, Mem.Frame frame) {
				List<PDM.CodeInstr> allInstr = new ArrayList<>();
				for (AST.Node node : nodes) {
					List<PDM.CodeInstr> nodeCode = node.accept(this, frame);
					if (nodeCode != null) {
						allInstr.addAll(nodeCode);
					}
				}
				return allInstr;
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.ParDef parDef, Mem.Frame frame) {
				// Parameters don't generate code themselves
				return null;
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.Init init, Mem.Frame frame) {
				// Initialization is handled in variable definitions
				return null;
			}




		}

	}

	/**
	 * Generator seznama ukazov, ki predstavljajo kodo programa.
	 */
	public static class CodeSegmentGenerator {

		/**
		 * Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
		 * predstavitve.
		 */
		private final AttrAST attrAST;

		/** Seznam ukazov za inicializacijo staticnih spremenljivk. */
		private final Vector<PDM.CodeInstr> codeInitSegment = new Vector<PDM.CodeInstr>();

		/** Seznam ukazov funkcij. */
		private final Vector<PDM.CodeInstr> codeFunsSegment = new Vector<PDM.CodeInstr>();

		/** Klicni zapis funkcije {@code main}. */
		private Mem.Frame main = null;

		/**
		 * Ustvari nov generator seznama ukazov, ki predstavljajo kodo programa.
		 *
		 * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi izracuna
		 *                pomnilniske predstavitve.
		 */
		public CodeSegmentGenerator(final AttrAST attrAST) {
			this.attrAST = attrAST;
		}

		/**
		 * Izracuna seznam ukazov, ki predstavljajo kodo programa.
		 * 
		 * @return Seznam ukazov, ki predstavljajo kodo programa.
		 */
		public List<PDM.CodeInstr> codeSegment() {
			attrAST.ast.accept(new Generator(), null);
			codeInitSegment.addLast(new PDM.PUSH(0, null));
			codeInitSegment.addLast(new PDM.NAME("main", null));
			codeInitSegment.addLast(new PDM.CALL(main, null));
			codeInitSegment.addLast(new PDM.PUSH(0, null));
			codeInitSegment.addLast(new PDM.NAME("exit", null));
			codeInitSegment.addLast(new PDM.CALL(null, null));
			final Vector<PDM.CodeInstr> codeSegment = new Vector<PDM.CodeInstr>();
			codeSegment.addAll(codeInitSegment);
			codeSegment.addAll(codeFunsSegment);
			return Collections.unmodifiableList(codeSegment);
		}

		/**
		 * Obiskovalec, ki izracuna seznam ukazov, ki predstavljajo kodo programa.
		 */
		private class Generator implements AST.FullVisitor<Object, Object> {

			@SuppressWarnings({ "doclint:missing" })
			public Generator() {
			}

			@Override
			public Object visit(final AST.FunDef funDef, final Object arg) {
				if (funDef.stmts.size() == 0)
					return null;
				List<PDM.CodeInstr> code = attrAST.attrCode.get(funDef);
				codeFunsSegment.addAll(code);
				funDef.pars.accept(this, arg);
				funDef.stmts.accept(this, arg);
				switch (funDef.name) {
				case "main" -> main = attrAST.attrFrame.get(funDef);
				}
				return null;
			}

			@Override
			public Object visit(final AST.VarDef varDef, final Object arg) {
				switch (attrAST.attrVarAccess.get(varDef)) {
				case Mem.AbsAccess __: {
					List<PDM.CodeInstr> code = attrAST.attrCode.get(varDef);
					codeInitSegment.addAll(code);
					break;
				}
				case Mem.RelAccess __: {
					break;
				}
				default:
					throw new Report.InternalError();
				}
				return null;
			}

		}

	}

	/**
	 * Generator seznama ukazov, ki predstavljajo podatke programa.
	 */
	public static class DataSegmentGenerator {

		/**
		 * Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
		 * predstavitve.
		 */
		private final AttrAST attrAST;

		/** Seznam ukazov, ki predstavljajo podatke programa. */
		private final Vector<PDM.DataInstr> dataSegment = new Vector<PDM.DataInstr>();

		/**
		 * Ustvari nov generator seznama ukazov, ki predstavljajo podatke programa.
		 *
		 * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi izracuna
		 *                pomnilniske predstavitve.
		 */
		public DataSegmentGenerator(final AttrAST attrAST) {
			this.attrAST = attrAST;
		}

		/**
		 * Izracuna seznam ukazov, ki predstavljajo podatke programa.
		 * 
		 * @return Seznam ukazov, ki predstavljajo podatke programa.
		 */
		public List<PDM.DataInstr> dataSegment() {
			attrAST.ast.accept(new Generator(), null);
			return Collections.unmodifiableList(dataSegment);
		}

		/**
		 * Obiskovalec, ki izracuna seznam ukazov, ki predstavljajo podatke programa.
		 */
		private class Generator implements AST.FullVisitor<Object, Object> {

			@SuppressWarnings({ "doclint:missing" })
			public Generator() {
			}

			@Override
			public Object visit(final AST.VarDef varDef, final Object arg) {
				List<PDM.DataInstr> data = attrAST.attrData.get(varDef);
				if (data != null)
					dataSegment.addAll(data);
				varDef.inits.accept(this, arg);
				return null;
			}

			@Override
			public Object visit(final AST.AtomExpr atomExpr, final Object arg) {
				List<PDM.DataInstr> data = attrAST.attrData.get(atomExpr);
				if (data != null)
					dataSegment.addAll(data);
				return null;
			}

		}

	}

	// --- ZAGON ---

	/**
	 * Zagon izracuna pomnilniske predstavitve kot samostojnega programa.
	 * 
	 * @param cmdLineArgs Argumenti v ukazni vrstici.
	 */
	public static void main(final String[] cmdLineArgs) {
		System.out.println("This is PINS'25 compiler (code generation):");

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
				// generiranje kode:
				final CodeGen.AttrAST codegenAttrAST = CodeGen.generate(memoryAttrAST);

				(new AST.Logger(codegenAttrAST)).log();
				{
					int addr = 0;
					final List<PDM.CodeInstr> codeSegment = (new CodeSegmentGenerator(codegenAttrAST)).codeSegment();
					{
						System.out.println("\n\033[1mCODE SEGMENT:\033[0m");
						for (final PDM.CodeInstr instr : codeSegment) {
							System.out.printf("%8d [%s] %s\n", addr, instr.size(),
									(instr instanceof PDM.LABEL ? "" : "  ") + instr.toString());
							addr += instr.size();
						}
					}
					final List<PDM.DataInstr> dataSegment = (new DataSegmentGenerator(codegenAttrAST)).dataSegment();
					{
						System.out.println("\n\033[1mDATA SEGMENT:\033[0m");
						for (final PDM.DataInstr instr : dataSegment) {
							System.out.printf("%8d [%s] %s\n", addr, (instr instanceof PDM.SIZE) ? " " : instr.size(),
									(instr instanceof PDM.LABEL ? "" : "  ") + instr.toString());
							addr += instr.size();
						}
					}
					System.out.println();
				}
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