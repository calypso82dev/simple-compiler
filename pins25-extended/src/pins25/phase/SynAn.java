package pins25.phase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import pins25.common.*;

/**
 * Sintaksni analizator.
 */
public class SynAn implements AutoCloseable {

	/** Leksikalni analizator. */
	private final LexAn lexAn;

	/**
	 * Ustvari nov sintaksni analizator.
	 * 
	 * @param srcFileName Ime izvorne datoteke.
	 */
	public SynAn(final String srcFileName) {
		this.lexAn = new LexAn(srcFileName);
	}

	@Override
	public void close() {
		lexAn.close();
	}

	/**
	 * Prevzame token od leksikalnega analizatorja in preveri, ali
	 * je prave vrste.
	 * 
	 * @param symbol Pricakovana vrsta leksikalnega simbola.
	 * @return Prevzeti leksikalni simbol.
	 */
	private Token check(Token.Symbol symbol) {
		final Token token = lexAn.takeToken();
		if (token.symbol() != symbol)
			throw new Report.Error(
				token.location(),
				"Syntax error: Unexpected symbol '" + token.lexeme() + "'."
			);
		return token;
	}
	private Token check(Token.Symbol symbol, String expectedMsg) {
		final Token token = lexAn.takeToken();
		if (token.symbol() != symbol)
			raiseSyntaxError(token, expectedMsg);
		return token;
	}

	private void raiseSyntaxError(Token token, String expectedMsg) {
		String tokenDisplay;
		if (token.symbol() == Token.Symbol.EOF) {
			tokenDisplay = "end of file";
		} else {
			tokenDisplay = "'" + token.lexeme() + "'";
		}
		throw new Report.Error(
			token.location(),
			"Syntax error: " + expectedMsg + " found " + tokenDisplay
		);
	}

	private Report.Locatable getExprLocation(AST.Expr expr)
	{
		return this.attrLoc.get(expr);
	}
	private Report.Locatable getExprLocation(AST.Expr exprL, AST.Expr exprR)
	{
		Report.Locatable startLoc = this.attrLoc.get(exprL);
		Report.Locatable endLoc = this.attrLoc.get(exprR);
		return new Report.Location(startLoc, endLoc);
	}

	/**
	 * Syntax analysis - return tree
	 */
	private HashMap<AST.Node, Report.Locatable> attrLoc;

	public AST.Node parse(HashMap<AST.Node, Report.Locatable> attrLoc) {
		this.attrLoc = attrLoc;
		final AST.Nodes<AST.MainDef> defs = parseProgram();
		defs.toString();
		if (lexAn.peekToken().symbol() != Token.Symbol.EOF)
			Report.warning(lexAn.peekToken(),
				"Unexpected text '" + lexAn.peekToken().lexeme() + "...' at the end of the program.");
		return defs;
	}

	private AST.Nodes<AST.MainDef> parseProgram() {
		// prog -> def prog2

		List<AST.MainDef> defs = new ArrayList<AST.MainDef>();

		AST.MainDef def = parseDef();
		defs.add(def);
		parseProg2(defs);

		return new AST.Nodes<AST.MainDef>(defs);
	}

	private void parseProg2(List<AST.MainDef> defsL) {
		// prog2 -> def prog2 | ε
		Token token = lexAn.peekToken();

		switch (token.symbol()) {
			// Check if token could start def
			case Token.Symbol.FUN:
			case Token.Symbol.VAR:
				// Production: prog2 -> def prog2
				AST.MainDef defR = parseDef();    // Parse Definition
				defsL.add(defR);
				parseProg2(defsL);  // Parse Program2
				break;
			case Token.Symbol.EOF:
				// Production: prog2 -> ε
				// End of file reached, empty production is correct
				break;
			default:
				// Unexpected token - can't apply any production
				raiseSyntaxError(token, "Expected definition ('fun' or 'var') or end of file");
		}
	}

	private AST.MainDef parseDef()
	{
		// def -> fun id lpar params rpar def2
		// def -> var id equ inits
		Token token = lexAn.peekToken();
		Report.Locatable startLoc, endLoc;

		AST.MainDef mainDef = null;

		switch (token.symbol())
		{
			case Token.Symbol.FUN: // fun id lpar params rpar def2
				// Production: def -> fun id lpar params rpar def2
				startLoc = check(Token.Symbol.FUN); // consume 'fun'
				// expected: id
				Token funId = check(Token.Symbol.IDENTIFIER, "Expected identifier after 'fun'");
				// expected: lparen
				check(Token.Symbol.LPAREN, "Expected '(' after function identifier");
				List<AST.ParDef> pars = parseParams(); // Parse parameters
				// expected: rparen
				endLoc = check(Token.Symbol.RPAREN, "Expected ')' after parameters");
				List<AST.Stmt> stmts = parseDef2(); // Parse definitions2
				mainDef = new AST.FunDef(funId.lexeme(), pars, stmts);
				// Add attribute
				if (!stmts.isEmpty()) {
					AST.Stmt lastStmt = stmts.getLast();
					endLoc = this.attrLoc.get(lastStmt);
				}
				this.attrLoc.put(mainDef, new Report.Location(startLoc, endLoc));
				break;
			case Token.Symbol.VAR: // var id equ inits
				// Production: def -> var id equ inits
				startLoc = check(Token.Symbol.VAR); // consume 'var'
				// expected: id
				Token varId = check(Token.Symbol.IDENTIFIER, "Expected identifier after 'var'");
				// expected: assign
				endLoc = check(Token.Symbol.ASSIGN, "Expected '=' after variable identifier");
				List<AST.Init> inits = parseInits(); // Parse Initializers
				mainDef = new AST.VarDef(varId.lexeme(), inits);
				if (!inits.isEmpty()) {
					AST.Init lastInit = inits.getLast();
					endLoc = this.attrLoc.get(lastInit);
				}
				this.attrLoc.put(mainDef, new Report.Location(startLoc, endLoc));
				break;

			default: 
				raiseSyntaxError(token, "Expected definition ('fun' or 'var')");
		}
		return mainDef;
	}

