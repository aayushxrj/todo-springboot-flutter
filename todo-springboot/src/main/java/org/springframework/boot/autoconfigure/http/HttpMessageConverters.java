package org.springframework.boot.autoconfigure.http;

import org.springframework.http.converter.HttpMessageConverter;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Compatibility shim for Spring Cloud OpenFeign on Spring Boot 4.
 */
public class HttpMessageConverters implements Iterable<HttpMessageConverter<?>> {

    private final List<HttpMessageConverter<?>> converters;

    public HttpMessageConverters() {
        this(List.of());
    }

    public HttpMessageConverters(HttpMessageConverter<?>... converters) {
        this(Arrays.asList(converters));
    }

    public HttpMessageConverters(List<HttpMessageConverter<?>> converters) {
        this.converters = List.copyOf(converters);
    }

    public List<HttpMessageConverter<?>> getConverters() {
        return converters;
    }

    @Override
    public Iterator<HttpMessageConverter<?>> iterator() {
        return converters.iterator();
    }
}
