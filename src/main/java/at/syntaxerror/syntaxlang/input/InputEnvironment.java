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
package at.syntaxerror.syntaxlang.input;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.function.Predicate;

import at.syntaxerror.syntaxlang.misc.AnsiUtils;
import at.syntaxerror.syntaxlang.string.WideCharacter;
import at.syntaxerror.syntaxlang.string.WideString;
import at.syntaxerror.syntaxlang.trace.Position;
import lombok.Getter;
import lombok.NonNull;

/**
 * 
 * @author SyntaxError
 *
 */
public final class InputEnvironment {
	
	@NonNull
	public static InputEnvironment fromString(String source, String string) {
		return new InputEnvironment(source, new WideString(string.replaceAll("\\r\\n|\\r", "\n")));
	}
	@NonNull
	public static InputEnvironment fromBytes(String source, byte[] bytes) {
		return fromString(source, new String(bytes));
	}
	@NonNull
	public static InputEnvironment fromStream(String source, InputStream stream) {
		try(ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			
			byte[] buf = new byte[8192];
			int len;
			
			while((len = stream.read(buf)) > -1)
				out.write(buf, 0, len);
			
			return fromBytes(source, out.toByteArray());
		} catch (Exception e) {
			throw new InputException("Could not read from stream", e);
		} finally {
			try {
				stream.close();
			} catch (Exception e2) {}
		}
	}
	@NonNull
	public static InputEnvironment fromFile(File file) {
		try {
			return fromStream(file.getAbsolutePath(), new FileInputStream(file));
		} catch (Exception e) {
			throw new InputException("Could not read from file", e);
		}
	}
	
	@Getter
	private final String source;
	private final WideString data;
	private int length;
	@Getter
	private int position;
	private int marked;
	
	@NonNull
	private InputEnvironment(String source, WideString data) {
		this.source = source;
		this.data = data;
		length = data.length();
		position = 0;
		marked = -1;
	}
	
	public void mark() {
		marked = position;
	}
	public void reset() {
		if(marked != -1)
			position = marked;
	}
	
	public void unread(int len) {
		this.position -= len;
	}
	
	// relative

	public WideCharacter currentChar() {
		return readCharAt(position);
	}
	public int currentCharAsInt() {
		WideCharacter c = currentChar();
		return c == null ? -1 : c.getCodepoint();
	}
	
	public WideCharacter readChar() {
		WideCharacter c = readCharAt(position);
		
		if(c != null)
			++position;
		
		return c;
	}
	public int readCharAsInt() {
		WideCharacter c = readChar();
		return c == null ? -1 : c.getCodepoint();
	}

	public WideString readWhile(@NonNull Predicate<Integer> predicate, boolean reset) {
		WideString str = new WideString();
		
		WideCharacter c;
		while((c = readChar()) != null && predicate.test(c.getCodepoint()))
			str = str.concat(c);
		
		if(reset && c != null)
			unread(1);
		
		return str;
	}
	public WideString readWhile(@NonNull Predicate<Integer> predicate) {
		return readWhile(predicate, true);
	}
	
	// absolute
	
	public WideCharacter readCharAt(final int pos) {
		if(pos >= length || pos < 0)
			return null;
		
		return data.charAt(pos);
	}
	
	public WideString readAt(int from, int to) {
		return data.substring(from, Math.min(to, length));
	}
	
	//
	
	public void terminate(String message, Position position) {
		System.out.println(highlight(HighlightLevel.ERROR, message, position));
		System.exit(1);
	}
	
	public String highlight(HighlightLevel level, String message, Position position) {
		mark();
		readWhile(i -> i != '\n', false);
		readWhile(i -> i != '\n');
		reset();
		
		int pos = getPosition();
		WideCharacter lf = currentChar();
		
		if(lf != null && lf.getCodepoint() != '\n')
			++pos;
		
		int inner = position.abs() - position.prevLine();
		
		return highlight(
			HighlightLevel.ERROR,
			message,
			position.prevLine(),
			pos,
			inner,
			inner + Math.max(position.len() - 1, 0),
			Math.max(position.line() - 1, 0)
		);
	}
	
