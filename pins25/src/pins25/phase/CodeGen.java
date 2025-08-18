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

            /** Stevec anonimnih label. */
            private int labelCounter = 0;

            /** Map from function definition to unique label name */
            private final Map<AST.FunDef, String> funLabelMap = new HashMap<>();

			// Generate unique labels
			private String nextLabel() {
				return "L" + (labelCounter++);
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.FunDef funDef, Mem.Frame frame) {
				List<PDM.CodeInstr> codeInstr = new ArrayList<>();
				Report.Locatable loc = attrAST.attrLoc.get(funDef);

                // Generate unique name if nested (fun name, parent name, depth)
                String funName = funDef.name;
                if (frame != null) {
                    funName += "_FUN_" + frame.name + "_D" + frame.depth;
                }
                funLabelMap.put(funDef, funName);

                // Retriver Function Frame for current funDef
				Mem.Frame funFrame = attrAST.attrFrame.get(funDef);

				// 1. Function label (Call address)
				PDM.LABEL funLabel = new PDM.LABEL(funName, loc);
				codeInstr.add(funLabel);

				// 2. Function body code (statements)
				for (AST.Stmt stmt : funDef.stmts) {
					List<PDM.CodeInstr> stmtCode = stmt.accept(this, funFrame);
                    codeInstr.addAll(stmtCode);
				}

				// 3. Return instruction
                // Result shoud be at current stack location
                // Push parameter size (parsSize - SL)
				codeInstr.add(new PDM.PUSH(funFrame.parsSize - INT_SIZE, loc));
                // Return instruction
				codeInstr.add(new PDM.RETN(funFrame, loc));

				// Store generated code
				attrAST.attrCode.put(funDef, codeInstr);
				return codeInstr;
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.VarDef varDef, Mem.Frame frame) {
				List<PDM.CodeInstr> codeInstr = new ArrayList<>();
				Report.Locatable loc = attrAST.attrLoc.get(varDef);

				Mem.Access var = attrAST.attrVarAccess.get(varDef);

                // Generate unique name if nested (var name, parent name, depth)
                String varName = varDef.name;
                String varInitName = varDef.name + "_INIT";
                if (frame != null)
                {
                    // Local var
                    varName += "_VAR_" + frame.name + "_D" + frame.depth;
                    varInitName += "_" + frame.name + "_D" +frame.depth;
                }
                // Global variables
                // Global variables - generate INIT code for initialization
                if (var instanceof Mem.AbsAccess && var.inits != null) {
                    // Variable address
                    codeInstr.add(new PDM.NAME(varName, loc));
                    // Variable Data address
                    codeInstr.add(new PDM.NAME(varInitName, loc));
                    // Call INIT
                    codeInstr.add(new PDM.INIT(loc));
                }
                else if (var instanceof Mem.RelAccess relAccess)
                {
                    // Put value 0 to next cell (space for var)
                    codeInstr.add(new PDM.PUSH(-relAccess.size, loc));
                    codeInstr.add(new PDM.POPN(loc));
                    // Variable address
                    codeInstr.add(new PDM.REGN(PDM.REGN.Reg.FP, loc));
                    codeInstr.add(new PDM.PUSH(relAccess.offset, loc));
                    codeInstr.add(new PDM.OPER(PDM.OPER.Oper.ADD, loc));
                    // Variable data address
                    codeInstr.add(new PDM.NAME(varInitName, loc));
                    // Call INIT
                    codeInstr.add(new PDM.INIT(loc));
                }

                // Local/Global variable - generate data segment
                List<PDM.DataInstr> varData = new ArrayList<>();

                // 1. FIRST: Variable storage space
                varData.add(new PDM.LABEL(varName, loc));           // Label "a"
                varData.add(new PDM.SIZE(var.size, loc));         // Reserve space for variable

                // 2. SECOND: Initialization data (if exists)
                if (var.inits != null) {
                    varData.add(new PDM.LABEL(varInitName, loc)); // Label "a_INIT"

                    // Add all initialization data values
                    List<Integer> inits = var.inits;
                    for (Integer value : inits) {
                        varData.add(new PDM.DATA(value, loc));
                    }
                }

                // Store data segment for this variable
                attrAST.attrData.put(varDef, varData);

				// Store code segment for this variable
				attrAST.attrCode.put(varDef, codeInstr);
				return codeInstr;
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.ExprStmt exprStmt, Mem.Frame frame) {
				List<PDM.CodeInstr> stmtInstr = new ArrayList<>();
				Report.Locatable stmtLoc = attrAST.attrLoc.get(exprStmt);

				// Generate code for expression
				List<PDM.CodeInstr> exprCode = exprStmt.expr.accept(this, frame);
                stmtInstr.addAll(exprCode);


				return stmtInstr;
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.AssignStmt assignStmt, Mem.Frame frame) {
				List<PDM.CodeInstr> codeInstr = new ArrayList<>();
				Report.Locatable loc = attrAST.attrLoc.get(assignStmt);

                // 1. Source value (right side)
                List<PDM.CodeInstr> srcCode = assignStmt.srcExpr.accept(this, frame);
                codeInstr.addAll(srcCode);

				// 2. Destination address (left side)
//				List<PDM.CodeInstr> dstCode = handleAddressOf(assignStmt.dstExpr, frame);
				List<PDM.CodeInstr> dstCode = assignStmt.dstExpr.accept(this, frame);
                codeInstr.addAll(dstCode);

                // 3. SAVE value to memory on address
				codeInstr.add(new PDM.SAVE(loc));

				return codeInstr;
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.IfStmt ifStmt, Mem.Frame frame) {
				List<PDM.CodeInstr> ifInstr = new ArrayList<>();
				Report.Locatable ifLoc = attrAST.attrLoc.get(ifStmt);

				String thenLabel = "J_" + nextLabel();
				String elseLabel = "J_" + nextLabel();
				String endLabel = "J_" + nextLabel();

				// Generate condition code (value is now on stack)
				List<PDM.CodeInstr> condCode = ifStmt.cond.accept(this, frame);
                ifInstr.addAll(condCode);


				// First jump: CJMP - if condition != 0, go to then; if condition == 0, go to else
				ifInstr.add(new PDM.NAME(thenLabel, ifLoc));     // addr2 - go to then (condition != 0)
				ifInstr.add(new PDM.NAME(elseLabel, ifLoc));     // addr1 - go to else (condition == 0)
				ifInstr.add(new PDM.CJMP(ifLoc));

				// Then label and statements
				ifInstr.add(new PDM.LABEL(thenLabel, ifLoc));
				for (AST.Stmt stmt : ifStmt.thenStmts) {
					List<PDM.CodeInstr> stmtCode = stmt.accept(this, frame);
                    ifInstr.addAll(stmtCode);
				}

				// Second jump: UJMP - after then statements, jump to end (skip else)
				ifInstr.add(new PDM.NAME(endLabel, ifLoc));
				ifInstr.add(new PDM.UJMP(ifLoc));

				// Else label and statements
				ifInstr.add(new PDM.LABEL(elseLabel, ifLoc));
				for (AST.Stmt stmt : ifStmt.elseStmts) {
					List<PDM.CodeInstr> stmtCode = stmt.accept(this, frame);
                    ifInstr.addAll(stmtCode);

				}

				// End label
				ifInstr.add(new PDM.LABEL(endLabel, ifLoc));

				return ifInstr;
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.WhileStmt whileStmt, Mem.Frame frame) {
				List<PDM.CodeInstr> whileInstr = new ArrayList<>();
				Report.Locatable whileLoc = attrAST.attrLoc.get(whileStmt);

				String startLabel = "J_" + nextLabel();  // Beginning of loop (condition check)
				String bodyLabel = "J_" + nextLabel();   // Start of loop body
				String endLabel = "J_" + nextLabel();    // End of loop

				// Start label - where we check the condition
				whileInstr.add(new PDM.LABEL(startLabel, whileLoc));

				// Generate condition code (value is now on stack)
				List<PDM.CodeInstr> condCode = whileStmt.cond.accept(this, frame);
                whileInstr.addAll(condCode);

				// First jump: CJMP - if condition == 0, exit loop; if condition != 0, continue to body
				whileInstr.add(new PDM.NAME(bodyLabel, whileLoc));   // addr2 - go to body (condition != 0)
				whileInstr.add(new PDM.NAME(endLabel, whileLoc));    // addr1 - go to end (condition == 0) - EXIT LOOP
				whileInstr.add(new PDM.CJMP(whileLoc));

				// Body label and statements
				whileInstr.add(new PDM.LABEL(bodyLabel, whileLoc));
				for (AST.Stmt stmt : whileStmt.stmts) {
					List<PDM.CodeInstr> stmtCode = stmt.accept(this, frame);
                    whileInstr.addAll(stmtCode);

				}

				// Second jump: UJMP - after body, jump back to start (condition check)
				whileInstr.add(new PDM.NAME(startLabel, whileLoc));
				whileInstr.add(new PDM.UJMP(whileLoc));

				// End label
				whileInstr.add(new PDM.LABEL(endLabel, whileLoc));

				return whileInstr;
			}
			@Override
			public List<PDM.CodeInstr> visit(AST.LetStmt letStmt, Mem.Frame frame) {
				List<PDM.CodeInstr> codeInstr = new ArrayList<>();
				Report.Locatable loc = attrAST.attrLoc.get(letStmt);

                // MainDefs code (funDef, varDef)
                for (AST.MainDef def : letStmt.defs)
                {
                    List<PDM.CodeInstr> defCode = def.accept(this, frame);
                    // Add code only for variables definition
                    // Skip Function definition (function itself) only CALL statement
                    if (def instanceof AST.VarDef)
                    {
                        codeInstr.addAll(defCode);
                    }

                }

				// Generate code for statements
				for (AST.Stmt stmt : letStmt.stmts) {
					List<PDM.CodeInstr> stmtCode = stmt.accept(this, frame);
                    codeInstr.addAll(stmtCode);
				}

				return codeInstr;
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
						Vector<Integer> chars = Memory.decodeStrConst(atomExpr, atomLoc);

                        // Generate simple null-terminated string for function calls like putstr
                        String strLabel = "STR_" + labelCounter++;
                        atomInstr.add(new PDM.NAME(strLabel, atomLoc));

                        List<PDM.DataInstr> strData = new ArrayList<>();
                        strData.add(new PDM.LABEL(strLabel, atomLoc));
                        // Simple format: just characters
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
				List<PDM.CodeInstr> codeInstr = new ArrayList<>();
				Report.Locatable loc = attrAST.attrLoc.get(unExpr);

				switch (unExpr.oper) {
					case MEMADDR -> {
						// Generate address of operand
                        if (unExpr.expr instanceof AST.VarExpr varExpr)
                        {
                            // Generate address of variable only (NO LOAD)
                            List<PDM.CodeInstr> addrInstr = generateAddress(varExpr, frame);
                            codeInstr.addAll(addrInstr);
                        }
                        else
                        {
                            // For other expressions (UnExpr, BinExpr, CallExpr, AtomExpr, etc.)
                            // Generate the expression normally
                            List<PDM.CodeInstr> exprInstr = unExpr.expr.accept(this, frame);
                            codeInstr.addAll(exprInstr);
                        }
					}
					case VALUEAT -> {
						// Generate code for address value
						List<PDM.CodeInstr> exprCode = unExpr.expr.accept(this, frame);
                        codeInstr.addAll(exprCode);

                        if (attrAST.attrLVal.get(unExpr) != Boolean.TRUE)
                        {
                            // UnExpression is not left value
                            // LOAD vlaue from address
                            codeInstr.add(new PDM.LOAD(loc));
                        }
					}
					default -> {
						// Generate code for operand
						List<PDM.CodeInstr> exprCode = unExpr.expr.accept(this, frame);
                        codeInstr.addAll(exprCode);

						
						// Apply unary operator
						switch (unExpr.oper) {
							case NOT -> codeInstr.add(new PDM.OPER(PDM.OPER.Oper.NOT, loc));
							case SUB -> codeInstr.add(new PDM.OPER(PDM.OPER.Oper.NEG, loc));
							case ADD -> { /* No operation needed for unary plus */ }
						}
					}
				}

				return codeInstr;
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.BinExpr binExpr, Mem.Frame frame) {
				List<PDM.CodeInstr> binInstr = new ArrayList<>();
				Report.Locatable binLoc = attrAST.attrLoc.get(binExpr);

				// Generate code for first operand
				List<PDM.CodeInstr> fstCode = binExpr.fstExpr.accept(this, frame);
                binInstr.addAll(fstCode);


				// Generate code for second operand
				List<PDM.CodeInstr> sndCode = binExpr.sndExpr.accept(this, frame);
                binInstr.addAll(sndCode);


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
				List<PDM.CodeInstr> codeInstr = new ArrayList<>();
				Report.Locatable loc = attrAST.attrLoc.get(varExpr);

                // Generate address calculation
                codeInstr.addAll(generateAddress(varExpr, frame));

                if (attrAST.attrLVal.get(varExpr) != Boolean.TRUE)
                {
                    // Varaible is not left value
                    // LOAD vlaue from address
                    codeInstr.add(new PDM.LOAD(loc));
                }

                return codeInstr;
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.CallExpr callExpr, Mem.Frame frame) {
				List<PDM.CodeInstr> codeInstr = new ArrayList<>();
				Report.Locatable loc = attrAST.attrLoc.get(callExpr);


                // 1. Push arguments in reverse order
                for (int i = callExpr.args.size() - 1; i >= 0; i--) {
                    List<PDM.CodeInstr> argCode = callExpr.args.get(i).accept(this, frame);
                    codeInstr.addAll(argCode);
                }

                // Frame of calling function
                AST.FunDef funDef = (AST.FunDef)attrAST.attrDef.get(callExpr);
                Mem.Frame funFrame = attrAST.attrFrame.get(funDef);

                // 2. Set Static Link
                // Current FP
                codeInstr.add(new PDM.REGN(PDM.REGN.Reg.FP, loc));

                // Load until depth is smaller than called depth
                // Correct depth = funFrame.depth - depth (parent) = 1
                int i = frame.depth;
                while (i >= funFrame.depth) {
                    codeInstr.add(new PDM.LOAD(loc));
                    i--;
                }

                // 3. Push function address and call
                String funName = funLabelMap.get(funDef);

                codeInstr.add(new PDM.NAME(funName, loc));
                codeInstr.add(new PDM.CALL(funFrame, loc));

				return codeInstr;
			}

            private List<PDM.CodeInstr> generateAddress(AST.VarExpr varExpr, Mem.Frame frame) {
                List<PDM.CodeInstr> codeInstr = new ArrayList<>();
                Report.Locatable loc = attrAST.attrLoc.get(varExpr);

                // Use semantic analysis to find the definition
                AST.Def definition = attrAST.attrDef.get(varExpr);

                // 1. Absolute Access (global variable)
                if (definition instanceof AST.VarDef varDef) {
                    // Variable definition
                    Mem.Access var = attrAST.attrVarAccess.get(varDef);

                    if (var instanceof Mem.AbsAccess) {
                        // Global variable - just push its address
                        codeInstr.add(new PDM.NAME(varExpr.name, loc));
                        // NO LOAD - we want the address, not the value
                        return codeInstr;
                    }
                }

                // 2. Relative Access
                Mem.RelAccess relAccess = null;  // Variable / Parameter
                if (definition instanceof AST.VarDef varDef) {
                    // Variable definition
                    Mem.Access var = attrAST.attrVarAccess.get(varDef);
                    if (var instanceof Mem.RelAccess localVar) {
                        relAccess = localVar;
                    }
                }
                else if (definition instanceof AST.ParDef parDef) {
                    // Parameter definition
                    relAccess = attrAST.attrParAccess.get(parDef);
                }
                if (relAccess == null) {
                    throw new Report.Error(loc, "Cannot determine access for variable: " + varExpr.name);
                }

                // Generate access adress (with offset from frame where defined)
                if (relAccess.depth.equals(frame.depth))
                {
                    // 1. Defined in current frame
                    codeInstr.add(new PDM.REGN(PDM.REGN.Reg.FP, loc));
                    codeInstr.add(new PDM.PUSH(relAccess.offset, loc));
                    codeInstr.add(new PDM.OPER(PDM.OPER.Oper.ADD, loc));
                }
                else {
                    // 2. Follow SL to get to correct frame depth (where var is defiend)
                    // Current FP
                    codeInstr.add(new PDM.REGN(PDM.REGN.Reg.FP, loc));

                    int i = frame.depth;
                    while (i > relAccess.depth) {
                        codeInstr.add(new PDM.LOAD(loc));
                        i--;
                    }
                    // Variable depth reached - LOAD value
                    codeInstr.add(new PDM.PUSH(relAccess.offset, loc));
                    codeInstr.add(new PDM.OPER(PDM.OPER.Oper.ADD, loc));
                    // NO LOAD - we want the address, not the value
                }

                return codeInstr;
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