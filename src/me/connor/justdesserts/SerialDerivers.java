package me.connor.justdesserts;

import java.util.function.*;

import me.connor.util.*;

public class SerialDerivers {
	
	public static final SerialDeriver<?, ?> PRIMITIVE_DERIVER = SerialDeriver.from(
			SerialHandlers.PRIMITIVE_HANDLERS::containsKey,
			SerialHandlers.PRIMITIVE_HANDLERS::get
			);
	
	@SuppressWarnings("rawtypes")
	public static final SerialDeriver<Enum, String> ENUM_DERIVER = SerialDeriver.from(
			Class::isEnum,
			SerialHandlers::enumHandler
			);

	public static <S, D> SerialDeriver<S[], D[]> arrayDeriver(@Nonnull SerialDeriver<S, D> deriver, @Nonnull IntFunction<S[]> serialGenerator, @Nonnull IntFunction<D[]> deserialGenerator) {
		Assert.allNotNull(deriver, serialGenerator, deserialGenerator);
		return SerialDeriver.from(
				(c) -> c.isArray() && deriver.accepts(c.componentType()),
				(c) -> SerialHandlers.arrayHandler(deriver.derive((Class<S>) c), serialGenerator, deserialGenerator)//TODO: Test
				);
	}
	
	@SuppressWarnings("rawtypes")
	public static SerialDeriver<Object[], Object[]> anonymousArrayDeriver(@Nonnull SerialDeriver deriver) {
		Assert.notNull(deriver);
		return SerialDeriver.from(
				(c) -> c.isArray() && deriver.accepts(c.componentType()),
				(c) -> SerialHandlers.anonymousArrayHandler(deriver.derive(c))
				);
	}
	
	public static DefaultSerialDeriver newDefault() {
		return new DefaultSerialDeriver();
	}
	
}
