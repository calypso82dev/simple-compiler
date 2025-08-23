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

            private int labelCounter = 0;
            private String generateJumpLabel() {
                return "L" + (labelCounter++);
            }

            private String generateStringLabel(AST.AtomExpr atomExpr) {
                int nodeHash = Math.abs(System.identityHashCode(atomExpr));
                return "STR_" + nodeHash;
            }

            private String generateScopedLabel(String baseName, String type, AST.Node node, int scopeDepth) {
                // Global scope (no parent frame)
                if (scopeDepth == 0) {
                    return baseName;
                }

                // Local scope (let) - create unique label
                int nodeHash = Math.abs(System.identityHashCode(node));
                return String.format("%s_%s_%d", type, baseName, nodeHash);
            }

            private String generateFunctionLabel(AST.FunDef funDef, int scopeDepth) {
                return generateScopedLabel(funDef.name, "FUN", funDef, scopeDepth);
            }

            private String generateVariableLabel(AST.VarDef varDef, int scopeDepth) {
                return generateScopedLabel(varDef.name, "VAR", varDef, scopeDepth);
            }

            private String generateVariableInitLabel(AST.VarDef varDef, int scopeDepth) {
                return generateScopedLabel(varDef.name + "_INIT", "VAR", varDef, scopeDepth);
            }

			@Override
			public List<PDM.CodeInstr> visit(AST.FunDef funDef, Mem.Frame frame) {
				List<PDM.CodeInstr> codeInstr = new ArrayList<>();
				Report.Locatable loc = attrAST.attrLoc.get(funDef);

                // Retriver Function Frame for current funDef
				Mem.Frame funFrame = attrAST.attrFrame.get(funDef);

				// 1. Function label (Call address)
                int scopeDepth = frame != null ? frame.depth : 0;
                String funName = generateFunctionLabel(funDef, scopeDepth);
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
                int scopeDepth = frame != null ? frame.depth : 0;
                String varName = generateVariableLabel(varDef, scopeDepth);
                String varInitName = generateVariableInitLabel(varDef, scopeDepth);

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
				List<PDM.CodeInstr> codeInstr = new ArrayList<>();

				// Generate code for expression
				List<PDM.CodeInstr> exprCode = exprStmt.expr.accept(this, frame);
                codeInstr.addAll(exprCode);


				return codeInstr;
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
				List<PDM.CodeInstr> codeInstr = new ArrayList<>();
				Report.Locatable loc = attrAST.attrLoc.get(ifStmt);

				String thenLabel = "J_IF_THEN_" + generateJumpLabel();
				String elseLabel = "J_IF_ELSE_" + generateJumpLabel();
				String endLabel = "J_IF_END_" + generateJumpLabel();

				// Generate condition code (value is now on stack)
				List<PDM.CodeInstr> condCode = ifStmt.cond.accept(this, frame);
                codeInstr.addAll(condCode);


				// First jump: CJMP - if condition != 0, go to then; if condition == 0, go to else
				codeInstr.add(new PDM.NAME(thenLabel, loc));     // addr2 - go to then (condition != 0)
				codeInstr.add(new PDM.NAME(elseLabel, loc));     // addr1 - go to else (condition == 0)
				codeInstr.add(new PDM.CJMP(loc));

				// Then label and statements
				codeInstr.add(new PDM.LABEL(thenLabel, loc));
				for (AST.Stmt stmt : ifStmt.thenStmts) {
					List<PDM.CodeInstr> stmtCode = stmt.accept(this, frame);
                    codeInstr.addAll(stmtCode);
				}

				// Second jump: UJMP - after then statements, jump to end (skip else)
				codeInstr.add(new PDM.NAME(endLabel, loc));
				codeInstr.add(new PDM.UJMP(loc));

				// Else label and statements
				codeInstr.add(new PDM.LABEL(elseLabel, loc));
				for (AST.Stmt stmt : ifStmt.elseStmts) {
					List<PDM.CodeInstr> stmtCode = stmt.accept(this, frame);
                    codeInstr.addAll(stmtCode);

				}

				// End label
				codeInstr.add(new PDM.LABEL(endLabel, loc));

				return codeInstr;
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.WhileStmt whileStmt, Mem.Frame frame) {
				List<PDM.CodeInstr> codeInstr = new ArrayList<>();
				Report.Locatable loc = attrAST.attrLoc.get(whileStmt);

				String startLabel = "J_WH_START_" + generateJumpLabel();  // Beginning of loop (condition check)
				String bodyLabel = "J_WH_BODY_" + generateJumpLabel();   // Start of loop body
				String endLabel = "J_WH_END_" + generateJumpLabel();    // End of loop

				// Start label - where we check the condition
				codeInstr.add(new PDM.LABEL(startLabel, loc));

				// Generate condition code (value is now on stack)
				List<PDM.CodeInstr> condCode = whileStmt.cond.accept(this, frame);
                codeInstr.addAll(condCode);

				// First jump: CJMP - if condition == 0, exit loop; if condition != 0, continue to body
				codeInstr.add(new PDM.NAME(bodyLabel, loc));   // addr2 - go to body (condition != 0)
				codeInstr.add(new PDM.NAME(endLabel, loc));    // addr1 - go to end (condition == 0) - EXIT LOOP
				codeInstr.add(new PDM.CJMP(loc));

				// Body label and statements
				codeInstr.add(new PDM.LABEL(bodyLabel, loc));
				for (AST.Stmt stmt : whileStmt.stmts) {
					List<PDM.CodeInstr> stmtCode = stmt.accept(this, frame);
                    codeInstr.addAll(stmtCode);

				}

				// Second jump: UJMP - after body, jump back to start (condition check)
				codeInstr.add(new PDM.NAME(startLabel, loc));
				codeInstr.add(new PDM.UJMP(loc));

				// End label
				codeInstr.add(new PDM.LABEL(endLabel, loc));

				return codeInstr;
			}
			@Override
			public List<PDM.CodeInstr> visit(AST.LetStmt letStmt, Mem.Frame frame) {
				List<PDM.CodeInstr> codeInstr = new ArrayList<>();

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
				List<PDM.CodeInstr> codeInstr = new ArrayList<>();
				Report.Locatable loc = attrAST.attrLoc.get(atomExpr);

				switch (atomExpr.type) {
					case INTCONST -> {
						Integer value = Memory.decodeIntConst(atomExpr, loc);
						codeInstr.add(new PDM.PUSH(value, loc));
					}
					case CHRCONST -> {
						Integer value = Memory.decodeChrConst(atomExpr, loc);
						codeInstr.add(new PDM.PUSH(value, loc));
					}
					case STRCONST -> {
						Vector<Integer> chars = Memory.decodeStrConst(atomExpr, loc);

                        // Generate simple null-terminated string for function calls like putstr
                        String strLabel = generateStringLabel(atomExpr);
                        codeInstr.add(new PDM.NAME(strLabel, loc));

                        List<PDM.DataInstr> strData = new ArrayList<>();
                        strData.add(new PDM.LABEL(strLabel, loc));
                        // Simple format: just characters
                        for (Integer ch : chars) {
                            strData.add(new PDM.DATA(ch, loc));
                        }
                        attrAST.attrData.put(atomExpr, strData);
					}
				}

				return codeInstr;
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
				List<PDM.CodeInstr> codeInstr = new ArrayList<>();
				Report.Locatable loc = attrAST.attrLoc.get(binExpr);

				// Generate code for first operand
				List<PDM.CodeInstr> fstCode = binExpr.fstExpr.accept(this, frame);
                codeInstr.addAll(fstCode);


				// Generate code for second operand
				List<PDM.CodeInstr> sndCode = binExpr.sndExpr.accept(this, frame);
                codeInstr.addAll(sndCode);


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

				codeInstr.add(new PDM.OPER(oper, loc));
				return codeInstr;
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.VarExpr varExpr, Mem.Frame frame) {
				List<PDM.CodeInstr> codeInstr = new ArrayList<>();
				Report.Locatable loc = attrAST.attrLoc.get(varExpr);


                // Generate address calculation
                codeInstr.addAll(generateAddress(varExpr, frame));

                // If variable is left value - get only address
                if (attrAST.attrLVal.get(varExpr) != Boolean.TRUE) {
                    // Is left value - LOAD
                    codeInstr.add(new PDM.LOAD(loc));
                }

                return codeInstr;
			}

            // Special handling of string variables - return address insted of value
            private boolean isStringVariable(AST.VarExpr varExpr) {
                // Get the definition of this variable
                AST.Def definition = attrAST.attrDef.get(varExpr);

                if (definition instanceof AST.VarDef varDef) {
                    // Check if any of the initializers are string constants
                    for (AST.Init init : varDef.inits) {
                        if (init.value.type == AST.AtomExpr.Type.STRCONST) {
                            return true;
                        }
                    }
                }

                return false;
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

                // Get the called function's definition and frame
                AST.FunDef calledFunDef = (AST.FunDef)attrAST.attrDef.get(callExpr);
                Mem.Frame calledFunFrame = attrAST.attrFrame.get(calledFunDef);

                // 2. Set Static Link
                // Set current frame pointer
                codeInstr.add(new PDM.REGN(PDM.REGN.Reg.FP, loc));

                // Follow static links up to the lexical parent of the called function
                int currentDepth = frame.depth;             // Caller`s lexical depth
                int targetDepth = calledFunFrame.depth;     // Called function`s lexical depth

                // Correct depth -> calledFun.depth - 1 (parent)
                while (targetDepth <= currentDepth) {
                    codeInstr.add(new PDM.LOAD(loc));  // Follow one static link up
                    currentDepth--;
                }

                // Generate function name
                // Depth of parent frame
                String funName = generateFunctionLabel(calledFunDef, calledFunFrame.depth - 1);
                // 3. Push function address and call
                codeInstr.add(new PDM.NAME(funName, loc));
                codeInstr.add(new PDM.CALL(calledFunFrame, loc));

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