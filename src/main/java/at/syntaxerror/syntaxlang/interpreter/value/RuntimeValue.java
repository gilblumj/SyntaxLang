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
package at.syntaxerror.syntaxlang.interpreter.value;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import at.syntaxerror.syntaxlang.SyntaxLangException;
import at.syntaxerror.syntaxlang.interpreter.object.classlike.RuntimeClassLikeInstance;
import at.syntaxerror.syntaxlang.string.WideString;
import at.syntaxerror.syntaxlang.trace.Position;
import ch.obermuhlner.math.big.BigComplex;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * @author SyntaxError
 * 
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RuntimeValue {
	
	public static RuntimeValue of(Position pos, WideString str) {
		return of(pos).setString(str);
	}
	public static RuntimeValue of(Position pos, String str) {
		return of(pos, new WideString(str));
	}
	
	public static RuntimeValue of(Position pos, BigDecimal num) {
		return of(pos).setNumber(num);
	}
	public static RuntimeValue of(Position pos, Number num) {
		return of(pos, BigDecimal.valueOf(num.doubleValue()));
	}
	
	public static RuntimeValue of(Position pos, BigComplex complex) {
		return of(pos).setComplex(complex);
	}
	
	public static RuntimeValue of(Position pos, boolean bool) {
		return of(pos).setBoolean(bool);
	}

	public static RuntimeValue of(Position pos, List<RuntimeValue> list) {
		return of(pos).setList(list);
	}

	public static RuntimeValue of(Position pos, Map<RuntimeValue, RuntimeValue> map) {
		return of(pos).setMap(map);
	}

	@Deprecated
	public static RuntimeValue of0(Position pos, Object func) { // TODO
		return of(pos).setFunction(func);
	}

	public static RuntimeValue of(Position pos, RuntimeClassLikeInstance obj) {
		return of(pos).setObject(obj);
	}
	
	public static RuntimeValue of(Position pos) {
		return new RuntimeValue(pos);
	}
	
	@NonNull
	private ValueType type = ValueType.NULL;
	private Object rawValue = null;
	@NonNull
	private final Position position;
	
	public boolean isString() {
		return type == ValueType.STRING;
	}
	public boolean isNumber() {
		return type == ValueType.NUMBER;
	}
	public boolean isComplex() {
		return type == ValueType.COMPLEX;
	}
	public boolean isBoolean() {
		return type == ValueType.BOOLEAN;
	}
	public boolean isList() {
		return type == ValueType.LIST;
	}
	public boolean isMap() {
		return type == ValueType.MAP;
	}
	public boolean isNull() {
		return type == ValueType.NULL;
	}
	public boolean isFunction() {
		return type == ValueType.FUNCTION;
	}
	public boolean isObject() {
		return type == ValueType.OBJECT;
	}
	
	private void ensureType(ValueType type) {
		if(this.type != type)
			throw new SyntaxLangException("Expected value to be of type %s, got %s instead at position %s"
					.formatted(type, this.type, position));
	}
	
	public WideString stringValue() {
		ensureType(ValueType.STRING);
		return (WideString) rawValue;
	}
	public BigDecimal numberValue() {
		ensureType(ValueType.NUMBER);
		return (BigDecimal) rawValue;
	}
	public BigComplex complexValue() {
		ensureType(ValueType.COMPLEX);
		return (BigComplex) rawValue;
	}
	public Boolean booleanValue() {
		ensureType(ValueType.BOOLEAN);
		return (Boolean) rawValue;
	}
	@SuppressWarnings("unchecked")
	public List<RuntimeValue> listValue() {
		ensureType(ValueType.BOOLEAN);
		return (List<RuntimeValue>) rawValue;
	}
	@SuppressWarnings("unchecked")
	public Map<RuntimeValue, RuntimeValue> mapValue() {
		ensureType(ValueType.BOOLEAN);
		return (Map<RuntimeValue, RuntimeValue>) rawValue;
	}
	@Deprecated
	public Object functionValue() { // TODO
		ensureType(ValueType.FUNCTION);
		return (Object) rawValue;
	}
	public RuntimeClassLikeInstance objectValue() {
		ensureType(ValueType.OBJECT);
		return (RuntimeClassLikeInstance) rawValue;
	}
	
	public RuntimeClassLikeInstance getAsObject() {
		if(isObject())
			return (RuntimeClassLikeInstance) rawValue;
		
		if(type == ValueType.NULL)
			throw new SyntaxLangException("Value is null at position %s"
					.formatted(position));

		if(type == ValueType.FUNCTION)
			throw new SyntaxLangException("Value is an anonymous function at position %s"
					.formatted(position));
		
		return switch(type) { // TODO
		case STRING -> null;
		case NUMBER -> null;
		case COMPLEX -> null;
		case BOOLEAN -> null;
		case LIST -> null;
		case MAP -> null;
		default -> null;
		};
	}

	public RuntimeValue setString(@NonNull WideString v) {
		rawValue = v;
		type = ValueType.STRING;
		return this;
	}
	public RuntimeValue setNumber(@NonNull BigDecimal v) {
		rawValue = v;
		type = ValueType.NUMBER;
		return this;
	}
	public RuntimeValue setComplex(@NonNull BigComplex v) {
		rawValue = v;
		type = ValueType.COMPLEX;
		return this;
	}
	public RuntimeValue setBoolean(@NonNull Boolean v) {
		rawValue = v;
		type = ValueType.BOOLEAN;
		return this;
	}
	public RuntimeValue setList(@NonNull List<RuntimeValue> v) {
		rawValue = v;
		type = ValueType.LIST;
		return this;
	}
	public RuntimeValue setMap(@NonNull Map<RuntimeValue, RuntimeValue> v) {
		rawValue = v;
		type = ValueType.MAP;
		return this;
	}
	public RuntimeValue setNull() {
		rawValue = null;
		type = ValueType.NULL;
		return this;
	}
	@Deprecated
	public RuntimeValue setFunction(@NonNull Object v) { // TODO
		rawValue = v;
		type = ValueType.BOOLEAN;
		return this;
	}
	public RuntimeValue setObject(@NonNull RuntimeClassLikeInstance v) {
		rawValue = v;
		type = ValueType.OBJECT;
		return this;
	}
	
	public RuntimeValue at(Position position) {
		return new RuntimeValue(type, rawValue, position);
	}
	
}
