package pins25.phase;

import java.util.*;

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
		throw new Report.Error(
			token.location(),
			"Syntax error: " + expectedMsg + ", found '" + token.lexeme() + "'"
		);
	}

	/**
	 * Opravi sintaksno analizo.
	 */
	public void parse() {
		// Parse program
		parseProg();

		// End of file
		if (lexAn.peekToken().symbol() != Token.Symbol.EOF) {
			throw new Report.Error(lexAn.peekToken(),
				"Unexpected text '" + lexAn.peekToken().lexeme() + "...' at the end of the program.");
		}

	}

	private void parseProg() {
		System.out.printf("Parsing: Program\n");
		// prog -> def prog2
		System.out.printf("  Production: prog -> def prog2\n");
		parseDef();
		parseProg2();
	}

	private void parseProg2() {
		System.out.printf("Parsing: Program2\n");
		// prog2 -> def prog2 | ε
		Token token = lexAn.peekToken();

		switch (token.symbol()) {
			// Check if token could start def
			case Token.Symbol.FUN:
			case Token.Symbol.VAR:
				System.out.printf("  Production: prog2 -> def prog2\n");
				parseDef();    // Parse Definition
				parseProg2();  // Parse Program2
				break;
				
			case Token.Symbol.EOF:
				System.out.printf("  Production: prog2 -> ε\n");
				// End of file reached, empty production is correct
				break;
				
			default:
				// Unexpected token - can't apply any production
				raiseSyntaxError(token, "Expected 'fun', 'var', or end of file");
		}
	}

	private void parseDef()
	{
		System.out.printf("Parsing: Definition\n");
		// def -> fun id lpar params rpar def2
		// def -> var id equ inits
		Token token = lexAn.peekToken();

		switch (token.symbol())
		{
			case Token.Symbol.FUN: // fun id lpar params rpar def2
				System.out.printf("  Production: def -> fun id lpar params rpar def2\n");
				check(Token.Symbol.FUN); // consume 'fun'
				// expected: id
				check(Token.Symbol.IDENTIFIER, "Expected identifier after 'fun'");
				// expected: lparen
				check(Token.Symbol.LPAREN, "Expected '(' after function identifier");
				parseParams(); // Parse parameters
				// expected: rparen
				check(Token.Symbol.RPAREN, "Expected ')' after parameters");
				parseDef2(); // Parse definitions2
				break;

			case Token.Symbol.VAR: // var id equ inits
				System.out.printf("  Production: def -> var id equ inits\n");
				check(Token.Symbol.VAR); // consume 'var'
				// expected: id
				check(Token.Symbol.IDENTIFIER, "Expected identifier after 'var'");
				// expected: assign
				check(Token.Symbol.ASSIGN, "Expected '=' after variable identifier");
				parseInits(); // Parse Initializers
				break;

			default: 
				raiseSyntaxError(token, "Expected 'fun' or 'var'");
		}
	}

	private void parseDef2() {
		System.out.printf("Parsing: Definition2\n");
		Token token = lexAn.peekToken();
		// def2 -> assign states | ε

		switch (token.symbol())
		{
			case Token.Symbol.ASSIGN: // assign states
				System.out.printf("  Production: def2 -> assign states\n");
				check(Token.Symbol.ASSIGN); // consume '='
				parseStates(); // Parse Statements
				break;

			case Token.Symbol.EOF:
				System.out.printf("  Production: def2 -> ε\n");
				// End of file reached, empty production is correct
				break;
				
			default:
				// Unexpected token - can't apply any production
				raiseSyntaxError(token, "Expected '=' or end of file");
		}
	}

	private void parseParams()
	{
		System.out.printf("Parsing: Parameters\n");
		// params -> id params2 | ε
		Token token = lexAn.peekToken();

		switch (token.symbol())
		{
			case Token.Symbol.IDENTIFIER: // id params2
				System.out.printf("  Production: params -> id params2\n");
				check(Token.Symbol.IDENTIFIER); // consume 'id'
				parseParams2(); // Parse parameters2
				break;
			
			case Token.Symbol.EOF:
				System.out.printf("  Production: params -> ε\n");
				// End of file reached, empty production is correct
				break;
				
			default:
				// Unexpected token - can't apply any production
				raiseSyntaxError(token, "Expected identifier or end of file");		
		}
	}

	private void parseParams2() {
		System.out.printf("Parsing: Parameters2\n");
		// params2 -> comma id params2 | ε
		Token token = lexAn.peekToken();

		switch (token.symbol())
		{
			case Token.Symbol.COMMA: // comma id params2
				System.out.printf("  Production: params2 -> comma id params2\n");
				lexAn.takeToken(); // consume ','
				// expected: id
				check(Token.Symbol.IDENTIFIER, "Expected identifier after ','");
				parseParams2(); // Parse Parameters2
				break;

			default:
				System.out.printf("  Production: params2 -> ε\n");
				// it's epsilon (empty) production
		}
	}

	private void parseStates()
	{
		System.out.printf("Parsing: Statements\n");
		// states -> state states2
		System.out.printf("  Production: states -> state states2\n");
		parseState(); // Parse Statement
		parseStates2(); // Parse Statements2
	}

	private void parseStates2() {
		System.out.printf("Parsing: Statements2\n");
		// states2 -> comma state states2 | ε
		Token token = lexAn.peekToken();

		switch (token.symbol())
		{
			case Token.Symbol.COMMA: // comma state states2
				System.out.printf("  Production: states2 -> comma state states2\n");
				lexAn.takeToken(); // consume ','
				parseState(); // Parse Statement
				parseStates2(); // Parse Statements2
				break;

			default:
				System.out.printf("  Production: states2 -> ε\n");
				// it's epsilon (empty) production
		}
	}

	private void parseState()
	{
		System.out.printf("Parsing: Statement\n");
		// state -> expr state2
		// state -> if expr then states else2 end
		// state -> while expr do states end
		// state -> let defs2 in states end
		Token token = lexAn.peekToken();

		switch (token.symbol())
		{
			case Token.Symbol.IF: // if expr then states else2 end
				System.out.printf("  Production: state -> if expr then states else2 end\n");
				lexAn.takeToken(); // consume 'if'
				parseExpr(); // Parse Expression
				// expected: then
				check(Token.Symbol.THEN, "Expected 'then' after 'if'");
				parseStates(); // Parse Statements
				parseElse2(); // Parse Else2
				// expected: end
				check(Token.Symbol.END, "Expected 'end' after 'then'");
				break;

			case Token.Symbol.WHILE: // while expr do states end
				System.out.printf("  Production: state -> while expr do states end\n");
				lexAn.takeToken(); // consume 'while'
				parseExpr(); // Parse Expression
				// expected: do
				check(Token.Symbol.DO, "Expected 'do' after 'while'");
				parseStates(); // Parse Statements
				// expected: end
				check(Token.Symbol.END, "Expected 'end' after 'do'");
				break;

			case Token.Symbol.LET: // let defs2 in states end
				System.out.printf("  Production: state -> let defs2 in states end\n");
				lexAn.takeToken(); // consume 'let'
				parseDefs2(); // Parse Definitions2
				// expected: in
				check(Token.Symbol.IN, "Expected 'in' after 'let'");
				parseStates(); // Parse Statements
				// expected: end
				check(Token.Symbol.END, "Expected 'end' after 'in'");
				break;

			default: // expr state2
				System.out.printf("  Production: state -> expr state2\n");
				parseExpr(); // Parse Expression
				parseState2(); // Parse Statement2
		}
	}

	private void parseState2() {
		System.out.printf("Parsing: Statement2\n");
		// state2 -> assign expr | ε
		Token token = lexAn.peekToken();

		switch (token.symbol())
		{
			case Token.Symbol.ASSIGN: // assign expr
				System.out.printf("  Production: state2 -> assign expr\n");
				lexAn.takeToken(); // consume '='
				parseExpr(); // Parse Expression
				break;

			default:
				System.out.printf("  Production: state2 -> ε\n");
				// it's epsilon (empty) production
		}
	}

	private void parseElse2() {
		System.out.printf("Parsing: Else2\n");
		// else2 -> else states | ε
		Token token = lexAn.peekToken();

		switch (token.symbol())
		{
			case Token.Symbol.ELSE: // else states
				System.out.printf("  Production: else2 -> else states\n");
				lexAn.takeToken(); // consume 'else'
				parseStates(); // Parse Expression
				break;

			default:
				System.out.printf("  Production: else2 -> ε\n");
				// it's epsilon (empty) production
		}
	}

	private void parseDefs2() {
		System.out.printf("Parsing: Definitions2\n");
		// defs2 -> def defs2 | ε
		Token token = lexAn.peekToken();

		if (couldStartDefinition(token.symbol())) {
			System.out.printf("  Production: defs2 -> def defs2\n");
			parseDef();    // Parse Definition
			parseDefs2();  // Parse Definitions2
		} else {
			System.out.printf("  Production: defs2 -> ε\n");
			// Otherwise, it's epsilon (empty) production, so do nothing
		}
	}
	private boolean couldStartDefinition(Token.Symbol symbol) {
		return symbol == Token.Symbol.FUN || symbol == Token.Symbol.VAR;
	}

	// Operators (by priority - incresing): 
	// Disjunction (||)
	// Konjunction (&&)
	// Comparison (==, !=, <, >, <=, >=) - not asociative
	// Additive (+, -)
	// Multiplication (*, /, %)
	// Prefix (^, +, -, !)
	// Postfix (^)

	private void parseExpr() {
		System.out.printf("Parsing: Expression\n");
		// expr -> or_expr
		System.out.printf("  Production: expr -> or_expr\n");
		parseOrExpr();
	}
	// OR
	private void parseOrExpr() {
		System.out.printf("Parsing: OrExpression\n");
		// or_expr -> and_expr or_expr2
		System.out.printf("  Production: or_expr -> and_expr or_expr2\n");
		parseAndExpr();
		parseOrExpr2();
	}
	private void parseOrExpr2() {
		System.out.printf("Parsing: OrExpression2\n");
		// or_expr2 -> or and_expr or_expr2 | ε
		Token token = lexAn.peekToken();
		switch (token.symbol())
		{
			case Token.Symbol.OR:  // or and_expr or_expr2
				System.out.printf("  Production: or_expr2 -> or and_expr or_expr2\n");
				lexAn.takeToken(); // consume operator
				System.out.printf("    Operator: ||\n");
				parseAndExpr();
				parseOrExpr2();
				break;

			default:
				System.out.printf("  Production: or_expr2 -> ε\n");
				// it's epsilon (empty) production
		}
	}
	// AND
	private void parseAndExpr() {
		System.out.printf("Parsing: AndExpression\n");
		// and_expr -> comp_expr and_expr2 
		System.out.printf("  Production: and_expr -> comp_expr and_expr2\n");
		parseCompExpr();
		parseAndExpr2();
	}
	private void parseAndExpr2() {
		System.out.printf("Parsing: AndExpression2\n");
		// and_expr2 -> and comp_expr and_expr2 | ε
		Token token = lexAn.peekToken();
		switch (token.symbol())
		{
			case Token.Symbol.AND:  // and comp_expr and_expr2
				System.out.printf("  Production: and_expr2 -> and comp_expr and_expr2\n");
				lexAn.takeToken(); // consume operator
				System.out.printf("    Operator: &&\n");
				parseCompExpr();
				parseAndExpr2();
				break;

			default:
				System.out.printf("  Production: and_expr2 -> ε\n");
				// it's epsilon (empty) production
		}
	}
	// Comparison
	private void parseCompExpr() {
		System.out.printf("Parsing: ComparisonExpression\n");
		// comp_expr -> addit_expr comp_expr2
		System.out.printf("  Production: comp_expr -> addit_expr comp_expr2\n");
		parseAdditExpr();
		parseCompExpr2();
	}
	private void parseCompExpr2() {
		System.out.printf("Parsing: ComparisonExpression2\n");
		// comp_expr2 -> compop addit_expr | ε
		Token token = lexAn.peekToken();
		// compop -> equ | neq | gth | lth | geq | leq .
		switch (token.symbol())
		{
			case Token.Symbol.EQU:
				System.out.printf("  Production: comp_expr2 -> equ addit_expr\n");
				lexAn.takeToken(); // consume operator
				parseAdditExpr();
				break;
			case Token.Symbol.NEQ:
				System.out.printf("  Production: comp_expr2 -> neq addit_expr\n");
				lexAn.takeToken(); // consume operator
				parseAdditExpr();
				break;
			case Token.Symbol.GTH:
				System.out.printf("  Production: comp_expr2 -> gth addit_expr\n");
				lexAn.takeToken(); // consume operator
				parseAdditExpr();
				break;
			case Token.Symbol.LTH:
				System.out.printf("  Production: comp_expr2 -> lth addit_expr\n");
				lexAn.takeToken(); // consume operator
				parseAdditExpr();
				break;
			case Token.Symbol.GEQ:
				System.out.printf("  Production: comp_expr2 -> geq addit_expr\n");
				lexAn.takeToken(); // consume operator
				parseAdditExpr();
				break;
			case Token.Symbol.LEQ:
				System.out.printf("  Production: comp_expr2 -> leq addit_expr\n");
				lexAn.takeToken(); // consume operator
				parseAdditExpr();
				break;
				
			default:
					System.out.printf("  Production: addit_expr2 -> ε\n");
					// it's epsilon (empty) production
		}
	}
	// Addititve operators
	private void parseAdditExpr() {
		System.out.printf("Parsing: AdditiveExpression\n");
		// addit_expr -> multi_expr addit_expr2
		System.out.printf("  Production: addit_expr -> multi_expr addit_expr2\n");
		parseMultiExpr();
		parseAdditExpr2();
	}
	private void parseAdditExpr2() {
		System.out.printf("Parsing: AdditiveExpression2\n");
		// addit_expr2 -> additop multi_expr addit_expr2 | ε
		Token token = lexAn.peekToken();

		// additop -> add | sub
		switch (token.symbol())
		{
			case Token.Symbol.ADD:
				System.out.printf("  Production: addit_expr2 -> add multi_expr addit_expr2\n");
				lexAn.takeToken(); // consume operator
				parseMultiExpr();
				parseAdditExpr2();
				break;
			case Token.Symbol.SUB:
				System.out.printf("  Production: addit_expr2 -> sub multi_expr addit_expr2\n");
				lexAn.takeToken(); // consume operator
				parseMultiExpr();
				parseAdditExpr2();
				break;
				
			default:
				System.out.printf("  Production: addit_expr2 -> ε\n");
				// it's epsilon (empty) production
		}
	}
	// Multiplicative operators
	private void parseMultiExpr() {
		System.out.printf("Parsing: MultiplicativeExpression\n");
		// multi_expr -> pre_expr multi_expr2
		System.out.printf("  Production: multi_expr -> pre_expr multi_expr2\n");
		parsePreExpr();
		parseMultiExpr2();
	}
	private void parseMultiExpr2() {
		System.out.printf("Parsing: MultiplicativeExpression2\n");
		// multi_expr2 -> multiop pre_expr multi_expr2 | ε
		Token token = lexAn.peekToken();

		// multiop -> mul | div | mod
		switch (token.symbol())
		{
			case Token.Symbol.MUL:
				System.out.printf("  Production: multi_expr2 -> mul pre_expr multi_expr2\n");
				lexAn.takeToken(); // consume operator
				parsePreExpr();
				parseMultiExpr2();
				break;
			case Token.Symbol.DIV:
				System.out.printf("  Production: multi_expr2 -> div pre_expr multi_expr2\n");
				lexAn.takeToken(); // consume operator
				parsePreExpr();
				parseMultiExpr2();
				break;
			case Token.Symbol.MOD:
				System.out.printf("  Production: multi_expr2 -> mod pre_expr multi_expr2\n");
				lexAn.takeToken(); // consume operator
				parsePreExpr();
				parseMultiExpr2();
				break;
				
			default:
				System.out.printf("  Production: multi_expr2 -> ε\n");
				// it's epsilon (empty) production
		}
	}
	// Prefix operators
	private void parsePreExpr() {
		System.out.printf("Parsing: PrefixExpression\n");
		// pre_expr -> prefixop pre_expr | post_expr
		Token token = lexAn.peekToken();

		// prefixop -> not | add | sub | ptr
		switch (token.symbol())
		{
			case Token.Symbol.NOT:
				System.out.printf("  Production: pre_expr -> not pre_expr\n");
				lexAn.takeToken(); // consume operator
				parsePreExpr();
				break;
			case Token.Symbol.ADD:
				System.out.printf("  Production: pre_expr -> add pre_expr\n");
				lexAn.takeToken(); // consume operator
				parsePreExpr();
				break;
			case Token.Symbol.SUB:
				System.out.printf("  Production: pre_expr -> sub pre_expr\n");
				lexAn.takeToken(); // consume operator
				parsePreExpr();
				break;
			case Token.Symbol.PTR:
				System.out.printf("  Production: pre_expr -> ptr pre_expr\n");
				lexAn.takeToken(); // consume operator
				parsePreExpr();
				break;
				
			default:
				System.out.printf("  Production: pre_expr -> post_expr\n");
				parsePostExpr();
		}
	}
	// Postfix operators
	private void parsePostExpr() {
		System.out.printf("Parsing: PostfixExpression\n");
		// post_expr -> primary post_expr2
		System.out.printf("  Production: post_expr -> primary post_expr2\n");
		parsePrimary();
		parsePostExpr2();
	}
	private void parsePostExpr2() {
		System.out.printf("Parsing: PostfixExpression2\n");
		// post_expr2 -> postfixop post_expr2 | ε
		Token token = lexAn.peekToken();

		// postfixop -> ptr
		switch (token.symbol())
		{
			case Token.Symbol.PTR:
				System.out.printf("  Production: post_expr2 -> ptr post_expr2\n");
				lexAn.takeToken(); // consume operator
				parsePostExpr2();
				break;
				
			default:
				System.out.printf("  Production: post_expr2 -> ε\n");
				// it's epsilon (empty) production
		}
	}
	// Primary 
	private void parsePrimary() {
		System.out.printf("Parsing: PrimaryExpression\n");
		// primary -> int | char | string
		// primary -> lpar expr rpar
		// primary -> id opt_args
		Token token = lexAn.peekToken();
		switch (token.symbol())
		{
			case Token.Symbol.INTCONST:
				System.out.printf("  Production: primary -> int\n");
				lexAn.takeToken(); // consume const
				break;
			case Token.Symbol.CHARCONST:
				System.out.printf("  Production: primary -> char\n");
				lexAn.takeToken(); // consume const
				break;
			case Token.Symbol.STRINGCONST:
				System.out.printf("  Production: primary -> string\n");
				lexAn.takeToken(); // consume const
				break;
			case Token.Symbol.LPAREN:
				System.out.printf("  Production: primary -> lpar expr rpar\n");
				lexAn.takeToken(); // consume '('
				parseExpr(); // Parse Expression
				check(Token.Symbol.RPAREN, "Expected ')' after '('");
				break;

			case Token.Symbol.IDENTIFIER:
				System.out.printf("  Production: primary -> id opt_args\n");
				lexAn.takeToken(); // consume id
				parseOptArgs(); // Parse Optinal args
				break;
				
			default:
				raiseSyntaxError(token, "Expected const or identifier");
		}
	}
	// Optional args
	private void parseOptArgs() {
		System.out.printf("Parsing: OptionalArguments\n");
		// opt_args -> lpar args rpar | ε
		Token token = lexAn.peekToken();
		switch (token.symbol())
		{
			case Token.Symbol.LPAREN:
				System.out.printf("  Production: opt_args -> lpar args rpar\n");
				lexAn.takeToken(); // consume '('
				parseArgs(); // Parse Arguments
				check(Token.Symbol.RPAREN, "Expected ')' after '('");
				break;
				
			default:
				System.out.printf("  Production: opt_args -> ε\n");
				// it's epsilon (empty) production
		}
	}
	private boolean couldStartExpression(Token.Symbol symbol) {
		return symbol == Token.Symbol.INTCONST ||    // Integer constants
			   symbol == Token.Symbol.CHARCONST ||   // Character constants
			   symbol == Token.Symbol.STRINGCONST || // String constants
			   symbol == Token.Symbol.LPAREN ||      // Left parenthesis (for grouped expressions)
			   symbol == Token.Symbol.IDENTIFIER ||  // Identifiers
			   symbol == Token.Symbol.NOT ||         // Logical NOT (prefix operator)
			   symbol == Token.Symbol.ADD ||         // Unary plus (prefix operator)
			   symbol == Token.Symbol.SUB ||         // Unary minus (prefix operator)
			   symbol == Token.Symbol.PTR;           // Pointer operator (prefix)
	}
	// Arguments
	private void parseArgs()
	{
		System.out.printf("Parsing: Arguments\n");
		// args -> expr args2 | ε
		Token token = lexAn.peekToken();

		// Check if token could start an expression
		if (couldStartExpression(token.symbol())) {
			System.out.printf("  Production: args -> expr args2\n");
			parseExpr();
			parseArgs2();
		} else {
			System.out.printf("  Production: args -> ε\n");
			// it's epsilon (empty) production
		   
		}
	}
	private void parseArgs2() {
		System.out.printf("Parsing: Arguments2\n");
		// args2 -> comma expr args2 | ε
		Token token = lexAn.peekToken();

		switch (token.symbol())
		{
			case Token.Symbol.COMMA: // comma expr args2
				System.out.printf("  Production: args2 -> comma expr args2\n");
				lexAn.takeToken(); // consume ','
				parseExpr(); // Parse Expressiom
				parseArgs2(); // Parse Arguments2
				break;

			default:
				System.out.printf("  Production: args2 -> ε\n");
				// it's epsilon (empty) production
		}
	}
	// Initializers
	private void parseInits()
	{
		System.out.printf("Parsing: Initializers\n");
		// inits -> init inits2 | ε
		Token token = lexAn.peekToken();

		// Check if token could start initializer
		if (couldStartInitializer(token.symbol())) {
			System.out.printf("  Production: inits -> init inits2\n");
			parseInit();
			parseInits2();
		} else {
			System.out.printf("  Production: inits -> ε\n");
			// it's epsilon (empty) production
		}
	}
	private void parseInits2() {
		System.out.printf("Parsing: Initializers2\n");
		// inits2 -> comma init inits2 | ε
		Token token = lexAn.peekToken();

		switch (token.symbol())
		{
			case Token.Symbol.COMMA: // comma init 
				System.out.printf("  Production: inits2 -> comma init inits2\n");
				lexAn.takeToken(); // consume ','
				parseInit(); // Parse Initializer
				parseInits2(); // Parse Initializers2
				break;

			default:
				System.out.printf("  Production: inits2 -> ε\n");
				// it's epsilon (empty) production
		}
	}
	// Initializer
	private void parseInit()
	{
		System.out.printf("Parsing: Initializer\n");
		// init -> int mul_const_expr | string | char
		Token token = lexAn.peekToken();

		switch (token.symbol()) {
			case Token.Symbol.INTCONST:
				System.out.printf("  Production: init -> int mul_const_expr\n");
				lexAn.takeToken(); // consume INTCONST
				parseMulConstExpr();
				break;
			case Token.Symbol.CHARCONST:
				System.out.printf("  Production: init -> char\n");
				lexAn.takeToken(); // consume CHARCONST
				break;
			case Token.Symbol.STRINGCONST:
				System.out.printf("  Production: init -> string\n");
				lexAn.takeToken(); // consume STRINGCONST
				break;
			default:
				raiseSyntaxError(token, "Expected an initializer (int, char, or string)");
		}
	}
	private void parseMulConstExpr() {
		System.out.printf("Parsing: MultiplicationConstExpression\n");
		// mul_const_expr -> mul const | ε
		Token token = lexAn.peekToken();

		switch (token.symbol())
		{
			case Token.Symbol.MUL: //  mul const
				System.out.printf("  Production: mul_const_expr -> mul const\n");
				lexAn.takeToken(); // consume mul
				parseConst();
				break;

			default:
				System.out.printf("  Production: mul_const_expr -> ε\n");
				// it's epsilon (empty) production
		}
	}
	private boolean couldStartInitializer(Token.Symbol symbol) {
		return symbol == Token.Symbol.INTCONST ||    // Integer constants (for both int_mul and const)
			   symbol == Token.Symbol.CHARCONST ||   // Character constants (for const)
			   symbol == Token.Symbol.STRINGCONST;   // String constants (for const)
	}
	// Const 
	private void parseConst() {
		System.out.printf("Parsing: Constant\n");
		// cosnt -> int | char | string
		Token token = lexAn.peekToken();
		switch (token.symbol())
		{
			case Token.Symbol.INTCONST:
				System.out.printf("  Production: const -> int\n");
				lexAn.takeToken(); // consume const
				break;
			case Token.Symbol.CHARCONST:
				System.out.printf("  Production: const -> char\n");
				lexAn.takeToken(); // consume const
				break;
			case Token.Symbol.STRINGCONST:
				System.out.printf("  Production: const -> string\n");
				lexAn.takeToken(); // consume const
				break;

			default:
				raiseSyntaxError(token, "Expected constant (int, char, string)");
		}
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
				synAn.parse();
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