	private List<AST.Stmt> parseDef2()
	{
		Token token = lexAn.peekToken();
		// def2 -> assign states | ε

		List<AST.Stmt> stmts = new ArrayList<AST.Stmt>();

		switch (token.symbol())
		{
			case Token.Symbol.ASSIGN: // assign states
				// Production: def2 -> assign states
				check(Token.Symbol.ASSIGN); // consume '='
				stmts = parseStates(); // Parse Statements
				break;
			case Token.Symbol.FUN:
			case Token.Symbol.VAR:
			case Token.Symbol.IN:
			case Token.Symbol.EOF:
				// Empty production
				// Production: def2 -> ε
				break;
			default:
				// Unexpected token - can't apply any production
				raiseSyntaxError(token, "Expected '=' or end of definition");
		}
		return stmts;
	}

	private List<AST.ParDef> parseParams()
	{
		// params -> id params2 | ε
		Token token = lexAn.peekToken();

		List<AST.ParDef> pars = new ArrayList<AST.ParDef>();

		switch (token.symbol())
		{
			case Token.Symbol.IDENTIFIER: // id params2
				// Production: params -> id params2
				Token id = check(Token.Symbol.IDENTIFIER); // consume 'id'
				AST.ParDef parDef = new AST.ParDef(id.lexeme());
				this.attrLoc.put(parDef, id);
				pars.add(parDef);
				parseParams2(pars); // Parse parameters2
				break;
			case Token.Symbol.RPAREN:
				// Empty production
				// Production: params -> ε
				break;
			default:
				// Unexpected token - can't apply any production
				raiseSyntaxError(token, "Expected id or ')'");
		}
		return pars;
	}

	private void parseParams2(List<AST.ParDef> parDefsL) {
		// params2 -> comma id params2 | ε
		Token token = lexAn.peekToken();

		switch (token.symbol())
		{
			case Token.Symbol.COMMA: // comma id params2
				// Production: params2 -> comma id params2
				check(Token.Symbol.COMMA); // consume ','
				// expected: id
				Token id = check(Token.Symbol.IDENTIFIER, "Expected identifier after ','");
				AST.ParDef parDefR = new AST.ParDef(id.lexeme());
				this.attrLoc.put(parDefR, id);
				parDefsL.add(parDefR);
				parseParams2(parDefsL); // Parse Parameters2
				break;
			case Token.Symbol.RPAREN:
				// Empty production
				// Production: params2 -> ε
				break;
			default:
				// Unexpected token - can't apply any production
				raiseSyntaxError(token, "Expected ',' or ')'");
		}
	}

	private List<AST.Stmt> parseStates()
	{
		// states -> state states2

		List<AST.Stmt> stmts = new ArrayList<AST.Stmt>();

		AST.Stmt stmt = parseState(); // Parse Statement
		stmts.add(stmt);
		parseStates2(stmts); // Parse Statements2
		return stmts;
	}

	private void parseStates2(List<AST.Stmt> stmtsL) {
		// states2 -> comma state states2 | ε
		Token token = lexAn.peekToken();

		switch (token.symbol())
		{
			case Token.Symbol.COMMA: // comma state states2
				// Production: states2 -> comma state states2
				check(Token.Symbol.COMMA); // consume ','
				AST.Stmt stmtR = parseState(); // Parse Statement
				stmtsL.add(stmtR);
				parseStates2(stmtsL); // Parse Statements2
				break;
			case Token.Symbol.FUN:
			case Token.Symbol.VAR:
			case Token.Symbol.END:
            case Token.Symbol.UNTIL: // Do Statements (expr) Until expr end
            case Token.Symbol.BREAK: // Break statement (loop only)
			case Token.Symbol.ELSE:
			case Token.Symbol.IN:
			case Token.Symbol.EOF:
				// Empty production
				// Production: states2 -> ε
				break;
			default:
				// Unexpected token - can't apply any production
				raiseSyntaxError(token, "Expected ',' or end of statements");
		}
	}

