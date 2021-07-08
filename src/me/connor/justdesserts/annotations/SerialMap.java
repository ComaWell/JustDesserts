package me.connor.justdesserts.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target({ FIELD, METHOD })
public @interface SerialMap {
	
	public Class<?> keyType();
	
	public Class<?> valueType();
	
}
