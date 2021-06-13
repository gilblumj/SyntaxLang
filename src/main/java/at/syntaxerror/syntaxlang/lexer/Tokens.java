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

import java.util.EmptyStackException;
import java.util.List;
import java.util.Stack;

import lombok.Getter;
import lombok.NonNull;

/**
 * @author SyntaxError
 * 
 */
public class Tokens {

	private List<Token> tokens;
	private Stack<Integer> marked;
	private Stack<Throwable> markedTrace;
	
	@Getter
	private int position;
	
	public Tokens(@NonNull List<Token> tokens) {
		this.tokens = tokens;
		marked = new Stack<>();
		markedTrace = new Stack<>();
		position = 0;
	}
	
	public int size() {
		return tokens.size();
	}
	
	public Token next() {
		Token tok = current();
		++position;
		return tok;
	}
	
	public Token current() {
		if(tokens.size() == 0)
			return null;
		
		int pos = position;
		
		if(pos >= tokens.size())
			pos = tokens.size() - 1;
		
		return tokens.get(pos);
	}
	
	public Token previous() {
		if(tokens.size() == 0)
			return null;
		
		int pos = position - 1;
		
		if(pos < 0)
			pos = 0;
		
		return tokens.get(pos);
	}
	
	public void mark() {
		marked.add(position);
		markedTrace.add(new Throwable());
	}
	public void unmark() {
		try {
			marked.pop();
			markedTrace.pop();
		} catch (EmptyStackException e) {}
	}
	
	public void reset() {
		try {
			Integer mark = marked.pop();
			markedTrace.pop();
			
			if(mark != null)
				position = mark;
		} catch (EmptyStackException e) {
			
		}
	}

	public boolean isMarked() {
		return !marked.isEmpty();
	}
	public void purgeMarks() {
		System.err.printf("purging %d marks:\n", marked.size());
		
		for(int i = 0; i < marked.size(); ++i) {
			System.err.printf("- #%d:\n", marked.pop().intValue());
			markedTrace.pop().printStackTrace();
		}
	}
	
	@Override
	public String toString() {
		return "Tokens" + tokens;
	}

}
