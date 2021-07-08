package me.connor.justdesserts.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Collection;

@Retention(RUNTIME)
@Target(FIELD)
public @interface SerialCompoundMap {

	public Class<?> keyType();
	
	public Class<?> valueType();
	
	@SuppressWarnings("rawtypes")
	public Class<? extends Collection> collectionType();
	
	public Position position() default Position.VALUE; 
	
	public static enum Position {
		KEY,
		VALUE,
		BOTH
	}
}
