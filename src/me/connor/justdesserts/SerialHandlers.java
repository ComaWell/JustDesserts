package me.connor.justdesserts;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import me.connor.util.*;

public class SerialHandlers {
	
	public static final Map<Class<?>, SerialHandler<?, ?>> PRIMITIVE_HANDLERS = Map.ofEntries(
			Map.entry(Boolean.class, 	SerialHandler.from(Boolean.class, 	(b) -> b.toString(), Boolean::valueOf)),
			Map.entry(boolean.class, 	SerialHandler.from(boolean.class, 	(b) -> Boolean.toString(b), Boolean::valueOf)),
			Map.entry(Byte.class, 		SerialHandler.from(Byte.class, 		(b) -> b.toString(), Byte::valueOf)),
			Map.entry(byte.class, 		SerialHandler.from(byte.class, 		(b) -> Byte.toString(b), Byte::valueOf)),
			Map.entry(Short.class, 		SerialHandler.from(Short.class, 	(s) -> s.toString(), Short::valueOf)),
			Map.entry(short.class, 		SerialHandler.from(short.class, 	(s) -> Short.toString(s), Short::valueOf)),
			Map.entry(Integer.class,	SerialHandler.from(Integer.class, 	(i) -> i.toString(), Integer::valueOf)),
			Map.entry(int.class, 		SerialHandler.from(int.class, 		(i) -> Integer.toString(i), Integer::valueOf)),
			Map.entry(Long.class, 		SerialHandler.from(Long.class, 		(l) -> l.toString(), Long::valueOf)),
			Map.entry(long.class,		SerialHandler.from(long.class, 		(l) -> Long.toString(l), Long::valueOf)),
			Map.entry(Float.class,		SerialHandler.from(Float.class, 	(f) -> f.toString(), Float::valueOf)),
			Map.entry(float.class,		SerialHandler.from(float.class, 	(f) -> Float.toString(f), Float::valueOf)),
			Map.entry(Double.class,		SerialHandler.from(Double.class, 	(d) -> d.toString(), Double::valueOf)),
			Map.entry(double.class, 	SerialHandler.from(double.class, 	(d) -> Double.toString(d), Double::valueOf)),
			Map.entry(Character.class, 	SerialHandler.from(Character.class, (c) -> c.toString(), (s) -> s.charAt(0))),//TODO: These Character Deserializers are not very good, make them better later
			Map.entry(char.class, 		SerialHandler.from(char.class, 		(c) -> Character.toString(c), (s) -> s.charAt(0))),
			Map.entry(String.class, 	SerialHandler.from(String.class, 	(o) -> o, (o) -> o))
			);
	
	public static <S extends Enum<S>> SerialHandler<S, String> enumHandler(@Nonnull Class<S> enumClass) {
		Assert.notNull(enumClass);
		return SerialHandler.from(
				enumClass,
				Enum::name,
				(s) -> Enum.valueOf(enumClass, s)
				);
	}
	
	public static <S, D> SerialHandler<S[], D[]> arrayHandler(@Nonnull SerialHandler<S, D> handler, @Nonnull IntFunction<S[]> serialGenerator, @Nonnull IntFunction<D[]> dataGenerator) {
		Assert.allNotNull(handler, serialGenerator, dataGenerator);
		checkNotArray(handler.serialType());
		return SerialHandler.from(
				(Class<S[]>) handler.getClass().arrayType(),
				(array) -> Arrays.stream(array)
				.map(handler::serialize)
				.toArray(dataGenerator),
				(data) -> Arrays.stream(data)
				.map(handler::deserialize)
				.toArray(serialGenerator)
				);
	}
	
	@SuppressWarnings("rawtypes")
	public static SerialHandler<Object[], Object[]> anonymousArrayHandler(@Nonnull SerialHandler handler) {
		Assert.notNull(handler);
		checkNotArray(handler.serialType());
		return SerialHandler.from(
				(Class<Object[]>) handler.getClass().arrayType(),
				(array) -> Arrays.stream(array)
				.map(handler::serialize)
				.toArray(),
				(data) -> Arrays.stream(data)
				.map(handler::deserialize)
				.toArray()
				);
	}
	
	private static void checkNotArray(@Nonnull Class<?> cls) throws IllegalArgumentException {
		Assert.notNull(cls);
		if (cls.isArray()) throw new IllegalArgumentException("Class " + cls.getCanonicalName() + " is already an array");
	}
	
	public static <S, D, C extends Collection<S>> SerialHandler<C, Collection<D>> collectionHandler(@Nonnull SerialHandler<S, D> handler, @Nonnull Supplier<C> supplier) {
		Assert.allNotNull(handler, supplier);
		return SerialHandler.from(
				(Class<C>) supplier.get().getClass(),
				(collection) -> collection.isEmpty() ? Collections.EMPTY_LIST : collection.parallelStream()
						.map(handler::serialize)
						.collect(Collectors.toList()),
				(collection) -> collection.isEmpty() ? supplier.get() : collection.parallelStream()
						.map(handler::deserialize)
						.collect(Collectors.toCollection(supplier))
				);
	}

	public static <SK, SV, DK, DV, M extends Map<SK, SV>> SerialHandler<M, Map<DK, DV>> mapHandler(@Nonnull SerialHandler<SK, DK> keyHandler, @Nonnull SerialHandler<SV, DV> valueHandler, @Nonnull Supplier<M> supplier) {
		Assert.allNotNull(keyHandler, valueHandler, supplier);
		return SerialHandler.from(
				(Class<M>) supplier.get().getClass(),
				(map) -> map.isEmpty() ? Collections.EMPTY_MAP : map.entrySet()
						.stream()
						.collect(
								LinkedHashMap::new,
								(m, e) -> m.put(keyHandler.serialize(e.getKey()), valueHandler.serialize(e.getValue())),
								LinkedHashMap::putAll
								),
				(map) ->  map.isEmpty() ? supplier.get() : map.entrySet()
						.stream()
						.collect(
								supplier,
								(m, e) -> m.put(keyHandler.deserialize(e.getKey()), valueHandler.deserialize(e.getValue())),
								Map::putAll
								)
						);
	}
	
}
