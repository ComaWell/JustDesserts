package me.connor.justdesserts;

public interface Serial { }

/*
 * Serial TODO:
 * 
 * - Figure out a more robust solution for allowing finer control over deriving Serialization and allowing users to define behavior (similar to AnnotationHandlers) on a per-Field, per-Class basis.
 * 
 * - How can I declare for something like an AnnotationHandler that I want to use the Translator I attach it to's getSerializer/getDeserializer Methods while still being able to define the Handler statically at compile-time?
 * 
 * - Separate- Rename SerialConfig to SerialHandler and update variable names in Translator the hard-coded logic for Annotations so that Annotations and the logic for handling them can be added and omitted from Translators dynamically
 * 
 * - "SerialOptional" Annotation: To be placed over a Field that should not be Serialized if it is null and is alright to be missing data for during Deserialization
 * 
 * - "SerialReference" Annotation: To be placed over a Reference Field so that it can be Serialized into a String representation rather than Serializing the whole thing.
 * Separate behavior would need to be defined for turning a Reference into a String (toString for some Objects might be good enough) and some sort of Function would
 * need to be specified per-Class for converting the String back into a reference.
 * 
 * - Find ways to either reduce or better define bound behavior (Serial Configs that are created from and/or dependent on an instance of some other Object rather than being static or derived).
 * Since the inner-functionality of Serializers are abstracted away, this behavior is not immediately apparent to an end-user, especially when they are not accessing the Translator in the same context as
 * the bound Object. This also raises concerns about the validity of a bound Translator. If the instance of the bound Object is discarded or replaced in other aspects of a program, then the Translator will maintain an
 * effectively dead reference to said Object, and therefore its behavior when translating relevant Objects will be undefined.
*/