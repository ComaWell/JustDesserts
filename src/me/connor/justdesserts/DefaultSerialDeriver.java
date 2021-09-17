package me.connor.justdesserts;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.stream.*;

import me.connor.justdesserts.annotations.*;
import me.connor.justdesserts.exceptions.NoSerialHandlerException;
import me.connor.justdesserts.exceptions.SerialException;
import me.connor.util.*;

public class DefaultSerialDeriver implements SerialDeriver<Object, Object> {//TODO: Better exception handling
	
	@SuppressWarnings("rawtypes")
	final HashMap<Class, SerialHandler> handlers = new HashMap<>();
	
	@SuppressWarnings("rawtypes")
	final HashMap<Class, Supplier> suppliers = new HashMap<>();
	
	DefaultSerialDeriver() {
		handlers.putAll(SerialHandlers.PRIMITIVE_HANDLERS);
	}
	
	@Override
	public boolean accepts(@Nonnull Class<?> type) {
		try {
			return derive(type) != null;
		} catch (IllegalArgumentException | SerialException e) {
			return false;
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	synchronized
	public <U> SerialHandler<U, Object> derive(@Nonnull Class<U> serialType) throws IllegalArgumentException, SerialException {
		Assert.notNull(serialType);
		if (handlers.containsKey(serialType)) return handlers.get(serialType);
		
		SerialHandler handler;
		
		try {
			handler = SerialHandler.forClass(serialType);
			handlers.put(serialType, handler);
			return handler;	
		} catch (NoSerialHandlerException unused) {
			
		}
		try {
			handler = PartialSerialHandler.forClass(serialType).create(this);
			handlers.put(serialType, handler);
			return handler;	
		} catch (NoSerialHandlerException unused) {
			
		}
		
		if (serialType.isEnum()) handler = SerialHandlers.enumHandler((Class<Enum>) serialType);
		else if (serialType.isArray()) handler = SerialHandlers.arrayHandler(derive(serialType.getComponentType()));
		else {//The actual deriving can begin
			validateType(serialType);
			Supplier<U> supplier = deriveSupplier(serialType);
			Map<Field, SerialHandler> fields = SerialUtils.getFields(serialType).stream()
					.collect(Collectors.toMap(
							(f) -> f,
							(f) -> {
								try {
									f.trySetAccessible();
									Class type = f.getType();
									if (Collection.class.isAssignableFrom(type)) {
										SerialCollection anno = f.getAnnotation(SerialCollection.class);
										if (anno != null) return SerialHandlers.collectionHandler(derive(anno.type()), deriveSupplier(type));
									}
									else if (Map.class.isAssignableFrom(type)) {
										SerialMap regularAnno = f.getAnnotation(SerialMap.class);
										if (regularAnno != null) return SerialHandlers.mapHandler(derive(regularAnno.keyType()), derive(regularAnno.valueType()), deriveSupplier(type));
										SerialCompoundMap compoundAnno = f.getAnnotation(SerialCompoundMap.class);//TODO: Test this Annotation
										if (compoundAnno != null) {
											switch (compoundAnno.position()) {
												case KEY:
													return SerialHandlers.mapHandler(SerialHandlers.collectionHandler(derive(compoundAnno.keyType()), deriveSupplier(compoundAnno.collectionType())),
															derive(compoundAnno.valueType()), deriveSupplier(type));
												case VALUE:
													return SerialHandlers.mapHandler(derive(compoundAnno.keyType()), 
															SerialHandlers.collectionHandler(derive(compoundAnno.valueType()), deriveSupplier(compoundAnno.collectionType())), deriveSupplier(type));
												case BOTH:
													return SerialHandlers.mapHandler(SerialHandlers.collectionHandler(derive(compoundAnno.keyType()), deriveSupplier(compoundAnno.collectionType())), 
															SerialHandlers.collectionHandler(derive(compoundAnno.valueType()), deriveSupplier(compoundAnno.collectionType())), deriveSupplier(type));
												default:
													throw new SerialException("Unhandled SerialCompoundMap Position case");
											}
										}
												
									}
									else if (AtomicReference.class.equals(type)) {
										SerialAtomicReference anno = f.getAnnotation(SerialAtomicReference.class);
										if (anno != null) return SerialHandlers.atomicReferenceHandler(derive(anno.referenceType()));
									}
									return derive(type);
								} catch (Throwable t) {
									throw new IllegalArgumentException("Issue creating SerialHandler for Field " + f.getName() + " in Class " + serialType.getCanonicalName(), t);
								}
							}
							));
			
			if (fields.size() == 0) throw new IllegalArgumentException("Cannot Serialize an Object with no Fields");
			
			/* If there is only a single Serializable Field in an Object, then the Object could just as easily be represented as just the contents of that Field, rather
			 * than a Map with a single entry of the contents of the Field. Functionally this makes little difference, however given that the goal of SerialHandlers
			 * is to improve the readability of data, this will help mitigate redundent information.
			 */
			if (fields.size() == 1) {//TODO: Make sure this single Field deriving works
				Map.Entry<Field, SerialHandler> innerHandler = fields.entrySet().iterator().next();
				handler = innerHandler.getValue().prepend(SerialHandler.from(
						serialType,
						(s) -> {
							try {
								return innerHandler.getKey().get(s);
							} catch (Throwable t) {
								throw new SerialException("Issue Serializing singular Field " + innerHandler.getKey().getName() + " in Class " + serialType.getCanonicalName(), t);
							}
						},
						(d) -> {
							try {
								U obj = supplier.get();
								innerHandler.getKey().set(obj, d);
								return obj;
							} catch (Throwable t) {
								throw new SerialException("Issue Deserializing singular Field " + innerHandler.getKey().getName() + " in Class " + serialType.getCanonicalName(), t);
							}
						}
						));
			}
			
			else handler = SerialHandler.from(
					serialType,
					(s) -> {
					return (Map) fields.entrySet()
					.stream()
					.filter((e) -> {
						try {
							return !e.getKey().isAnnotationPresent(SerialOptional.class) || e.getKey().get(s) != null;
						} catch (Throwable t) {
							throw new SerialException("Problem enforcing SerialOptional", t);
						}
					})
					.collect(
						LinkedHashMap::new,
						(m, e) -> {
							try {
								Field f = e.getKey();
								Object v = f.get(s);
								f.trySetAccessible();
								m.put(e.getKey().getName(), (v == null ? null : e.getValue().serialize(v)));
							} catch (Throwable t) {
								throw new SerialException("Serialization Failed for Field " + e.getKey().getName() + " in Class " + s.getClass(), t);
							}
						},
						LinkedHashMap::putAll
						);
					},
					(d) -> {
						U obj = supplier.get();
						fields.entrySet()
						.stream()
						.filter((e) -> {
							try {
								return !e.getKey().isAnnotationPresent(SerialOptional.class) || d.containsKey(e.getKey().getName());
							} catch (Throwable t) {
								throw new SerialException("Problem enforcing SerialOptional", t);
							}
						})
						.forEach((e) -> {
							Field f = e.getKey();
							Object v = d.get(f.getName());
							if (!d.containsKey(f.getName())) throw new SerialException("Data for Field " + f.getName() + " not found while deserializing Class " + serialType.getCanonicalName());
							try {
								if (!f.canAccess(obj)) f.setAccessible(true);
								f.set(obj, (v == null ? null : e.getValue().deserialize(v)));
							} catch (Throwable t) {
								throw new SerialException("An Exception occured when Deserializing Field " + f.getName() + " in Class " + serialType.getCanonicalName(), t);
							}
						});
						return obj;
					});
		}
		handlers.put(serialType, handler);
		return handler;	
	}
	
	public void addHandler(@Nonnull SerialHandler<?, ?> handler) {//Note for docs: does not override any derived SerialHandler that already exists for the Handler's serialType
		Assert.notNull(handler);
		handlers.putIfAbsent(handler.serialType(), handler);
	}
	
	public <U> Supplier<U> deriveSupplier(@Nonnull Class<U> serialType) throws IllegalArgumentException, SerialException {
		Assert.notNull(serialType);
		if (suppliers.containsKey(serialType)) return suppliers.get(serialType);
		Constructor<U> constructor = SerialUtils.getConstructor(serialType);
		constructor.trySetAccessible();
		Supplier<U> supplier = () -> {
			try {
				return constructor.newInstance();
			} catch (Throwable t) {
				throw new SerialException("Could not instantiate Class " + serialType.getCanonicalName(), t);
			}
		};
		U test = supplier.get();
		if (test == null) throw new SerialException("Derived Supplier for Class " + serialType.getCanonicalName() + " returned null");
		suppliers.put(serialType, supplier);
		return supplier;
	}
	
	public void addSupplier(@Nonnull Supplier<?> supplier) throws IllegalArgumentException {//Note for docs: does not override any derived Supplier that already exists for the supplied Object's Class
		Assert.notNull(supplier);
		Object test = supplier.get();
		if (test == null) throw new IllegalArgumentException("Supplier must not return null");
		suppliers.putIfAbsent(test.getClass(), supplier);
	}
	
	static void validateType(@Nonnull Class<?> serialType) throws IllegalArgumentException {
		Assert.notNull(serialType);
		int modifiers = serialType.getModifiers();
		if (Modifier.isInterface(modifiers)) throw new SerialException("Interfaces cannot be Serialized (" + serialType.getCanonicalName() + ")");
		if (Modifier.isAbstract(modifiers)) throw new SerialException("Abstract Classes cannot be Serialized (" + serialType.getCanonicalName() + ")");
	}
	
}
