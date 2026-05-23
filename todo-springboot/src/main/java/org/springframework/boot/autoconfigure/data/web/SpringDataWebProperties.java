package org.springframework.boot.autoconfigure.data.web;

/**
 * Compatibility shim for Spring Cloud OpenFeign on Spring Boot 4.
 * Provides only the fields used by Feign's Pageable support.
 */
public class SpringDataWebProperties {

    private final Pageable pageable = new Pageable();

    public Pageable getPageable() {
        return pageable;
    }

    public static class Pageable {
        private int maxPageSize = 2000;

        public int getMaxPageSize() {
            return maxPageSize;
        }

        public void setMaxPageSize(int maxPageSize) {
            this.maxPageSize = maxPageSize;
        }
    }
}
