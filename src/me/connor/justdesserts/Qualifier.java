package me.connor.justdesserts;

import java.lang.reflect.*;
import java.util.*;

import me.connor.util.*;

@FunctionalInterface
public interface Qualifier<T> {

	boolean accepts(@Nonnull T type);
	
	public default Qualifier<T> invert() {
		return (t) -> !accepts(t);
	}
	
	public static final Qualifier<Field> TRANSIENT_FIELD_QUALIFIER = (f) -> !Modifier.isTransient(f.getModifiers());
	
	public static final Qualifier<Field> STATIC_FIELD_QUALIFIER = (f) -> !Modifier.isStatic(f.getModifiers());
	
	public static Qualifier<Class<?>> exactClass(@Nonnull Class<?> serialClass) {
		Assert.notNull(serialClass);
		return (c) -> serialClass.equals(c);
	}
	
	public static Qualifier<Class<?>> assignableClass(@Nonnull Class<?> serialClass) {
		Assert.notNull(serialClass);
		return (c) -> serialClass.isAssignableFrom(c);
	}
	
	public static enum ReturnCondition {//Note for docs: This Enum describes condition under which a merged Qualifier returns true, e.g. a Qualifier set as ALL_ACCEPTS returns true only if ALL merged Qualifiers accept the input
		
		ALL_ACCEPTS,
		ANY_ACCEPTS,
		NONE_ACCEPTS,
		
	}
	
	public static <T> Qualifier<T> merge(@Nonnull ReturnCondition condition, @Nonnull Qualifier<T>...qualifiers) {//TODO: When I'm not lazy make the enum provide the ReturnCondition logic rather than this Method
		Assert.allNotNull(condition, qualifiers);
		List<Qualifier<T>> list = List.of(qualifiers);
		switch (condition) {
			case ALL_ACCEPTS:
				return (t) -> {
					for (Qualifier<T> qualifier : list) {
						if (!qualifier.accepts(t)) return false;
					}
					return true;
				};
			case ANY_ACCEPTS:
				return (t) -> {
					for (Qualifier<T> qualifier : list) {
						if (qualifier.accepts(t)) return true;
					}
					return false;
				};
			case NONE_ACCEPTS:
				return (t) -> {
					for (Qualifier<T> qualifier : list) {
						if (qualifier.accepts(t)) return false;
					}
					return true;
				};
			default:
				throw new UnsupportedOperationException("Missing Condition block for " + condition.toString() + ". My bad, not yours");
		}
			
		
	}
	
}