	private AST.Stmt parseState()
	{
        // state -> break
		// state -> expr state2
		// state -> if expr then states else2 end
		// state -> while expr do states end
        // state -> do states until end
		// state -> let defs2 in states end
		Token token = lexAn.peekToken();
		Report.Locatable startLoc, endLoc;

		AST.Stmt stmt = null;

		switch (token.symbol())
		{
            case Token.Symbol.BREAK:
                startLoc = check(Token.Symbol.BREAK); // consume 'break'
                stmt = new AST.BreakStmt();
                this.attrLoc.put(stmt, new Report.Location(startLoc, startLoc));
                break;

			case Token.Symbol.IF: // if expr then states else2 end
				// Production: state -> if expr then states else2 end
				startLoc = check(Token.Symbol.IF); // consume 'if'
				// Condition expression
				AST.Expr ifCondExpr = parseExpr(); // Parse Expression
				// expected: then
				check(Token.Symbol.THEN, "Expected 'then' after 'if'");
				List<AST.Stmt> thenStmts = parseStates(); // Parse Statements
				List<AST.Stmt> elseStmts = parseElse2(); // Parse Else2
				// expected: end
				endLoc = check(Token.Symbol.END, "Expected 'end' after 'then'");
				stmt = new AST.IfStmt(ifCondExpr, thenStmts, elseStmts);
				this.attrLoc.put(stmt, new Report.Location(startLoc, endLoc));
				break;

			case Token.Symbol.WHILE: // while expr do states end
				// Production: state -> while expr do states end
				startLoc = check(Token.Symbol.WHILE); // consume 'while'
				// Condition expression
				AST.Expr whileCondExpr = parseExpr(); // Parse Expression
				// expected: do
				check(Token.Symbol.DO, "Expected 'do' after 'while'");
				List<AST.Stmt> whileStmts =parseStates(); // Parse Statements
				// expected: end
				endLoc = check(Token.Symbol.END, "Expected 'end' after 'do'");
				stmt = new AST.WhileStmt(whileCondExpr, whileStmts);
				this.attrLoc.put(stmt, new Report.Location(startLoc, endLoc));
				break;

            case Token.Symbol.DO: // do states until expr end
                startLoc = check(Token.Symbol.DO); // consume 'do'
                // Body
                List<AST.Stmt> untilStmts =parseStates(); // Parse Statements
                // expected: until
                check(Token.Symbol.UNTIL, "Expected 'until' after 'do'");
                // Condition expression
                AST.Expr untilCondExpr = parseExpr(); // Parse Expression
                // expected: end
                endLoc = check(Token.Symbol.END, "Expected 'end' after 'until'");
                stmt = new AST.UntilStmt(untilCondExpr, untilStmts);
                this.attrLoc.put(stmt, new Report.Location(startLoc, endLoc));
                break;

			case Token.Symbol.LET: // let defs2 in states end
				// Production: state -> let defs2 in states end
				startLoc = check(Token.Symbol.LET); // consume 'let'
				AST.MainDef letDef = parseDef(); // Parse Definition
				List<AST.MainDef> letDefs = new ArrayList<AST.MainDef>();
				letDefs.add(letDef);
				parseDefs2(letDefs); // Parse Definitions2
				// expected: in
				check(Token.Symbol.IN, "Expected 'in' after 'let'");
				List<AST.Stmt> letStmts = parseStates(); // Parse Statements
				// expected: end
				endLoc = check(Token.Symbol.END, "Expected 'end' after 'in'");
				stmt = new AST.LetStmt(letDefs, letStmts);
				this.attrLoc.put(stmt, new Report.Location(startLoc, endLoc));
				break;

			default: // expr state2
				// Production: state -> expr state2
				AST.Expr expr = parseExpr(); // Parse Expression
				stmt = parseState2(expr); // Parse Statement2
		}
		return stmt;
	}

	private AST.Stmt parseState2(AST.Expr dstExpr) {
		// state2 -> assign expr | ε
		Token token = lexAn.peekToken();
		AST.Stmt stmt = null;

		switch (token.symbol())
		{
			case Token.Symbol.ASSIGN: // assign expr
				// Production: state2 -> assign expr
				check(Token.Symbol.ASSIGN); // consume '='
				AST.Expr srcExpr = parseExpr(); // Parse Expression
				// Assign statement
				stmt = new AST.AssignStmt(dstExpr, srcExpr);
				this.attrLoc.put(stmt, getExprLocation(dstExpr, srcExpr));
				break;
			case Token.Symbol.FUN:
			case Token.Symbol.VAR:
			case Token.Symbol.COMMA:
			case Token.Symbol.ELSE:
            case Token.Symbol.BREAK: // Break statement (loop only)
            case Token.Symbol.UNTIL: // Do Statements (expr) Until expr end
			case Token.Symbol.END:
			case Token.Symbol.IN:
			case Token.Symbol.EOF:
				// Empty production
				// Expression statement
				stmt = new AST.ExprStmt(dstExpr);
				this.attrLoc.put(stmt, getExprLocation(dstExpr));
				// Production: state2 -> ε
				break;
			default:
				// Unexpected token - can't apply any production
				raiseSyntaxError(token, "Expected '=' or end of statement");
		}
		return stmt;
	}

	private List<AST.Stmt> parseElse2()
	{
		// else2 -> else states | ε
		Token token = lexAn.peekToken();
		List<AST.Stmt> elseStmts = new ArrayList<AST.Stmt>();

		switch (token.symbol())
		{
			case Token.Symbol.ELSE: // else states
				// Production: else2 -> else states
				check(Token.Symbol.ELSE); // consume 'else'
				elseStmts = parseStates(); // Parse Expression
				break;
			case Token.Symbol.END:
				// Empty production
				// Production: else2 -> ε
				break;
			default:
				// Unexpected token - can't apply any production
				raiseSyntaxError(token, "Expected 'else' or 'end'");
		}
		return elseStmts;
	}

	private void parseDefs2(List<AST.MainDef> defsL)
	{
		// defs2 -> def defs2 | ε
		Token token = lexAn.peekToken();

		switch (token.symbol())
		{
			case Token.Symbol.FUN:
			case Token.Symbol.VAR:
				// Production: defs2 -> def defs2
				AST.MainDef defR = parseDef();    // Parse Definition
				defsL.add(defR);
				parseDefs2(defsL);  // Parse Definitions2
				break;
			case Token.Symbol.IN:
				// Empty production
				// Production: defs2 -> ε
				break;
			default:
				// Unexpected token - can't apply any production
				raiseSyntaxError(token, "Expected definition ('fun' or 'var') or end of definitions");
		}
	}

	// Operators (by priority - incresing): 
	// Disjunction (||)
	// Konjunction (&&)
	// Comparison (==, !=, <, >, <=, >=) - not asociative
	// Additive (+, -)
	// Multiplication (*, /, %)
	// Prefix (^, +, -, !)
	// Postfix (^)

