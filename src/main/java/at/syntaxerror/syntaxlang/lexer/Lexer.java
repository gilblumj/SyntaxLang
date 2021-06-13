/*
 * SyntaxLang - A simple programming language written in Java
 * Copyright (C) 2021  SyntaxError
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package at.syntaxerror.syntaxlang.lexer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import at.syntaxerror.syntaxlang.input.InputEnvironment;
import at.syntaxerror.syntaxlang.string.WideCharacter;
import at.syntaxerror.syntaxlang.string.WideString;
import at.syntaxerror.syntaxlang.trace.ErrorOptional;
import at.syntaxerror.syntaxlang.trace.Position;
import ch.obermuhlner.math.big.BigComplex;
import ch.obermuhlner.math.big.BigDecimalMath;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author SyntaxError
 *
 */
@RequiredArgsConstructor
public class Lexer {
	
	private static final List<BiFunction<Lexer, Integer, ErrorOptional<Token>>> TOKEN_FACTORIES = new ArrayList<>();
	private static final Map<Character, TokenType> SINGLE_TOKENS = new HashMap<>();
	
	private static final BiFunction<Lexer, Integer, ErrorOptional<Token>> makeFactory(char c, Function<Lexer, TokenType> factory) {
		return (l, n) -> {
			if(n != c) return null;
			l.input.readChar();
			l.input.mark();
			
			TokenType tt = factory.apply(l);
			if(tt != null)
				return ErrorOptional.of(new Token(tt));
			
			return null;
		};
	}
	
	static {
		TOKEN_FACTORIES.add(Lexer::makeIdentifier);
		TOKEN_FACTORIES.add(Lexer::makeString);
		TOKEN_FACTORIES.add(Lexer::makeNumber);
		
		TOKEN_FACTORIES.add(makeFactory('!', Lexer::makeExclamation));
		TOKEN_FACTORIES.add(makeFactory('%', Lexer::makePercent));
		TOKEN_FACTORIES.add(makeFactory('&', Lexer::makeAmpersand));
		TOKEN_FACTORIES.add(makeFactory('*', Lexer::makeAsterisk));
		TOKEN_FACTORIES.add(makeFactory('+', Lexer::makePlus));
		TOKEN_FACTORIES.add(makeFactory('-', Lexer::makeMinus));
		TOKEN_FACTORIES.add(makeFactory('.', Lexer::makePeriod));
		TOKEN_FACTORIES.add(makeFactory('/', Lexer::makeSlash));
		TOKEN_FACTORIES.add(makeFactory('<', Lexer::makeLess));
		TOKEN_FACTORIES.add(makeFactory('=', Lexer::makeEquals));
		TOKEN_FACTORIES.add(makeFactory('>', Lexer::makeGreater));
		TOKEN_FACTORIES.add(makeFactory('^', Lexer::makeCaret));
		TOKEN_FACTORIES.add(makeFactory('|', Lexer::makeVerticalBar));
		
		SINGLE_TOKENS.put('(', TokenType.LPAREN);
		SINGLE_TOKENS.put(')', TokenType.RPAREN);
		SINGLE_TOKENS.put(',', TokenType.COMMA);
		SINGLE_TOKENS.put(':', TokenType.COLON);
		SINGLE_TOKENS.put(';', TokenType.SEMICOLON);
		SINGLE_TOKENS.put('?', TokenType.QUESTION);
		SINGLE_TOKENS.put('[', TokenType.LBRACKET);
		SINGLE_TOKENS.put('\\', TokenType.SCOPE);
		SINGLE_TOKENS.put(']', TokenType.RBRACKET);
		SINGLE_TOKENS.put('{', TokenType.LBRACE);
		SINGLE_TOKENS.put('}', TokenType.RBRACE);
		SINGLE_TOKENS.put('~', TokenType.COMPLEMENT);
	}

	@NonNull
	private final InputEnvironment input;
	
	private int line = 0;
	private int prevLineAbs = 0;
	private int lineAbs = 0;
	private int abs = -1;
	
	private void startPosition() {
		abs = input.getPosition();
	}
	private Position getPosition() {
		return new Position(abs, abs - lineAbs, line, input.getPosition() - abs, prevLineAbs);
	}
	
	private ErrorOptional<Token> getError(String message) {
		return new ErrorOptional<>(getPosition(), message);
	}
	private ErrorOptional<Token> getError(String message, int unread) {
		input.unread(unread);
		return new ErrorOptional<>(getPosition(), message);
	}

	private void newLine() {
		++line;
		prevLineAbs = lineAbs;
		lineAbs = input.getPosition();
	}
	
