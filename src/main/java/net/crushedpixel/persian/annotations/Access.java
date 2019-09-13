package net.crushedpixel.persian.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Access {

    enum AccessType {
        FIELD, METHOD
    }

    AccessType value() default AccessType.METHOD;

}
