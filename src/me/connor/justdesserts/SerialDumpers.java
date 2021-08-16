package me.connor.justdesserts;

import java.util.*;
import java.util.stream.*;

import me.connor.util.*;

public class SerialDumpers {
	
	public static final Map<Class<?>, SerialHandler<?, ?>> PRIMITIVE_HANDLERS = SerialHandlers.PRIMITIVE_HANDLERS.entrySet()
			.stream()
			.map((e) -> Map.entry(e.getKey(), dumpWrapper(e.getValue())))
			.collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
	
	public static <S, D> SerialHandler<S, D> dumpWrapper(@Nonnull SerialHandler<S, D> handler) {
		Assert.notNull(handler);
		return handler.prepend(SerialHandler.from(
				handler.serialType(),
				(o) -> o,
				SerialDumper::deserializer
				));
	}

	public static <S extends Enum<S>> SerialHandler<S, String> enumHandler(@Nonnull Class<S> enumClass) {
		Assert.notNull(enumClass);
		return SerialHandler.from(
				enumClass,
				Enum::name,
				SerialDumper::deserializer
				);
	}
	
	private static void checkNotArray(@Nonnull Class<?> cls) throws IllegalArgumentException {
		Assert.notNull(cls);
		if (cls.isArray()) throw new IllegalArgumentException("Class " + cls.getCanonicalName() + " is already an array");
	}
	 
	public static <S> SerialHandler<S[], Object[]> arrayHandler(@Nonnull SerialHandler<S, ?> handler) {
		Assert.notNull(handler);
		checkNotArray(handler.serialType());
		return SerialHandler.from(
				(Class<S[]>) handler.serialType().arrayType(),
				(array) -> Arrays.stream(array)
				.map(handler::serialize)
				.toArray(),
				SerialDumper::deserializer
				);
	}
	
	@SuppressWarnings("rawtypes")
	public static SerialDeriver<Collection, Collection> collectionDeriver(@Nonnull SerialDumper dumper) {
		Assert.notNull(dumper);
		return SerialDeriver.from(
				Qualifier.assignableClass(Collection.class),
				(c) -> SerialHandler.from(
						c,
						(collection) -> collection.isEmpty() ? Collections.EMPTY_LIST : ((Stream<?>) collection.parallelStream())
								.map(dumper::serialize)
								.collect(Collectors.toList()),
						SerialDumper::deserializer
						)
				);
	}
	
	@SuppressWarnings("rawtypes")
	public static SerialDeriver<Map, Map> mapDeriver(@Nonnull SerialDumper dumper) {
		Assert.notNull(dumper);
		return SerialDeriver.from(
				Qualifier.assignableClass(Map.class),
				(c) -> SerialHandler.from(
						c,
						(map) -> map.isEmpty() ? Collections.EMPTY_MAP : ((Set<Map.Entry<?, ?>>) map.entrySet())
								.stream()
								.collect(
										LinkedHashMap::new,
										(m, e) -> m.put(dumper.serialize(e.getKey()), dumper.serialize(e.getValue())),
										LinkedHashMap::putAll
										),
						SerialDumper::deserializer
						)
				);
	}
	
}
