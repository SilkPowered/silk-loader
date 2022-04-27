package me.modmuss50.optifabric.compat;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(CLASS)
@Target(PARAMETER)
public @interface LoudCoerce {
	String value();

	boolean remap() default true;
}