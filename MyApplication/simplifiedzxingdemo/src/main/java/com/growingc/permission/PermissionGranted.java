package com.growingc.permission;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by RB-cgy on 2016/8/11.
 * Register a method invoked when permission requests are succeeded.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PermissionGranted {
    int requestCode();
}
