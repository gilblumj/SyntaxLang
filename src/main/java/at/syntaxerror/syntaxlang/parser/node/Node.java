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
package at.syntaxerror.syntaxlang.parser.node;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Objects;

import org.json.JSONArray;
import org.json.JSONObject;

import at.syntaxerror.syntaxlang.SyntaxLangException;
import at.syntaxerror.syntaxlang.lexer.Token;
import at.syntaxerror.syntaxlang.trace.Position;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * @author SyntaxError
 * 
 */
public abstract class Node {

	@NonNull
	@Setter
	@Getter
	private Position position = null;

	public void errorCheck() {
		errorCheck("[" + getClass().getSimpleName() + "] ");
	}
	
	private void errorCheck(String prefix) {
		try {
			Objects.requireNonNull(getPosition(), "Node position is null: " + prefix);
			
			String cname = getClass().getSimpleName();
			
			for(Field f : getClass().getDeclaredFields()) {
				if(Modifier.isStatic(f.getModifiers()))
					continue;
				
				f.setAccessible(true);
				
				Object value = f.get(this);
				String name = f.getName();
				
				if(value instanceof Node n) {
					n.errorCheck(prefix + name + "[" + cname + "] ");
				} else if(value instanceof Token t)
					Objects.requireNonNull(t.getPosition(), "Token position is null: " + prefix + name + "[" + cname + "]");
				else if(value instanceof List<?> l) {
					int i = 0;
					
					for(Object lval : l) {
						if(lval instanceof Node n)
							n.errorCheck(prefix + name + "#" + i + "[" + cname + "] ");
						else if(lval instanceof Token t)
							Objects.requireNonNull(t.getPosition(), "Token position is null: " + prefix + name + "#" + i + "[" + cname + "]");

						++i;
					}
				}
			}
		} catch (SyntaxLangException e) {
			throw e;
		} catch (Exception e) {
			throw new SyntaxLangException(e);
		}
	}
	
	public JSONObject toJSON() {
		try {
			JSONObject json = new JSONObject();
			json.put("nodename", getClass().getSimpleName());
			
			for(Field f : getClass().getDeclaredFields()) {
				if(Modifier.isStatic(f.getModifiers()))
					continue;
				
				f.setAccessible(true);
				
				Object value = f.get(this);
				String name = f.getName();
				
				if(value instanceof Node n)
					json.put(name, n.toJSON());
				else if(value instanceof Token t)
					json.put(name, t.toSimpleString());
				else if(value instanceof Boolean b)
					json.put(name, b);
				else if(value instanceof List<?> l) {
					JSONArray list = new JSONArray();
					
					for(Object lval : l) {
						if(lval instanceof Node n)
							list.put(n.toJSON());
						else if(lval instanceof Token t)
							list.put(t.toSimpleString());
					}
					
					json.put(name, list);
				}
			}
			
			return json;
		} catch (Exception e) {
			throw new SyntaxLangException("Could not serialize Node", e);
		}
	}
	
}