	public Tokens makeTokens() {
		List<Token> tokens = new ArrayList<>();
		
		while(true) {
			input.mark();
			WideCharacter wc = input.readChar();
			input.reset();
			
			if(wc == null)
				break;
			
			int c = wc.getCodepoint();
			
			if(c == '\n') {
				input.readChar();
				newLine();
				continue;
			}
			
			if(c == ' ' || c == '\t') {
				input.readChar();
				continue;
			}
			
			if(c == '#') {
				input.readWhile(ch -> ch != '\n');
				continue;
			}
			
			startPosition();
			
			ErrorOptional<Token> result = null;
			
			Character chr = (char) (c <= 0xFFFF ? c : 0);
			
			if(SINGLE_TOKENS.containsKey(chr)) {
				result = ErrorOptional.of(new Token(SINGLE_TOKENS.get(chr)));
				input.readChar();
			} else for(BiFunction<Lexer, Integer, ErrorOptional<Token>> tokenFactory : TOKEN_FACTORIES) {
				int pos = input.getPosition();
				
				if((result = tokenFactory.apply(this, c)) != null)
					break;
				
				input.unread(input.getPosition() - pos);
			}
			
			if(result == null || (result.isEmpty() && !result.isError()))
				result = getError("Unexpected character: " + wc.toVerboseString());
			
			if(result != null && result.isError()) {
				input.terminate(
					result.getMessage(),
					result.getPosition()
				);
				return null; // unreachable
			}
			
			Token tok = result.getValue();
			tok.setPosition(getPosition());
			tokens.add(tok);
		}
		
		startPosition();
		Token eof = new Token(TokenType.EOF);
		eof.setPosition(getPosition());
		
		tokens.add(eof);
		
		return new Tokens(tokens);
	}
	
	//
	
	private int readHex() {
		WideCharacter wc = input.readChar();
		
		if(wc == null)
			return -1;
		
		int cp = wc.getCodepoint();
		
		if(cp >= '0' && cp <= '9')
			return cp - '0';
		if(cp >= 'a' && cp <= 'f')
			return cp - 'a' + 0xA;
		if(cp >= 'A' && cp <= 'F')
			return cp - 'A' + 0xA;
		
		return -2;
	}
	private int readOct() {
		WideCharacter wc = input.readChar();
		
		if(wc == null)
			return -1;
		
		int cp = wc.getCodepoint();
		
		if(cp >= '0' && cp <= '7')
			return cp - '0';
		
		return -2;
	}
	
