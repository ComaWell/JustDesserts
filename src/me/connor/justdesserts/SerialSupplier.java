package me.connor.justdesserts;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import me.connor.justdesserts.exceptions.SerialException;
import me.connor.util.*;

public interface SerialSupplier<S> extends Qualifier<Class<?>> {

	S create(@Nonnull Class<S> serialType) throws IllegalArgumentException;
	
	public static <S> SerialSupplier<S> from(@Nonnull Qualifier<Class<?>> qualifier, @Nonnull Function<Class<S>, S> creator) {
		return new BaseSerialSupplier<>(qualifier, creator);
	}
	
	public static <S> SerialSupplier<S> from(@Nonnull Function<Class<S>, S> creator) {
		return new BaseSerialSupplier<>((c) -> {
			try {
				return creator.apply((Class<S>) c) != null;
			} catch (Throwable t) {
				return false;
			}
		}, creator);
	}
	
	public static <S> SerialSupplier<S> from(@Nonnull Supplier<S> supplier) {
		return from(
				Qualifier.exactClass(supplier.get().getClass()),//This should be fine; SerialSuppliers by definition should be stateless and return a new instance on every invocation
				(c) -> supplier.get()
				);
	}
	
	@SuppressWarnings("rawtypes")
	public static <S> SerialSupplier<S> merge(@Nonnull SerialSupplier<? extends S>...suppliers) {
		Assert.notNull(suppliers);
		List<SerialSupplier> list = List.of(suppliers);
		return from(
				Qualifier.merge(Qualifier.ReturnCondition.ANY_ACCEPTS, suppliers),
				(c) -> {
					for (SerialSupplier supplier : list) {
						if (supplier.accepts(c)) return (S) supplier.create(c);
					}
					throw new IllegalArgumentException("This SerialSupplier does not accept Class " + c.getCanonicalName());
				}
				);
	}
	
	public static SerialSupplier<Object> NO_ARG_SUPPLIER = from(
			(c) -> {
				try {
					return c.getDeclaredConstructor() != null;
				} catch (NoSuchMethodException e) {
					return false;
				}	
			},
			(c) -> {
				try {
					Constructor<?> con = c.getDeclaredConstructor();
					con.setAccessible(true);
					return con.newInstance();
				} catch (NoSuchMethodException e) {
					throw new IllegalArgumentException("A valid no-args Constructor does not exist for Class " + c.getCanonicalName());
				} catch (Throwable t) {
					throw new SerialException("Could not instantiate Class " + c.getCanonicalName(), t);
				}
			}
			);
	
}