	private AST.Expr parseExpr() {
		// expr -> or_expr
		return parseOrExpr();
	}
	// OR
	private AST.Expr parseOrExpr()
	{
		// or_expr -> and_expr or_expr2
		AST.Expr exprL = parseAndExpr();
		return parseOrExpr2(exprL);
	}
	private AST.Expr parseOrExpr2(AST.Expr exprL)
	{
		// or_expr2 -> or and_expr or_expr2 | ε
		Token token = lexAn.peekToken();
		AST.Expr expr = null;
		AST.BinExpr binExpr;

		switch (token.symbol())
		{
			case Token.Symbol.OR:  // or and_expr or_expr2
				// Production: or_expr2 -> or and_expr or_expr2
				check(Token.Symbol.OR); // consume operator
				AST.Expr exprR = parseAndExpr();
				binExpr = new AST.BinExpr(AST.BinExpr.Oper.OR, exprL, exprR);
				this.attrLoc.put(binExpr, getExprLocation(exprL, exprR));
				expr = parseOrExpr2(binExpr);
				break;

			case Token.Symbol.RPAREN:
			case Token.Symbol.FUN:
			case Token.Symbol.ASSIGN:
			case Token.Symbol.VAR:
			case Token.Symbol.COMMA:
			case Token.Symbol.THEN:
            case Token.Symbol.BREAK: // Break statement (loop only)
            case Token.Symbol.UNTIL: // Do Statements (expr) Until expr end
			case Token.Symbol.END:
			case Token.Symbol.ELSE:
			case Token.Symbol.DO:
			case Token.Symbol.IN:
			case Token.Symbol.EOF:
				// Empty production
				expr = exprL;
				// Production: or_expr2 -> ε
				break;
			default:
				// Unexpected token - can't apply any production
				raiseSyntaxError(token, "Expected operator or end of expression");
		}
		return expr;
	}
	// AND
	private AST.Expr parseAndExpr() {
		// and_expr -> comp_expr and_expr2 
		AST.Expr exprL = parseCompExpr();
		return parseAndExpr2(exprL);
	}
	private AST.Expr parseAndExpr2(AST.Expr exprL)
	{
		// and_expr2 -> and comp_expr and_expr2 | ε
		Token token = lexAn.peekToken();
		AST.Expr expr = null;
		AST.Expr exprR;
		AST.BinExpr binExpr;

		switch (token.symbol())
		{
			case Token.Symbol.AND:  // and comp_expr and_expr2
				// Production: and_expr2 -> and comp_expr and_expr2
				check(Token.Symbol.AND); // consume operator
				exprR = parseCompExpr();
				binExpr = new AST.BinExpr(AST.BinExpr.Oper.AND, exprL, exprR);
				this.attrLoc.put(binExpr, getExprLocation(exprL, exprR));
				expr = parseAndExpr2(binExpr);
				break;
			case Token.Symbol.OR:
			case Token.Symbol.RPAREN:
			case Token.Symbol.FUN:
			case Token.Symbol.ASSIGN:
			case Token.Symbol.VAR:
			case Token.Symbol.COMMA:
			case Token.Symbol.THEN:
            case Token.Symbol.UNTIL: // Do Statements (expr) Until expr end
			case Token.Symbol.END:
			case Token.Symbol.ELSE:
			case Token.Symbol.DO:
			case Token.Symbol.IN:
			case Token.Symbol.EOF:
				// Empty production
				expr = exprL;
				// Production: and_expr2 -> ε
				break;
			default:
				// Unexpected token - can't apply any production
				raiseSyntaxError(token, "Expected non comparison operator or end of expression");
		}
		return expr;
	}
	// Comparison
	private AST.Expr parseCompExpr()
	{
		// comp_expr -> addit_expr comp_expr2
		AST.Expr exprL = parseAdditExpr();
		return parseCompExpr2(exprL);
	}
	private AST.Expr parseCompExpr2(AST.Expr exprL)
	{
		// comp_expr2 -> compop addit_expr | ε
		Token token = lexAn.peekToken();
		// compop -> equ | neq | gth | lth | geq | leq .
		AST.Expr expr = null;
		AST.Expr exprR;

		switch (token.symbol())
		{
			case Token.Symbol.EQU:
				// Production: comp_expr2 -> equ addit_expr
				check(Token.Symbol.EQU); // consume operator
				exprR = parseAdditExpr();
				expr = new AST.BinExpr(AST.BinExpr.Oper.EQU, exprL, exprR);
				this.attrLoc.put(expr, getExprLocation(exprL, exprR));
				break;
			case Token.Symbol.NEQ:
				// Production: comp_expr2 -> neq addit_expr
				check(Token.Symbol.NEQ); // consume operator
				exprR = parseAdditExpr();
				expr = new AST.BinExpr(AST.BinExpr.Oper.NEQ, exprL, exprR);
				this.attrLoc.put(expr, getExprLocation(exprL, exprR));
				break;
			case Token.Symbol.GTH:
				// Production: comp_expr2 -> gth addit_expr
				check(Token.Symbol.GTH); // consume operator
				exprR = parseAdditExpr();
				expr = new AST.BinExpr(AST.BinExpr.Oper.GTH, exprL, exprR);
				this.attrLoc.put(expr, getExprLocation(exprL, exprR));
				break;
			case Token.Symbol.LTH:
				// Production: comp_expr2 -> lth addit_expr
				check(Token.Symbol.LTH); // consume operator
				exprR = parseAdditExpr();
				expr = new AST.BinExpr(AST.BinExpr.Oper.LTH, exprL, exprR);
				this.attrLoc.put(expr, getExprLocation(exprL, exprR));
				break;
			case Token.Symbol.GEQ:
				// Production: comp_expr2 -> geq addit_expr
				check(Token.Symbol.GEQ); // consume operator
				exprR = parseAdditExpr();
				expr = new AST.BinExpr(AST.BinExpr.Oper.GEQ, exprL, exprR);
				this.attrLoc.put(expr, getExprLocation(exprL, exprR));
				break;
			case Token.Symbol.LEQ:
				// Production: comp_expr2 -> leq addit_expr
				check(Token.Symbol.LEQ); // consume operator
				exprR = parseAdditExpr();
				expr = new AST.BinExpr(AST.BinExpr.Oper.LEQ, exprL, exprR);
				this.attrLoc.put(expr, getExprLocation(exprL, exprR));
				break;
			case Token.Symbol.OR:
			case Token.Symbol.AND:
			case Token.Symbol.RPAREN:
			case Token.Symbol.FUN:
			case Token.Symbol.ASSIGN:
			case Token.Symbol.VAR:
			case Token.Symbol.COMMA:
			case Token.Symbol.THEN:
            case Token.Symbol.BREAK: // Break statement (loop only)
            case Token.Symbol.UNTIL: // Do Statements (expr) Until expr end
			case Token.Symbol.END:
			case Token.Symbol.ELSE:
			case Token.Symbol.DO:
			case Token.Symbol.IN:
			case Token.Symbol.EOF:
				// Empty production
				expr = exprL;
				// Production: comp_expr2 -> ε
				break;
			default:
				// Unexpected token - can't apply any production
				raiseSyntaxError(token, "Expected operator or end of expression");
		}
		return expr;
	}
	// Addititve operators
	private AST.Expr parseAdditExpr()
	{
		// addit_expr -> multi_expr addit_expr2
		AST.Expr exprL = parseMultiExpr();
		return parseAdditExpr2(exprL);
	}
	private AST.Expr parseAdditExpr2(AST.Expr exprL)
	{
		// addit_expr2 -> additop multi_expr addit_expr2 | ε
		Token token = lexAn.peekToken();
		AST.Expr expr = null;
		AST.Expr exprR;
		AST.BinExpr binExpr;

		// additop -> add | sub
		switch (token.symbol())
		{
			case Token.Symbol.ADD:
				// Production: addit_expr2 -> add multi_expr addit_expr2\
				check(Token.Symbol.ADD); // consume operator
				exprR = parseMultiExpr();
				binExpr = new AST.BinExpr(AST.BinExpr.Oper.ADD, exprL, exprR);
				this.attrLoc.put(binExpr, getExprLocation(exprL, exprR));
				expr = parseAdditExpr2(binExpr);
				break;
			case Token.Symbol.SUB:
				// Production: addit_expr2 -> sub multi_expr addit_expr2
				check(Token.Symbol.SUB); // consume operator
				exprR = parseMultiExpr();
				binExpr = new AST.BinExpr(AST.BinExpr.Oper.SUB, exprL, exprR);
				this.attrLoc.put(binExpr, getExprLocation(exprL, exprR));
				expr = parseAdditExpr2(binExpr);
				break;
			case Token.Symbol.EQU:
			case Token.Symbol.NEQ:
			case Token.Symbol.GTH:
			case Token.Symbol.LTH:
			case Token.Symbol.GEQ:
			case Token.Symbol.LEQ:
			case Token.Symbol.OR:
			case Token.Symbol.AND:
			case Token.Symbol.RPAREN:
			case Token.Symbol.FUN:
			case Token.Symbol.ASSIGN:
			case Token.Symbol.VAR:
			case Token.Symbol.COMMA:
			case Token.Symbol.THEN:
            case Token.Symbol.BREAK: // Break statement (loop only)
            case Token.Symbol.UNTIL: // Do Statements (expr) Until expr end
			case Token.Symbol.END:
			case Token.Symbol.ELSE:
			case Token.Symbol.DO:
			case Token.Symbol.IN:
			case Token.Symbol.EOF:
				// Empty production
				expr = exprL;
				// Production: addit_expr2 -> ε
				break;
			default:
				// Unexpected token - can't apply any production
				raiseSyntaxError(token, "Expected operator or end of expression");
		}
		return expr;
	}
	// Multiplicative operators
	private AST.Expr parseMultiExpr()
	{
		// multi_expr -> pre_expr multi_expr2
		AST.Expr exprL = parsePreExpr();
		return parseMultiExpr2(exprL);
	}

