package com.forentrack.web.seguridad;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.forentrack.web.model.Permiso;

/** Marca un método de controlador con el permiso que exige; lo verifica SesionInterceptor (403 si no lo tiene). */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequierePermiso {
    Permiso value();
}
