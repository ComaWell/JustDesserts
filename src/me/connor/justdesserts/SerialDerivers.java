package me.connor.justdesserts;

import java.util.*;

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

	public static <S, D> SerialDeriver<S[], Collection<D>> arrayDeriver(@Nonnull SerialDeriver<S, D> deriver) {
		Assert.allNotNull(deriver);
		return SerialDeriver.from(
				(c) -> c.isArray() && deriver.accepts(c.componentType()),
				(c) -> SerialHandlers.arrayHandler(deriver.derive((Class<S>) c))//TODO: Test
				);
	}
	
	public static DefaultSerialDeriver newDefault() {
		return new DefaultSerialDeriver();
	}
	
}
