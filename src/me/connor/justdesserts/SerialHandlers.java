package me.connor.justdesserts;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.stream.*;

import me.connor.util.*;

public class SerialHandlers {
	
	public static final SerialHandler<Boolean, Boolean> BOOLEAN_HANDLER = SerialHandler.from(Boolean.class, (b) -> b, (b) -> b);
	public static final SerialHandler<Boolean, Boolean> PRIM_BOOLEAN_HANDLER = SerialHandler.from(boolean.class, (b) -> b, (b) -> b);
	public static final SerialHandler<Byte, Number> BYTE_HANDLER = SerialHandler.from(Byte.class, Number::longValue, Number::byteValue);
	public static final SerialHandler<Byte, Number> PRIM_BYTE_HANDLER = SerialHandler.from(byte.class, Number::longValue, Number::byteValue);
	public static final SerialHandler<Short, Number> SHORT_HANDLER = SerialHandler.from(Short.class, Number::longValue, Number::shortValue);
	public static final SerialHandler<Short, Number> PRIM_SHORT_HANDLER = SerialHandler.from(short.class, Number::longValue, Number::shortValue);
	public static final SerialHandler<Integer, Number> INTEGER_HANDLER = SerialHandler.from(Integer.class, Number::longValue, Number::intValue);
	public static final SerialHandler<Integer, Number> PRIM_INTEGER_HANDLER = SerialHandler.from(int.class, Number::longValue, Number::intValue);
	public static final SerialHandler<Long, Number> LONG_HANDLER = SerialHandler.from(Long.class, Number::longValue, Number::longValue);
	public static final SerialHandler<Long, Number> PRIM_LONG_HANDLER = SerialHandler.from(long.class, Number::longValue, Number::longValue);
	public static final SerialHandler<Float, Number> FLOAT_HANDLER = SerialHandler.from(Float.class, Number::doubleValue, Number::floatValue);
	public static final SerialHandler<Float, Number> PRIM_FLOAT_HANDLER = SerialHandler.from(float.class, Number::doubleValue, Number::floatValue);
	public static final SerialHandler<Double, Number> DOUBLE_HANDLER = SerialHandler.from(Double.class, Number::doubleValue, Number::doubleValue);
	public static final SerialHandler<Double, Number> PRIM_DOUBLE_HANDLER = SerialHandler.from(double.class, Number::doubleValue, Number::doubleValue);
	public static final SerialHandler<Character, String> CHARACTER_HANDLER = SerialHandler.from(Character.class, (c) -> c.toString(), (s) -> {
		if (s.length() != 1) throw new IllegalArgumentException("Input is not the correct length to be a Character");
		return s.charAt(0);
	});
	public static final SerialHandler<Character, String> PRIM_CHARACTER_HANDLER = SerialHandler.from(char.class, (c) -> c.toString(), (s) -> {
		if (s.length() != 1) throw new IllegalArgumentException("Input is not the correct length to be a Character");
		return s.charAt(0);
	});
	public static final SerialHandler<String, String> STRING_HANDLER = SerialHandler.from(String.class, (s) -> s, (s) -> s);
	
	public static final Map<Class<?>, SerialHandler<?, ?>> PRIMITIVE_HANDLERS = Map.ofEntries(
			Map.entry(Boolean.class, 	BOOLEAN_HANDLER),
			Map.entry(boolean.class, 	PRIM_BOOLEAN_HANDLER),
			Map.entry(Byte.class, 		BYTE_HANDLER),
			Map.entry(byte.class, 		PRIM_BYTE_HANDLER),
			Map.entry(Short.class, 		SHORT_HANDLER),
			Map.entry(short.class, 		PRIM_SHORT_HANDLER),
			Map.entry(Integer.class,	INTEGER_HANDLER),
			Map.entry(int.class, 		PRIM_INTEGER_HANDLER),
			Map.entry(Long.class, 		LONG_HANDLER),
			Map.entry(long.class,		PRIM_LONG_HANDLER),
			Map.entry(Float.class,		FLOAT_HANDLER),
			Map.entry(float.class,		PRIM_FLOAT_HANDLER),
			Map.entry(Double.class,		DOUBLE_HANDLER),
			Map.entry(double.class, 	PRIM_DOUBLE_HANDLER),
			Map.entry(Character.class,  CHARACTER_HANDLER),
			Map.entry(char.class, 		PRIM_CHARACTER_HANDLER),
			Map.entry(String.class, 	STRING_HANDLER)
			);
	
	public static <S extends Enum<S>> SerialHandler<S, String> enumHandler(@Nonnull Class<S> enumClass) {
		Assert.notNull(enumClass);
		return SerialHandler.from(
				enumClass,
				Enum::name,
				(s) -> Enum.valueOf(enumClass, s)
				);
	}
	
	private static <T> T[] createArray(@Nonnull Class<T> type) {
		Assert.notNull(type);
		checkNotArray(type);
		return (T[]) Array.newInstance(type, 0);
	}
	
	public static <S, D> SerialHandler<S[], Collection<D>> arrayHandler(@Nonnull SerialHandler<S, D> handler) {
		Assert.notNull(handler);
		checkNotArray(handler.serialType());
		return collectionHandler(handler, ArrayList::new).prepend(SerialHandler.from(
				(Class<S[]>) handler.serialType().arrayType(),
				(array) -> Arrays.stream(array)
				.collect(Collectors.toCollection(ArrayList::new)),
				(data) -> data.toArray(createArray(handler.serialType()))
				));
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
	
	@SuppressWarnings("rawtypes")
	public static <S, D> SerialHandler<AtomicReference, D> atomicReferenceHandler(@Nonnull SerialHandler<S, D> handler) {
		Assert.notNull(handler);
		return SerialHandler.from(
				AtomicReference.class,
				(r) -> handler.serialize((S) r.get()),
				(d) -> new AtomicReference<S>(handler.deserialize(d))
				);
	}
	
}