	private AST.Expr parseMultiExpr2(AST.Expr exprL)
	{
		// multi_expr2 -> multiop pre_expr multi_expr2 | ε
		Token token = lexAn.peekToken();
		AST.Expr expr = null;
		AST.Expr exprR;
		AST.BinExpr binExpr;

		// multiop -> mul | div | mod
		switch (token.symbol())
		{
			case Token.Symbol.MUL:
				// Production: multi_expr2 -> mul pre_expr multi_expr2
				check(Token.Symbol.MUL); // consume operator
				exprR = parsePreExpr();
				binExpr = new AST.BinExpr(AST.BinExpr.Oper.MUL, exprL, exprR);
				this.attrLoc.put(binExpr, getExprLocation(exprL, exprR));
				expr = parseMultiExpr2(binExpr);
				break;
			case Token.Symbol.DIV:
				// Production: multi_expr2 -> div pre_expr multi_expr2
				check(Token.Symbol.DIV); // consume operator
				exprR = parsePreExpr();
				binExpr = new AST.BinExpr(AST.BinExpr.Oper.DIV, exprL, exprR);
				this.attrLoc.put(binExpr, getExprLocation(exprL, exprR));
				expr = parseMultiExpr2(binExpr);
				break;
			case Token.Symbol.MOD:
				// Production: multi_expr2 -> mod pre_expr multi_expr2
				check(Token.Symbol.MOD); // consume operator
				exprR = parsePreExpr();
				binExpr = new AST.BinExpr(AST.BinExpr.Oper.MOD, exprL, exprR);
				this.attrLoc.put(binExpr, getExprLocation(exprL, exprR));
				expr = parseMultiExpr2(binExpr);
				break;
			case Token.Symbol.ADD:
			case Token.Symbol.SUB:
			case Token.Symbol.EQU:
			case Token.Symbol.NEQ:
			case Token.Symbol.GTH:
			case Token.Symbol.LTH:
			case Token.Symbol.GEQ:
			case Token.Symbol.LEQ:
			case Token.Symbol.OR:
			case Token.Symbol.AND:
			case Token.Symbol.RPAREN:
			case Token.Symbol.FUN:
			case Token.Symbol.ASSIGN:
			case Token.Symbol.VAR:
			case Token.Symbol.COMMA:
			case Token.Symbol.THEN:
            case Token.Symbol.BREAK: // Break statement (loop only)
            case Token.Symbol.UNTIL: // Do Statements (expr) Until expr end
			case Token.Symbol.END:
			case Token.Symbol.ELSE:
			case Token.Symbol.DO:
			case Token.Symbol.IN:
			case Token.Symbol.EOF:
				// Empty production
				expr = exprL;
				// Production: multi_expr2 -> ε
				break;
			default:
				// Unexpected token - can't apply any production
				raiseSyntaxError(token, "Expected operator or end of expression");
		}
		return expr;
	}
	// Prefix operators
	private AST.Expr parsePreExpr()
	{
		// pre_expr -> prefixop pre_expr | post_expr
		Token token = lexAn.peekToken();
		Report.Locatable startLoc;
		AST.Expr expr, exprR;
		AST.UnExpr unExpr;

		// prefixop -> not | add | sub | ptr
		switch (token.symbol())
		{
			case Token.Symbol.NOT:
				// Production: pre_expr -> not pre_expr
				startLoc = check(Token.Symbol.NOT); // consume operator
				exprR = parsePreExpr(); // Recursively parse prefix operators
				unExpr = new AST.UnExpr(AST.UnExpr.Oper.NOT, exprR);
				this.attrLoc.put(unExpr, new Report.Location(startLoc, getExprLocation(exprR)));
                expr = unExpr;
				break;
			case Token.Symbol.ADD:
				// Production: pre_expr -> add pre_expr
				startLoc = check(Token.Symbol.ADD); // consume operator
				exprR = parsePreExpr(); // Recursively parse prefix operators
				unExpr = new AST.UnExpr(AST.UnExpr.Oper.ADD, exprR);
				this.attrLoc.put(unExpr, new Report.Location(startLoc, getExprLocation(exprR)));
                expr = unExpr;
				break;
			case Token.Symbol.SUB:
				// Production: pre_expr -> sub pre_expr
				startLoc = check(Token.Symbol.SUB); // consume operator
				exprR = parsePreExpr(); // Recursively parse prefix operators
				unExpr = new AST.UnExpr(AST.UnExpr.Oper.SUB, exprR);
				this.attrLoc.put(unExpr, new Report.Location(startLoc, getExprLocation(exprR)));
                expr = unExpr;
				break;
			case Token.Symbol.PTR:
				// Production: pre_expr -> ptr pre_expr
				startLoc = check(Token.Symbol.PTR); // consume operator
				exprR = parsePreExpr(); // Recursively parse prefix operators
				unExpr = new AST.UnExpr(AST.UnExpr.Oper.MEMADDR, exprR);
				this.attrLoc.put(unExpr, new Report.Location(startLoc, getExprLocation(exprR)));
                expr = unExpr;
				break;
			default:
				// Production: pre_expr -> post_expr
				expr = parsePostExpr();
		}
		return expr;
	}

