package com.veggiepal.blog.enums;

public enum TargetType {
    BLOG,

    /**
     * Declared before videos exist on purpose. Hibernate maps this enum to a native
     * MySQL ENUM column and ddl-auto=update will NOT add a constant later — adding
     * it then would need a manual ALTER TABLE on every environment.
     */
    VIDEO
}
