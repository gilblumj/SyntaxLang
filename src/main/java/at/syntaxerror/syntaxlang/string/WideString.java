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

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import lombok.NonNull;

/**
 * @author SyntaxError
 * 
 */
public final class WideString {
	
	public static WideString[] convert(String[] strings) {
		WideString[] wstrings = new WideString[strings.length];
		
		for(int i = 0; i < strings.length; ++i)
			wstrings[i] = new WideString(strings[i]);
		
		return wstrings;
	}
	
	private final WideCharacter[] data;
	private final int rawLength;
	
	public WideString(@NonNull String data) {
		this(data.getBytes());
	}
	
	public WideString(@NonNull byte[] data) {
		WideCharacter[] codepoints = new WideCharacter[data.length];
		
		int length = 0;
		AtomicInteger i = new AtomicInteger();
		
		while(i.get() < data.length)
			codepoints[length++] = new WideCharacter(() -> data[i.getAndIncrement()]);
		
		this.data = Arrays.copyOfRange(codepoints, 0, length);
		this.rawLength = data.length;
	}
	
	public WideString(@NonNull WideCharacter[] data) {
		this.data = data;
		
		int raw = 0;
		
		for(WideCharacter c : data)
			raw += c.byteCount();
		
		rawLength = raw;
	}
	
	public WideString() {
		data = new WideCharacter[0];
		rawLength = 0;
	}
	
	WideString(@NonNull WideCharacter[] data, int raw) {
		this.data = data;
		rawLength = raw;
	}
	
	public int length() {
		return data.length;
	}
	public int rawLength() {
		return rawLength;
	}
	
	public boolean isEmpty() {
		return data.length == 0;
	}
	
	public WideCharacter charAt(int index) {
		return data[index];
	}
	
	public int width() {
		int width = 0;
		
		for(WideCharacter c : data)
			width += c.width();
		
		return width;
	}
	
	public byte[] getBytes() {
		try(ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			for(int i = 0; i < length(); ++i)
				baos.write(data[i].getBytes());
			
			return baos.toByteArray();
		} catch (Exception e) {
			throw new UTFException("Could not convert string to UTF-8");
		}
	}
	
	public WideCharacter[] toCharArray() {
		return data.clone();
	}
	
	@Override
	public String toString() {
		String strrep = "";
		
		for(WideCharacter cp : data)
			strrep += cp;
		
		return strrep;
	}

	public <R> R stringOp(@NonNull Function<String, R> op) {
		return op.apply(toString());
	}
	public WideString[] stringArrayOp(@NonNull Function<String, String[]> op) {
		return convert(stringOp(op));
	}
	public WideString stringModOp(@NonNull Function<String, String> op) {
		return new WideString(stringOp(op));
	}
	public WideString stringByteOp(@NonNull Function<String, byte[]> op) {
		return new WideString(stringOp(op));
	}
	
	public WideString substring(int from, int to) {
		if(from < 0 || from > to || to > length())
			throw new StringIndexOutOfBoundsException("begin " + from + ", end " + to + ", length " + length());
		
		WideCharacter[] chars = new WideCharacter[to - from];
		System.arraycopy(data, from, chars, 0, chars.length);
		
		return new WideString(chars);
	}
	
	public WideString concat(@NonNull WideCharacter c) {
		WideCharacter[] ndata = new WideCharacter[length() + 1];
		System.arraycopy(data, 0, ndata, 0, length());
		ndata[length()] = c;
		return new WideString(ndata, rawLength() + c.byteCount());
	}
	public WideString concat(char c) {
		return concat(new WideCharacter(c));
	}
	
	public WideString concat(@NonNull WideString s) {
		WideCharacter[] ndata = new WideCharacter[length() + s.length()];
		System.arraycopy(data, 0, ndata, 0, length());
		System.arraycopy(s.data, 0, ndata, length(), s.length());
		return new WideString(ndata, rawLength() + s.rawLength());
	}
	public WideString concat(String s) {
		return concat(new WideString(s));
	}

	public int count(WideCharacter c) {
		return count(c.getCodepoint());
	}
	public int count(int cp) {
		int count = 0;
		
		for(WideCharacter c : data)
			if(c.getCodepoint() == cp)
				++count;
		
		return count;
	}

}