	// Postfix operators
	private AST.Expr parsePostExpr()
	{
		// post_expr -> primary post_expr2
		AST.Expr exprL = parsePrimary();
		return parsePostExpr2(exprL);
	}
	private AST.Expr parsePostExpr2(AST.Expr exprL)
	{
		// post_expr2 -> postfixop post_expr2 | ε
		Token token = lexAn.peekToken();
		AST.Expr expr = null;

		// postfixop -> ptr
		switch (token.symbol())
		{
			case Token.Symbol.PTR:
				// Production: post_expr2 -> ptr post_expr2
				Token endLoc = check(Token.Symbol.PTR); // consume operator
				AST.UnExpr unExpr = new AST.UnExpr(AST.UnExpr.Oper.VALUEAT, exprL);
				this.attrLoc.put(unExpr, new Report.Location(getExprLocation(exprL), endLoc));
				expr = parsePostExpr2(unExpr); // Recursively parse postfix operators
				break;


			case Token.Symbol.MUL:
			case Token.Symbol.DIV:
			case Token.Symbol.MOD:
			case Token.Symbol.ADD:
			case Token.Symbol.SUB:
			case Token.Symbol.EQU:
			case Token.Symbol.NEQ:
			case Token.Symbol.GTH:
			case Token.Symbol.LTH:
			case Token.Symbol.GEQ:
			case Token.Symbol.LEQ:
			case Token.Symbol.OR:
			case Token.Symbol.AND:
			case Token.Symbol.RPAREN:
			case Token.Symbol.FUN:
			case Token.Symbol.ASSIGN:
			case Token.Symbol.VAR:
			case Token.Symbol.COMMA:
			case Token.Symbol.THEN:
            case Token.Symbol.BREAK: // Break statement (loop only)
            case Token.Symbol.UNTIL: // Do Statements (expr) Until expr end
			case Token.Symbol.END:
			case Token.Symbol.ELSE:
			case Token.Symbol.DO:
			case Token.Symbol.IN:
			case Token.Symbol.EOF:
				// Empty production
				expr = exprL;
				// Production: post_expr2 -> ε
				break;
			default:
				// Unexpected token - can't apply any production
				raiseSyntaxError(token, "Expected operator or end of expression");
		}
		return expr;
	}
	// Primary 
	private AST.Expr parsePrimary()
	{
		// primary -> int | char | string
		// primary -> lpar expr rpar
		// primary -> id opt_args
		Token token = lexAn.peekToken();
		Token value;
		AST.Expr expr = null;

		switch (token.symbol())
		{
			case Token.Symbol.INTCONST:
				// Production: primary -> int
				value = check(Token.Symbol.INTCONST); // consume const
				expr = new AST.AtomExpr(AST.AtomExpr.Type.INTCONST, value.lexeme());
				this.attrLoc.put(expr, value);
				break;
			case Token.Symbol.CHARCONST:
				// Production: primary -> char
				value = check(Token.Symbol.CHARCONST); // consume const
				expr = new AST.AtomExpr(AST.AtomExpr.Type.CHRCONST, value.lexeme());
				this.attrLoc.put(expr, value);
				break;
			case Token.Symbol.STRINGCONST:
				// Production: primary -> string
				value = check(Token.Symbol.STRINGCONST); // consume const
				expr = new AST.AtomExpr(AST.AtomExpr.Type.STRCONST, value.lexeme());
				this.attrLoc.put(expr, value);
				break;
			case Token.Symbol.LPAREN:
				// Production: primary -> lpar expr rpar
				Token start = check(Token.Symbol.LPAREN); // consume '('
				expr = parseExpr(); // Parse Expression
				Token end = check(Token.Symbol.RPAREN, "Expected ')' after '('");
				// Adjust expr location to: start on '(', end on ')'
				this.attrLoc.put(expr, new Report.Location(start, end));
				break;

			case Token.Symbol.IDENTIFIER:
				// Production: primary -> id opt_args
				Token id = check(Token.Symbol.IDENTIFIER); // consume id
				List<AST.Expr> args = parseOptArgs(); // Parse Optinal args
				// If optional args != null -> function call 
				// If optional args == null -> var or par access
				if (args == null) {
					expr = new AST.VarExpr(id.lexeme());
					this.attrLoc.put(expr, id);
				} else {
					expr = new AST.CallExpr(id.lexeme(), args);
					Report.Location startLoc = id.location();
					Report.Location endLoc;
					if (args.isEmpty()) {
						// fun() --> +2 '()'
						endLoc = new Report.Location(startLoc.endLine(), startLoc.endColumn() + 2);
						this.attrLoc.put(expr, new Report.Location(startLoc, endLoc));
					} else {
						// fun(par1, par2...) --> +1 ')'
						Report.Location lastParLoc = this.attrLoc.get(args.getLast()).location();
						endLoc = new Report.Location(lastParLoc.endLine(), lastParLoc.endColumn() + 1);
						this.attrLoc.put(expr, new Report.Location(startLoc, endLoc));
					}
				}
				break;
			default:
				raiseSyntaxError(token, "Expected constant (integer, character, string), identifier, or '('");
		}
		return expr;
	}
	// Optional args
	private List<AST.Expr> parseOptArgs()
	{
		// opt_args -> lpar args rpar | ε
		List<AST.Expr> args = null;

		Token token = lexAn.peekToken();
		switch (token.symbol())
		{
			case Token.Symbol.LPAREN:
				// Production: opt_args -> lpar args rpar
				check(Token.Symbol.LPAREN); // consume '('
				args = parseArgs(); // Parse Arguments
				check(Token.Symbol.RPAREN, "Expected ')' after '('");
				break;
			case Token.Symbol.MUL:
			case Token.Symbol.DIV:
			case Token.Symbol.MOD:
			case Token.Symbol.ADD:
			case Token.Symbol.SUB:
			case Token.Symbol.PTR:
			case Token.Symbol.EQU:
			case Token.Symbol.NEQ:
			case Token.Symbol.GTH:
			case Token.Symbol.LTH:
			case Token.Symbol.GEQ:
			case Token.Symbol.LEQ:
			case Token.Symbol.OR:
			case Token.Symbol.AND:
			case Token.Symbol.RPAREN:
			case Token.Symbol.FUN:
			case Token.Symbol.ASSIGN:
			case Token.Symbol.VAR:
			case Token.Symbol.COMMA:
			case Token.Symbol.THEN:
            case Token.Symbol.BREAK: // Break statement (loop only)
            case Token.Symbol.UNTIL: // Do Statements (expr) Until expr end
			case Token.Symbol.END:
			case Token.Symbol.ELSE:
			case Token.Symbol.DO:
			case Token.Symbol.IN:
			case Token.Symbol.EOF:
				// Empty production
				args = null;
				// Production: opt_args -> ε
				break;
			default:
				// Unexpected token - can't apply any production
				raiseSyntaxError(token, "Expected '(' for optional arguemnts");
		}
		return args;
	}

