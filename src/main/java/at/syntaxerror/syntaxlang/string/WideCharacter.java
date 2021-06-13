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
package at.syntaxerror.syntaxlang.string;

import at.syntaxerror.syntaxlang.SyntaxLangException;
import at.syntaxerror.syntaxlang.misc.ExceptionSupplier;
import lombok.Getter;
import lombok.NonNull;

/**
 * @author SyntaxError
 * 
 */
public final class WideCharacter {
	
	private static int parse(@NonNull ExceptionSupplier<Byte, Exception> byteSupplier) {
		try {
			// U+0000 - U+007F
			
			int b1 = byteSupplier.get() & 0xFF;
			
			if((b1 & 0x80) == 0x00) // 0xxxxxxx
				return (char) b1;

			// U+0080 - U+07FF
			
			int b2 = byteSupplier.get() & 0xFF;
			
			if((b2 & 0xC0) != 0x80)
				throw new UTFException("Invalid UTF-8 codepoint (byte 2)");
			
			b2 &= 0x3F;
			
			if((b1 & 0xE0) == 0xC0) // 110xxxxx 10xxxxxx
				return ((b1 & 0x1F) << 6) | b2;
			
			// U+0800 - U+FFFF

			int b3 = byteSupplier.get() & 0xFF;
			
			if((b3 & 0xC0) != 0x80)
				throw new UTFException("Invalid UTF-8 codepoint (byte 3)");
			
			b3 &= 0x3F;
			
			if((b1 & 0xF0) == 0xE0) // 1110xxxx 10xxxxxx 10xxxxxx
				return ((b1 & 0x0F) << 12) | (b2 << 6) | b3;
			
			// U+10000 - U+10FFF

			int b4 = byteSupplier.get() & 0xFF;
			
			if((b4 & 0xC0) != 0x80)
				throw new UTFException("Invalid UTF-8 codepoint (byte 4)");
			
			b4 &= 0x3F;
			
			if((b1 & 0xF8) == 0xF0) // 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
				return ((b1 & 0x07) << 18) | (b2 << 12) | (b3 << 6) | b4;

			throw new UTFException("Invalid UTF-8 codepoint (byte 1)");
		} catch (SyntaxLangException e) {
			throw e;
		} catch (Exception e) {
			throw new SyntaxLangException("Could not parse UTF-8", e);
		}
	}

	@Getter
	private final int codepoint;
	private final char[] chars;
	
	public WideCharacter(int codepoint) {
		if(codepoint < 0 || codepoint > 0x10FFFF)
			throw new UTFException("UTF-8 codepoint out of range: 0x" + Integer.toHexString(codepoint));
		
		this.codepoint = codepoint;
		
		chars = Character.toChars(codepoint);
	}
	public WideCharacter(@NonNull ExceptionSupplier<Byte, Exception> byteSupplier) {
		this(parse(byteSupplier));
	}
	
	public boolean inRange(int lo, int hi) {
		return codepoint >= lo && codepoint <= hi;
	}
	public boolean inAnyRange(int[][] ranges) {
		for(int[] range : ranges)
			if(inRange(range[0], range[1]))
				return true;
		return false;
	}
	public boolean inAllRanges(int[][] ranges) {
		for(int[] range : ranges)
			if(!inRange(range[0], range[1]))
				return false;
		return true;
	}
	
	public boolean isSurrogate() {
		return chars.length != 1;
	}
	public char getHigh() {
		return chars[0];
	}
	public char getLow() {
		return isSurrogate() ? chars[1] : 0;
	}
	
	public int width() {
		return WCWidthData.width(codepoint);
	}
	
	public int byteCount() {
		if(codepoint <= 0x007F) return 1;
		if(codepoint <= 0x07FF) return 2;
		if(codepoint <= 0xFFFF) return 3;
		return 4;
	}
	
	public byte[] getBytes() {
		return switch(byteCount()) {
		case 4 -> new byte[] {
			(byte) (0xF0 | ((codepoint >> 18) & 0x07)),
			(byte) (0x80 | ((codepoint >> 12) & 0x3F)),
			(byte) (0x80 | ((codepoint >> 6) & 0x3F)),
			(byte) (0x80 | (codepoint & 0x3F)),
		};
		case 3 -> new byte[] {
			(byte) (0xE0 | ((codepoint >> 12) & 0x0F)),
			(byte) (0x80 | ((codepoint >> 6) & 0x3F)),
			(byte) (0x80 | (codepoint & 0x3F)),
		};
		case 2 -> new byte[] {
			(byte) (0xC0 | ((codepoint >> 6) & 0x1F)),
			(byte) (0x80 | (codepoint & 0x3F))
		};
		case 1 -> new byte[] { (byte) codepoint };
		default -> throw new IllegalArgumentException();
		};
	}
	
	@Override
	public String toString() {
		return isSurrogate() ? 
			String.valueOf(new char[] { getHigh(), getLow() }) : 
			String.valueOf(getHigh());
	}
	
	public String toVerboseString() {
		return "%s (U+%04X)".formatted(toString(), codepoint);
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof WideCharacter c && c.codepoint == codepoint;
	}

}