	public String highlight(HighlightLevel level, String message, int outerFrom, int outerTo, 
			int innerFrom, int innerTo, int lineOffset) {
		StringBuilder sb = new StringBuilder();
		
		int line = 0;
		int lineOff = 0;
		int off = -1;
		int len = 0;
		
		int fromLine = -1;
		int toLine = -1;
		int fromOffset = -1;
		int fullLength = 0;
		
		WideString part = readAt(outerFrom, outerTo);
		WideCharacter[] chars = part.concat('\n').toCharArray();
		
		int newlines = part.count('\n');
		
		int lineIndexLength = String.valueOf(lineOffset + newlines + 1).length();
		
		for(int i = 0; i < chars.length; ++i) {
			WideCharacter c = chars[i];
			int cp = c.getCodepoint();
			int cw = c.width();
			int tab = 4 - lineOff % 4;
			boolean isLast = i + 1 >= chars.length;
			
			if(cw == -1 && cp != '\n' && cp != '\t')
				continue;
			
			if(i >= innerFrom && i <= innerTo) {
				if(off == -1)
					off = lineOff;
				
				if(fromLine == -1) {
					fromLine = line;
					fromOffset = lineOff;
				}
				
				toLine = line;
				
				if(cw > 0)
					len += cw;
				else if(cp == '\t')
					len += tab;
				else if(cp == '\n')
					++len;
				
				++fullLength;
			}
			
			if(lineOff == 0) {
				String lineIndex = String.valueOf(lineOffset + line + 1);
				
				sb.append("%s%s%s%s ".formatted(
					AnsiUtils.Special.INVERT,
					" ".repeat(lineIndexLength - lineIndex.length()),
					lineIndex,
					AnsiUtils.Special.RESET
				));
			}
			
			if(cp == '\n') {
				if(!isLast)
					sb.append('↵');
				
				sb.append('\n');
				
				if(off > -1) {
					sb.append("%1$s%4$s%2$s %5$s%3$s%6$s%2$s\n".formatted(
						AnsiUtils.Special.INVERT,
						AnsiUtils.Special.RESET,
						AnsiUtils.Foreground.BRIGHT_RED,
						" ".repeat(lineIndexLength),
						" ".repeat(off),
						"~".repeat(len)
					));
					
					off = -1;
					len = 0;
				}
				
				lineOff = 0;
				++line;
			} else if(cp == '\t') {
				lineOff += tab;
				sb.append(" ".repeat(tab));
			} else {
				lineOff += cw;
				sb.append(c.toString());
			}
		}
		
		StringBuilder pre = new StringBuilder();

		pre.append("%s%s%s:".formatted(
			AnsiUtils.Foreground.BRIGHT_CYAN,
			source,
			AnsiUtils.Special.RESET
		));
		
		if(toLine == fromLine || toLine < 0 || fromLine < 0)
			pre.append("%s%d".formatted(
				AnsiUtils.Foreground.BRIGHT_YELLOW,
				fromLine < 0 ? -1 : fromLine + lineOffset + 1
			));
		else pre.append("%1$s[%2$s%4$d%3$s:%2$s%5$d%1$s]".formatted(
				AnsiUtils.Foreground.WHITE,
				AnsiUtils.Foreground.BRIGHT_YELLOW,
				AnsiUtils.Special.RESET,
				fromLine + lineOffset + 1,
				toLine + lineOffset + 1
			));
		
		pre.append("%s:%s%d".formatted(
			AnsiUtils.Special.RESET,
			AnsiUtils.Foreground.BRIGHT_YELLOW,
			fromOffset + 1
		));
		
		if(fullLength > 1)
			pre.append("%s:%s%d".formatted(
				AnsiUtils.Special.RESET,
				AnsiUtils.Foreground.BRIGHT_YELLOW,
				fullLength
			));
		
		pre.append("%1$s - %3$s%1$s %2$s\"%1$s%4$s%2$s\"%1$s\n\n".formatted(
			AnsiUtils.Special.RESET,
			AnsiUtils.Foreground.BRIGHT_BLACK,
			level.getColorized(),
			message
		));
		
		String result = pre.toString() + sb.toString();
		return AnsiUtils.stripAnsi(result);
	}
	
}