	// Arguments
	private List<AST.Expr> parseArgs()
	{
		// args -> expr args2 | ε
		Token token = lexAn.peekToken();

		List<AST.Expr> args = new ArrayList<AST.Expr>();

		switch (token.symbol())
		{
			case Token.Symbol.RPAREN:
				// Empty production
				// Production: args -> ε
				break;
			default:
				// Production: args -> expr args2
				AST.Expr arg = parseExpr();
				args.add(arg);
				parseArgs2(args);
		}
		return args;
	}

	private void parseArgs2(List<AST.Expr> argsL) {
		// args2 -> comma expr args2 | ε
		Token token = lexAn.peekToken();

		switch (token.symbol())
		{
			case Token.Symbol.COMMA: // comma expr args2
				// Production: args2 -> comma expr args2
				check(Token.Symbol.COMMA); // consume ','
				AST.Expr exprR = parseExpr(); // Parse Expressiom
				argsL.add(exprR);
				parseArgs2(argsL); // Parse Arguments2
				break;
			case Token.Symbol.RPAREN:
				// Empty production
				// Production: args2 -> ε
				break;
			default:
				// Unexpected token - can't apply any production
				raiseSyntaxError(token, "Expected ',' or end of arguments");
		}
	}
	// Initializers
	private List<AST.Init> parseInits()
	{
		// inits -> init inits2 | ε
		Token token = lexAn.peekToken();

		List<AST.Init> inits = new ArrayList<AST.Init>();

		switch (token.symbol())
		{
			case Token.Symbol.FUN:
			case Token.Symbol.VAR:
			case Token.Symbol.IN:
			case Token.Symbol.EOF:
				// Empty production
				// Production: inits -> ε
				break;
			default:
				// Production: inits -> init inits2
				AST.Init init = parseInit();
				inits.add(init);
				parseInits2(inits);
		}

		return inits;
	}

