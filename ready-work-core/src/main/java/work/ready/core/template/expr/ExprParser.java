/**
 * Copyright (c) 2011-2019, James Zhan 詹波 (jfinal@126.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package work.ready.core.template.expr;

import work.ready.core.template.EngineConfig;
import work.ready.core.template.expr.ast.*;
import work.ready.core.template.stat.Location;
import work.ready.core.template.stat.ParaToken;
import work.ready.core.template.stat.ParseException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class ExprParser {

	static final Tok EOF = new Tok(Sym.EOF, -1);

	Tok peek = null;
	int forward = 0;
	List<Tok> tokenList;
	Location location;

	ParaToken paraToken;
	EngineConfig engineConfig;

	public ExprParser(ParaToken paraToken, EngineConfig engineConfig, String fileName) {
		this.paraToken = paraToken;
		this.engineConfig = engineConfig;
		this.location = new Location(fileName, paraToken.getRow());
	}

	void initPeek() {
		peek = tokenList.get(forward);
	}

	Tok peek() {
		return peek;
	}

	Tok move() {
		peek = tokenList.get(++forward);
		return peek;
	}
	void resetForward(int position) {
		forward = position;
		peek = tokenList.get(forward);
	}

	Tok match(Sym sym) {
		Tok current = peek();
		if (current.sym == sym) {
			move();
			return current;
		}
		throw new ParseException("Expression error: can not match the symbol \"" + sym.value() + "\"", location);
	}

	public ExprList parseExprList() {
		return (ExprList)parse(true);
	}

	public ForCtrl parseForCtrl() {
		Expr forCtrl = parse(false);

		if (forCtrl instanceof ForCtrl) {
			return (ForCtrl)forCtrl;
		} else {
			throw new ParseException("The expression of #for directive is error", location);
		}
	}

	Expr parse(boolean isExprList) {
		tokenList = new ExprLexer(paraToken, location).scan();
		if (tokenList.size() == 0) {
			return ExprList.NULL_EXPR_LIST;
		}
		tokenList.add(EOF);
		initPeek();
		Expr expr = isExprList ? exprList() : forCtrl();
		if (peek() != EOF) {
			throw new ParseException("Expression error: can not match \"" + peek().value() + "\"", location);
		}
		return expr;
	}

	ExprList exprList() {
		List<Expr> exprList = new ArrayList<Expr>();
		while (true) {
			Expr stat = expr();
			if (stat != null) {
				exprList.add(stat);
				if (peek().sym == Sym.COMMA) {
					move();
					if (peek() == EOF) {
						throw new ParseException("Expression error: can not match the char of comma ','", location);
					}
					continue ;
				}
			}
			break ;
		}
		return new ExprList(exprList);
	}

	Expr expr() {
		return assign();
	}

	Expr assign() {
		Tok idTok = peek();
		if (idTok.sym != Sym.ID) {
			return ternary();
		}

		int begin = forward;

		if (move().sym == Sym.ASSIGN) {
			move();
			return new Assign(idTok.value(), expr(), location);
		}

		if (peek().sym == Sym.LBRACK) {
			move();
			Expr index = expr();
			match(Sym.RBRACK);
			if (peek().sym == Sym.ASSIGN) {
				move();
				return new Assign(idTok.value(), index, expr(), location);	
			}
		}

		resetForward(begin);
		return ternary();
	}

	Expr ternary() {
		Expr cond = or();
		if (peek().sym == Sym.QUESTION) {
			move();
			Expr exprOne = expr();
			match(Sym.COLON);
			return new Ternary(cond, exprOne, expr(), location);
		}
		return cond;
	}

	Expr or() {
		Expr expr = and();
		for (Tok tok=peek(); tok.sym==Sym.OR; tok=peek()) {
			move();
			expr = new Logic(Sym.OR, expr, and(), location);
		}
		return expr;
	}

	Expr and() {
		Expr expr = equalNotEqual();
		for (Tok tok=peek(); tok.sym==Sym.AND; tok=peek()) {
			move();
			expr = new Logic(Sym.AND, expr, equalNotEqual(), location);
		}
		return expr;
	}

	Expr equalNotEqual() {
		Expr expr = greaterLess();
		for (Tok tok=peek(); tok.sym==Sym.EQUAL || tok.sym==Sym.NOTEQUAL; tok=peek()) {
			move();
			expr = new Compare(tok.sym, expr, greaterLess(), location);
		}
		return expr;
	}

	Expr greaterLess() {
		Expr expr = addSub();
		Tok tok = peek();
		if (tok.sym == Sym.LT || tok.sym == Sym.LE || tok.sym == Sym.GT || tok.sym == Sym.GE) {
			move();
			return new Compare(tok.sym, expr, addSub(), location);
		}
		return expr;
	}

	Expr addSub() {
		Expr expr = mulDivMod();
		for (Tok tok=peek(); tok.sym==Sym.ADD || tok.sym==Sym.SUB; tok=peek()) {
			move();
			expr = new Arith(tok.sym, expr, mulDivMod(), location);
		}
		return expr;
	}

	Expr mulDivMod() {
		Expr expr = nullSafe();
		for (Tok tok=peek(); tok.sym==Sym.MUL || tok.sym==Sym.DIV || tok.sym==Sym.MOD; tok=peek()) {
			move();
			expr = new Arith(tok.sym, expr, nullSafe(), location);
		}
		return expr;
	}

	Expr nullSafe() {
		Expr expr = unary();
		for (Tok tok=peek(); tok.sym==Sym.NULL_SAFE; tok=peek()) {
			move();
			expr = new NullSafe(expr, unary(), location);
		}
		return expr;
	}

	Expr unary() {
		Tok tok = peek();
		switch (tok.sym) {
		case NOT:
			move();
			return new Logic(tok.sym, unary(), location);
		case ADD:
		case SUB:
			move();
			return new Unary(tok.sym, unary(), location).toConstIfPossible();
		case INC:
		case DEC:
			move();
			return new IncDec(tok.sym, false, incDec(), location);
		default:
			return incDec();
		}
	}

	Expr incDec() {
		Expr expr = staticMember();
		Tok tok = peek();
		if (tok.sym == Sym.INC || tok.sym == Sym.DEC) {
			move();
			return new IncDec(tok.sym, true, expr, location);
		}

		return expr;
	}

	Expr staticMember() {
		if (peek().sym != Sym.ID) {
			return sharedMethod();
		}

		int begin = forward;
		while (move().sym == Sym.DOT && move().sym == Sym.ID) {
			;
		}

		if (peek().sym != Sym.STATIC || tokenList.get(forward - 1).sym != Sym.ID) {
			resetForward(begin);
			return sharedMethod();
		}

		String clazz = getClazz(begin);
		match(Sym.STATIC);
		String memberName = match(Sym.ID).value();

		if (peek().sym == Sym.LPAREN) {
			move();
			if (peek().sym == Sym.RPAREN) {
				move();
				return new StaticMethod(clazz, memberName, location);
			}

			ExprList exprList = exprList();
			match(Sym.RPAREN);
			return new StaticMethod(clazz, memberName, exprList, location);
		}

		return new StaticField(clazz, memberName, location);
	}

	String getClazz(int begin) {
		StringBuilder clazz = new StringBuilder();
		for (int i=begin; i<forward; i++) {
			clazz.append(tokenList.get(i).value());
		}
		return clazz.toString();
	}

	Expr sharedMethod() {
		Tok tok = peek();
		if (tok.sym != Sym.ID) {
			return indexMethodField(null);
		}
		if (move().sym != Sym.LPAREN) {
			resetForward(forward - 1);
			return indexMethodField(null);
		}

		move();
		if (peek().sym == Sym.RPAREN) {
			SharedMethod sharedMethod = new SharedMethod(engineConfig.getSharedMethodKit(), tok.value(), ExprList.NULL_EXPR_LIST, location);
			move();
			return indexMethodField(sharedMethod);
		}

		ExprList exprList = exprList();
		SharedMethod sharedMethod = new SharedMethod(engineConfig.getSharedMethodKit(), tok.value(), exprList, location);
		match(Sym.RPAREN);
		return indexMethodField(sharedMethod);
	}

	Expr indexMethodField(Expr expr) {
		if (expr == null) {
			expr = map();
		}

		while (true) {
			Tok tok = peek();

			if (tok.sym == Sym.LBRACK) {
				move();
				Expr index = expr();
				match(Sym.RBRACK);
				expr = new Index(expr, index, location);
				continue;
			}
			if (tok.sym != Sym.DOT) {
				return expr;
			}
			if ((tok = move()).sym != Sym.ID) {
				resetForward(forward - 1);
				return expr;
			}

			move();
			if (peek().sym != Sym.LPAREN) {
				expr = new Field(expr, tok.value(), location);
				continue;
			}

			move();

			if (peek().sym == Sym.RPAREN) {
				move();
				expr = new Method(expr, tok.value(), location);
				continue;
			}

			ExprList exprList = exprList();
			match(Sym.RPAREN);
			expr = new Method(expr, tok.value(), exprList, location);
		}
	}

	Expr map() {
		if (peek().sym != Sym.LBRACE) {
			return array();
		}

		LinkedHashMap<Object, Expr> mapEntry = new LinkedHashMap<Object, Expr>();
		Map map = new Map(mapEntry);
		move();
		if (peek().sym == Sym.RBRACE) {
			move();
			return map;
		}

		buildMapEntry(mapEntry);
		while (peek().sym == Sym.COMMA) {
			move();
			buildMapEntry(mapEntry);
		}
		match(Sym.RBRACE);
		return map;
	}

	void buildMapEntry(LinkedHashMap<Object, Expr> map) {
		Expr keyExpr = expr();
		Object key;
		if (keyExpr instanceof Id) {
			key = ((Id)keyExpr).getId();
		} else if (keyExpr instanceof Const) {
			key = ((Const)keyExpr).getValue();
		} else {
			throw new ParseException("Expression error: the value of map key must be identifier, String, Boolean, null or Number", location);
		}

		match(Sym.COLON);
		Expr value = expr();
		if (value == null) {
			throw new ParseException("Expression error: the value on the right side of map entry can not be blank", location);
		}
		map.put(key, value);
	}

	Expr array() {
		if (peek().sym != Sym.LBRACK) {
			return atom();
		}

		move();
		if (peek().sym == Sym.RBRACK) {
			move();
			return new Array(ExprList.NULL_EXPR_ARRAY, location);
		}
		ExprList exprList = exprList();
		if (exprList.length() == 1 && peek().sym == Sym.RANGE) {
			move();
			Expr end = expr();
			match(Sym.RBRACK);
			return new RangeArray(exprList.getExprArray()[0], end, location);
		}

		match(Sym.RBRACK);
		return new Array(exprList.getExprArray(), location);
	}

	Expr atom() {
		Tok tok = peek();
		switch (tok.sym) {
		case LPAREN:
			move();
			Expr expr = expr();
			match(Sym.RPAREN);
			return expr;
		case ID:
			move();
			return new Id(tok.value());
		case STR:
			move();
			return new Const(tok.sym, tok.value());
		case INT:
		case LONG:
		case FLOAT:
		case DOUBLE:
			move();
			return new Const(tok.sym, ((NumTok)tok).getNumberValue());
		case TRUE:
			move();
			return Const.TRUE;
		case FALSE:
			move();
			return Const.FALSE;
		case NULL:
			move();
			return Const.NULL;
		case COMMA:
		case SEMICOLON:
		case QUESTION:	
		case AND: case OR: case EQUAL: case NOTEQUAL:	
		case RPAREN:	
		case RBRACK:	
		case RBRACE:	
		case RANGE:		
		case COLON:		
		case EOF:
			return null;
		default :
			throw new ParseException("Expression error: can not match the symbol \"" + tok.value() + "\"", location);
		}
	}

	ForCtrl forCtrl() {
		ExprList exprList = exprList();
		if (peek().sym == Sym.SEMICOLON) {
			move();
			Expr cond = expr();
			match(Sym.SEMICOLON);
			ExprList update = exprList();
			return new ForCtrl(exprList, cond, update, location);
		}

		if (exprList.length() == 1) {
			Expr expr = exprList.getExprArray()[0];
			if (expr instanceof Id) {
				match(Sym.COLON);
				return new ForCtrl(((Id)expr), expr(), location);
			}
		}
		throw new ParseException("The expression of #for directive is error", location);
	}
}

