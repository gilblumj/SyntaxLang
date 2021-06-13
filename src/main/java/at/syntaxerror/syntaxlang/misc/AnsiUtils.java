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
package at.syntaxerror.syntaxlang.misc;

import lombok.experimental.UtilityClass;

/**
 * @author SyntaxError
 * 
 */
@UtilityClass
public class AnsiUtils {

	public static boolean COLORIZED = true;

	public static enum Special {
		RESET		(0),
		BOLD		(1),
		ITALIC		(3),
		UNDERLINE	(4),
		INVERT		(7);
		
		private final String ansi;
		
		private Special(int code) {
			ansi = ansi(code);
		}
		
		@Override
		public String toString() {
			return ansi;
		}
	}
	
	public static enum Foreground {
		BLACK,
		RED,
		GREEN,
		YELLOW,
		BLUE,
		MAGENTA,
		CYAN,
		WHITE,
		BRIGHT_BLACK,
		BRIGHT_RED,
		BRIGHT_GREEN,
		BRIGHT_YELLOW,
		BRIGHT_BLUE,
		BRIGHT_MAGENTA,
		BRIGHT_CYAN,
		BRIGHT_WHITE;
		
		private final String ansi;
		
		private Foreground() {
			int code = ordinal();
			
			if(code > 7)
				code += 82;
			else code += 30;
			
			ansi = ansi(code);
		}
		
		@Override
		public String toString() {
			return ansi;
		}
		
	}
	
	public static enum Background {
		BLACK,
		RED,
		GREEN,
		YELLOW,
		BLUE,
		MAGENTA,
		CYAN,
		WHITE,
		BRIGHT_BLACK,
		BRIGHT_RED,
		BRIGHT_GREEN,
		BRIGHT_YELLOW,
		BRIGHT_BLUE,
		BRIGHT_MAGENTA,
		BRIGHT_CYAN,
		BRIGHT_WHITE;
		
		private final String ansi;
		
		private Background() {
			int code = ordinal();
			
			if(code > 7)
				code += 92;
			else code += 40;
			
			ansi = ansi(code);
		}
		
		@Override
		public String toString() {
			return ansi;
		}
		
	}
	
	public static String ansi(int code, int... more) {
		String ansi = "\033[" + code;
		
		for(int i : more)
			ansi += ";" + i;
		
		return ansi + "m";
	}
	
	public static String stripAnsi(String input) {
		return COLORIZED ? input : input.replaceAll("\\x1B\\[\\d{1,3}(;\\d{1,3})*m", "");
	}

}