	private void parseInits2(List<AST.Init> initsL) {
		// inits2 -> comma init inits2 | ε
		Token token = lexAn.peekToken();

		switch (token.symbol())
		{
			case Token.Symbol.COMMA: // comma init 
				// Production: inits2 -> comma init inits2
				check(Token.Symbol.COMMA); // consume ','
				AST.Init initR = parseInit(); // Parse Initializer
				initsL.add(initR);
				parseInits2(initsL); // Parse Initializers2
				break;
			case Token.Symbol.FUN:
			case Token.Symbol.VAR:
			case Token.Symbol.IN:
			case Token.Symbol.EOF:
				// Empty production
				// Production: inits2 -> ε
				break;
			default:
				// Unexpected token - can't apply any production
				raiseSyntaxError(token, "Expected ',' or end of initializers");
		}
	}
	// Initializer
	private AST.Init parseInit()
	{
		// init -> int mul_const_expr | string | char
		Token token = lexAn.peekToken();
		Token value;
		AST.AtomExpr atomExprVal = null;
		// Value only -> 1 * value
		AST.AtomExpr atomExprNum = new AST.AtomExpr(AST.AtomExpr.Type.INTCONST, "1");

		switch (token.symbol()) {
			case Token.Symbol.INTCONST:
				// Production: init -> int mul_const_expr
				value = check(Token.Symbol.INTCONST); // consume INTCONST
				AST.AtomExpr intExpr = new AST.AtomExpr(AST.AtomExpr.Type.INTCONST, value.lexeme());
				AST.AtomExpr mulConstExpr = parseMulConstExpr();
				if (mulConstExpr != null) {
					// mulConstExpr = constant, set Num to value
					atomExprNum = intExpr;
					atomExprVal = mulConstExpr;
					this.attrLoc.put(atomExprNum, value);
					this.attrLoc.put(atomExprVal, this.attrLoc.get(mulConstExpr));

				} else {
					// mulConstExpr = empty, Set Value to int const, Num = 1
					atomExprVal = intExpr;
					this.attrLoc.put(atomExprNum, value);
					this.attrLoc.put(atomExprVal, value);
				}
				break;
			case Token.Symbol.CHARCONST:
				// Production: init -> char
				value = check(Token.Symbol.CHARCONST); // consume CHARCONST
				atomExprVal = new AST.AtomExpr(AST.AtomExpr.Type.CHRCONST, value.lexeme());
				this.attrLoc.put(atomExprNum, value);
				this.attrLoc.put(atomExprVal, value);
				break;
			case Token.Symbol.STRINGCONST:
				// Production: init -> string
				value = check(Token.Symbol.STRINGCONST); // consume STRINGCONST
				atomExprVal = new AST.AtomExpr(AST.AtomExpr.Type.STRCONST, value.lexeme());
				this.attrLoc.put(atomExprNum, value);
				this.attrLoc.put(atomExprVal, value);
				break;
			default:
				raiseSyntaxError(token, "Expected an initializer (integer, character, or string)");
		}
		AST.Init init = new AST.Init(atomExprNum, atomExprVal);
		this.attrLoc.put(init, getExprLocation(atomExprNum, atomExprVal));
		return init;
	}

	private AST.AtomExpr parseMulConstExpr() {
		// mul_const_expr -> mul const | ε
		Token token = lexAn.peekToken();
		AST.AtomExpr atomExpr = null;

		switch (token.symbol())
		{
			case Token.Symbol.MUL: //  mul const
				// Production: mul_const_expr -> mul const
				check(Token.Symbol.MUL); // consume mul
				atomExpr = parseConst();
				break;
			case Token.Symbol.FUN:
			case Token.Symbol.VAR:
			case Token.Symbol.IN:
			case Token.Symbol.COMMA:
			case Token.Symbol.EOF:
				// Empty production
				atomExpr = null;
				// Production: mul_const_expr -> ε
				break;
			default:
				// Unexpected token - can't apply any production
				raiseSyntaxError(token, "Expected '*' or end of var initialization");
		}
		return atomExpr;
	}
	// Const 
	private AST.AtomExpr parseConst() {
		// cosnt -> int | char | string
		Token token = lexAn.peekToken();
		AST.AtomExpr atomExpr = null;
		Token value;

		switch (token.symbol())
		{
			case Token.Symbol.INTCONST:
				// Production: const -> int
				value = check(Token.Symbol.INTCONST); // consume INTCONST
				atomExpr = new AST.AtomExpr(AST.AtomExpr.Type.INTCONST, value.lexeme());
				this.attrLoc.put(atomExpr, value);
				break;
			case Token.Symbol.CHARCONST:
				// Production: const -> char
				value = check(Token.Symbol.CHARCONST); // consume const
				atomExpr = new AST.AtomExpr(AST.AtomExpr.Type.CHRCONST, value.lexeme());
				this.attrLoc.put(atomExpr, value);
				break;
			case Token.Symbol.STRINGCONST:
				// Production: const -> string
				value = check(Token.Symbol.STRINGCONST); // consume const
				atomExpr = new AST.AtomExpr(AST.AtomExpr.Type.STRCONST, value.lexeme());
				this.attrLoc.put(atomExpr, value);
				break;
			default:
				raiseSyntaxError(token, "Expected constant (integer, character, string)");
		}
		return atomExpr;
	}

	// --- ZAGON ---

	/**
	 * Zagon sintaksnega analizatorja kot samostojnega programa.
	 * 
	 * @param cmdLineArgs Argumenti v ukazni vrstici.
	 */
	public static void main(final String[] cmdLineArgs) {
		System.out.println("This is PINS'25 compiler (syntax analysis):");

		try {
			if (cmdLineArgs.length == 0)
				throw new Report.Error("No source file specified in the command line.");
			if (cmdLineArgs.length > 1)
				Report.warning("Unused arguments in the command line.");

			try (SynAn synAn = new SynAn(cmdLineArgs[0])) {
				HashMap<AST.Node, Report.Locatable> attrLoc = new HashMap<AST.Node, Report.Locatable>();
				synAn.parse(attrLoc);
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
