package me.connor.justdesserts;

import java.lang.reflect.*;
import java.util.*;

import me.connor.justdesserts.exceptions.*;
import me.connor.util.*;

public final class SerialDumper implements Translator<Object, Object> {
	
	@SuppressWarnings("rawtypes")
	private final SerialDeriver<Collection, Collection> collectionDeriver = SerialDumpers.collectionDeriver(this);
	
	@SuppressWarnings("rawtypes")
	private final SerialDeriver<Map, Map> mapDeriver = SerialDumpers.mapDeriver(this);
	
	@SuppressWarnings("rawtypes")
	private final HashMap<Class, SerialHandler> handlers = new HashMap<>();
	
	public SerialDumper() {
		handlers.putAll(SerialDumpers.PRIMITIVE_HANDLERS);
	}

	@Override
	public boolean accepts(@Nonnull Class<?> type) {//TODO: Theoretically this can derive *nearly* anything, so if it tests well this might just be able to return true
		try {
			return derive(type) != null;
		} catch (IllegalArgumentException | SerialException e) {
			return false;
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public <U> SerialHandler<U, Object> derive(@Nonnull Class<U> serialType) throws IllegalArgumentException {
		Assert.notNull(serialType);
		if (handlers.containsKey(serialType)) return handlers.get(serialType);
		synchronized(this) {
			SerialHandler handler;
			
			try {
				handler = SerialDumpers.dumpWrapper(SerialHandler.forClass(serialType));
				handlers.put(serialType, handler);
				return handler;
			} catch (NoSerialHandlerException unused) { }
			try {
				handler = SerialDumpers.dumpWrapper(PartialSerialHandler.forClass(serialType).create(this));
				handlers.put(serialType, handler);
				return handler;
			} catch (NoSerialHandlerException unused) { }
			
			if (serialType.isEnum()) handler = SerialDumpers.enumHandler((Class<Enum>) serialType);
			else if (serialType.isArray()) handler = SerialDumpers.arrayHandler(derive(serialType.getComponentType()));
			else if (Collection.class.isAssignableFrom(serialType)) handler = collectionDeriver.derive((Class<Collection>) serialType);
			else if (Map.class.isAssignableFrom(serialType)) handler = mapDeriver.derive((Class<Map>) serialType);
			else {
				validateType(serialType);
				Set<Field> fields = SerialUtils.getFields(serialType);
				if (fields.size() == 0) ;//TODO: Still don't know what to do here.
				
				if (fields.size() == 1) {
					Field f = fields.iterator().next();
					handler = SerialHandler.from(
							serialType,
							(s) -> {
								try {
									if (!f.canAccess(s)) f.setAccessible(true);
									return serialize(f.get(s));
								} catch (IllegalAccessException e) {
									throw new SerialException("Thrown for Field " + f.getName() + " in Class " + serialType.getCanonicalName(), e);
								}
							},
							SerialDumper::deserializer
							);
				}
				else handler = SerialHandler.from(
						serialType,
						(s) -> fields.stream()
						.collect(
								LinkedHashMap::new,
								(m, f) -> {
									try {
										if (!f.canAccess(s)) f.setAccessible(true);
										m.put(f.getName(), serialize(f.get(s)));
									} catch (IllegalAccessException e) {
										throw new SerialException("Thrown for Field " + f.getName() + " in Class " + serialType.getCanonicalName(), e);
									}
								},
								LinkedHashMap::putAll
								),
						SerialDumper::deserializer
						);
				
				handlers.put(serialType, handler);
			}
			return handler;
		}
	}
	
	@Override
	public <U, K> K serialize(@Nullable U obj) throws IllegalArgumentException, SerialException {
		return obj == null ? null : (K) derive((Class<U>) obj.getClass()).serialize(obj);
	}

	@Override
	public <U, K> U deserialize(@Nonnull Class<U> serialType, @Nullable K data) throws UnsupportedOperationException {
		throw unsupported();
	}
	
	static void validateType(@Nonnull Class<?> serialType) throws IllegalArgumentException {
		Assert.notNull(serialType);
		int modifiers = serialType.getModifiers();
		if (Modifier.isInterface(modifiers)) throw new SerialException("Interfaces cannot be Serialized (" + serialType.getCanonicalName() + ")");
		if (Modifier.isAbstract(modifiers)) throw new SerialException("Abstract Classes cannot be Serialized (" + serialType.getCanonicalName() + ")");
	}
	
	public static <S, D> D deserializer(@Nonnull S obj) throws UnsupportedOperationException {
		throw unsupported();
	}
	
	
	public static UnsupportedOperationException unsupported() {
		return new UnsupportedOperationException("SerialDumpers cannot Deserialize Objects");
	}

}