	private ErrorOptional<Token> makeIdentifier(int c) {
		if(!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_'))
			return null;
		
		WideString value = input.readWhile(wc -> 
			(wc >= 'a' && wc <= 'z') || (wc >= 'A' && wc <= 'Z') || 
			(wc >= '0' && wc <= '9') || wc == '_'
		);
		
		String strval = value.toString();
		
		Keyword keyword = Keyword.getKeyword(strval);
		
		if(keyword != null)
			return ErrorOptional.of(new Token(keyword));
		
		TokenType tt = switch(strval) {
		case "not" -> TokenType.NOT;
		case "and" -> TokenType.AND;
		case "is" -> TokenType.EQUAL;
		case "or" -> TokenType.OR;
		default -> null;
		};
		
		return ErrorOptional.of(tt == null ? new Token(TokenType.IDENTIFIER, value) : new Token(tt, true));
	}
	
	private ErrorOptional<Token> makeString(int c) {
		if(c != '"' && c != '\'')
			return null;
		input.readChar();
		
		boolean single = c == '\'';
		
		WideString value = new WideString();
		
		WideCharacter wc;
		while((wc = input.readChar()) != null) {
			int cp = wc.getCodepoint();
			
			if(cp == '\n') {
				input.mark();
				input.readChar();
				newLine();
				input.reset();
			}
			
			if((cp == '\'' && single) || cp == '"' && !single)
				break;
			
			if(cp == '\\') {
				wc = input.readChar();
				
				if(wc == null)
					return getError("Unterminated escape sequence in string");
				
				String error = null;
				
				String concat = switch(wc.getCodepoint()) {
				case 'b': yield "\b";
				case 'e': yield "\33";
				case 'f': yield "\f";
				case 'n': yield "\n";
				case 'r': yield "\r";
				case 't': yield "\t";
				case 'v': yield "\13";
				case '"': yield "\"";
				case '\'': yield "'";
				case '\\': yield "\\";
				case 'x': {
					int v = 0;
					
					for(int i = 1; i >= 0; --i) {
						int hex = readHex();
						
						if(hex == -1) {
							error = "Unfinished hexadecimal escape sequence in string";
							yield null;
						}
						if(hex == -2) {
							error = "Illegal hexadecimal character in escape sequence in string: "
									+ input.currentChar().toVerboseString();
							yield null;
						}
						
						v |= hex << (i << 2);
					}
					
					yield String.valueOf((char) v);
				}
				case 'u': {
					int v = 0;
					
					for(int i = 3; i >= 0; --i) {
						int hex = readHex();
						
						if(hex == -1) {
							error = "Unfinished unicode escape sequence in string";
							yield null;
						}
						if(hex == -2) {
							error = "Illegal unicode character in escape sequence in string: "
									+ input.currentChar().toVerboseString();
							yield null;
						}
						
						v |= hex << (i << 2);
					}
					
					yield String.valueOf((char) v);
				}
				case 'U': {
					int v = 0;
					int n = 0;
					
					WideCharacter extendedUnicode = input.readChar();
					
					if(extendedUnicode == null) {
						error = "Unfinished extended unicode escape sequence in string";
						yield null;
					}
					if(extendedUnicode.getCodepoint() != '{') {
						error = "Illegal extended unicode character in escape sequence in string: "
								+ extendedUnicode.toVerboseString();
						yield null;
					}
					
					for(int i = 5; i >= 0; --i) {
						input.mark();
						int hex = readHex();
						
						if(hex < 0) {
							input.reset();
							break;
						}
						
						v <<= 4;
						v |= hex;
						++n;
					}
					
					extendedUnicode = input.readChar();
					
					if(extendedUnicode == null) {
						error = "Unfinished extended unicode escape sequence in string";
						yield null;
					}
					if(n == 0 || extendedUnicode.getCodepoint() != '}') {
						error = "Illegal extended unicode character in escape sequence in string: "
								+ extendedUnicode.toVerboseString();
						yield null;
					}
					
					yield new WideCharacter(v).toString();
				}
				case '0': case '1': case '2': case '3':
				case '4': case '5': case '6': case '7': {
					int v = wc.getCodepoint() - '0';
					
					for(int i = 1; i >= 0; --i) {
						input.mark();
						int hex = readOct();
						
						if(hex < 0) {
							input.reset();
							break;
						}
						
						v <<= 3;
						v |= hex;
					}
					
					yield String.valueOf((char) v);
				}
				default:
					error = "Illegal escape sequence in string: " + wc.toVerboseString();
					yield null;
				};
				
				if(concat == null)
					return getError(error);

				value = value.concat(concat);
				continue;
			}
			
			value = value.concat(wc);
		}
		
		if(wc == null)
			return getError("Unterminated string: expected " + (single ? "'" : "\""));
		
		return ErrorOptional.of(new Token(TokenType.STRING, value));
	}

	private ErrorOptional<Token> makeNumber(int c) {
		if(!((c >= '0' && c <= '9') || c == '.'))
			return null;
		
		BigDecimal value = null;
		boolean hasValue = false;
		
		String name = "number";
		
		if(c == '0') {
			input.mark();
			input.readChar();
			int n = input.readCharAsInt();
			
			if(n == 'x' || n == 'o' || n == 'b') {
				String charset;
				int radix;
				
				switch(n) {
				case 'x':
					charset = "0123456789abcdefABCDEF";
					name = "hexadecimal";
					radix = 16;
					break;
				case 'o':
					charset = "01234567";
					name = "octal";
					radix = 8;
					break;
				case 'b':
					charset = "01";
					name = "binary";
					radix = 2;
					break;
				default:
					throw new RuntimeException();
				}
				
				WideString wliteral = input.readWhile(p -> charset.indexOf(p) > -1);
				
				if(wliteral.length() == 0)
					return getError("Invalid " + name + " literal", 1);
				
				value = new BigDecimal(new BigInteger(wliteral.toString(), radix));
				hasValue = true;
			} else input.reset();
		}
		
		if(!hasValue) {
			String integer = input.readWhile(p -> p >= '0' && p <= '9').toString();
			String decimal;
			
			input.mark();
			int n = input.readCharAsInt();
			
			if(n == '.')
				decimal = input.readWhile(p -> p >= '0' && p <= '9').toString();
			else {
				decimal = "";
				input.reset();
			}
			
			if(integer.isEmpty() && decimal.isEmpty())
				return null; // don't throw an error, because '.' could still be another token
			
			String exponent;
			boolean sign = true;
			
			input.mark();
			n = input.readCharAsInt();
			
			if(n == 'e' || n == 'E') {
				input.mark();
				n = input.readCharAsInt();
				
				if(n != -1) {
					if(n == '-')
						sign = false;
					else if(n != '+')
						input.reset();
					
					exponent = input.readWhile(p -> p >= '0' && p <= '9').toString();
				} else exponent = "";
				
				if(exponent.isEmpty())
					return getError("Unfinished exponent in number literal", 1);
			} else {
				input.reset();
				exponent = "0";
			}
			
			if(integer.isEmpty())
				integer = "0";
			if(decimal.isEmpty())
				decimal = "0";
			
			value = BigDecimalMath.toBigDecimal("%s.%se%c%s".formatted(integer, decimal, sign ? '+' : '-', exponent));
		}
		
		input.mark();
		int n = input.readCharAsInt();
		
		if(n != 'i' && n != 'j' && ((n >= 'a' && n <= 'z') ||
			(n >= 'A' && n <= 'Z') || (n >= '0' && n <= '9') || n == '_' || n == '.'))
			return getError("Invalid character in " + name + " literal: " + input.currentChar().toVerboseString(), 1);
		
		if(n == 'i' || n == 'j')
			return ErrorOptional.of(new Token(TokenType.COMPLEX, BigComplex.valueOf(BigDecimal.ZERO, value)));
		else input.reset();
		
		return ErrorOptional.of(new Token(TokenType.NUMBER, value));
	}
	
	// 
	
	private TokenType makeExclamation() {
		if(input.readCharAsInt() == '=') {
			if(input.readCharAsInt() == '=')
				return TokenType.NOT_IDENTICAL;
			
			return TokenType.NOT_EQUAL;
		}
		
		input.reset();
		return TokenType.NOT;
	}
	private TokenType makePercent() {
		if(input.readCharAsInt() == '=')
			return TokenType.ASSIGN_MOD;
		
		input.reset();
		return TokenType.MODULO;
	}
	private TokenType makeAmpersand() {
		int n = input.readCharAsInt();
		
		if(n == '=')
			return TokenType.ASSIGN_AND;
		if(n == '&')
			return TokenType.AND;
		
		input.reset();
		return TokenType.BITAND;
	}
	private TokenType makeAsterisk() {
		int n = input.readCharAsInt();
		
		if(n == '=')
			return TokenType.ASSIGN_MUL;
		if(n == '*') {
			input.mark();
			n = input.readCharAsInt();
			
			if(n == '=')
				return TokenType.ASSIGN_POW;

			input.reset();
			return TokenType.POWER;
		}
		
		input.reset();
		return TokenType.MULTIPLY;
	}
	private TokenType makePlus() {
		int n = input.readCharAsInt();
		
		if(n == '=')
			return TokenType.ASSIGN_ADD;
		if(n == '+')
			return TokenType.INCREMENT;
		
		input.reset();
		return TokenType.PLUS;
	}
	private TokenType makeMinus() {
		int n = input.readCharAsInt();
		
		if(n == '=')
			return TokenType.ASSIGN_SUB;
		if(n == '-')
			return TokenType.DECREMENT;
		if(n == '>')
			return TokenType.SINGLE_ARROW;
		
		input.reset();
		return TokenType.MINUS;
	}
	private TokenType makePeriod() {
		if(input.readCharAsInt() == '.') {
			if(input.readCharAsInt() == '.')
				return TokenType.VARARGS;
		}
		
		input.reset();
		return TokenType.PERIOD;
	}
	private TokenType makeSlash() {
		if(input.readCharAsInt() == '=')
			return TokenType.ASSIGN_DIV;
		
		input.reset();
		return TokenType.DIVIDE;
	}
	private TokenType makeLess() {
		int n = input.readCharAsInt();
		
		if(n == '=')
			return TokenType.LESS_EQUAL;
		if(n == '<') {
			input.mark();
			n = input.readCharAsInt();
			
			if(n == '=')
				return TokenType.ASSIGN_LSH;

			input.reset();
			return TokenType.LSHIFT;
		}
		
		input.reset();
		return TokenType.LESS;
	}
	private TokenType makeEquals() {
		int n = input.readCharAsInt();
		
		if(n == '=') {
			if(input.readCharAsInt() == '=')
				return TokenType.IDENTICAL;
			
			return TokenType.EQUAL;
		}
		if(n == '>')
			return TokenType.DOUBLE_ARROW;
		
		input.reset();
		return TokenType.ASSIGN;
	}
	private TokenType makeGreater() {
		int n = input.readCharAsInt();
		
		if(n == '=')
			return TokenType.GREATER_EQUAL;
		if(n == '>') {
			input.mark();
			n = input.readCharAsInt();
			
			if(n == '=')
				return TokenType.ASSIGN_RSH;

			input.reset();
			return TokenType.RSHIFT;
		}
		
		input.reset();
		return TokenType.GREATER;
	}
	private TokenType makeCaret() {
		if(input.readCharAsInt() == '=')
			return TokenType.ASSIGN_XOR;
		
		input.reset();
		return TokenType.XOR;
	}
	private TokenType makeVerticalBar() {
		int n = input.readCharAsInt();
		
		if(n == '=')
			return TokenType.ASSIGN_OR;
		if(n == '|')
			return TokenType.OR;
		
		input.reset();
		return TokenType.BITOR;
	}
	
}