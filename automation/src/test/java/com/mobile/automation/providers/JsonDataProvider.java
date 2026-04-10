package com.mobile.automation.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Loads a JSON array from the test classpath into TestNG {@code Object[][]} (one column per row).
 * Typical API-style layout: {@code @DataProvider} calls {@link #fromClasspath(String, Class)} with a record/POJO type.
 */
public final class JsonDataProvider {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonDataProvider() {}

    /**
     * @param classpathResource path under {@code src/test/resources} (e.g. {@code testdata/desktop/mac-login.json})
     * @param rowType         type of each JSON object in the root array
     * @return rows as {@code new Object[][] { { row0 }, { row1 }, ... }} for {@code @Test(dataProvider = ...)}
     */
    public static <T> Object[][] fromClasspath(String classpathResource, Class<T> rowType) {
        ClassLoader cl = JsonDataProvider.class.getClassLoader();
        if (cl == null) {
            throw new IllegalStateException("Cannot resolve classpath for test data");
        }
        try (InputStream in = cl.getResourceAsStream(classpathResource)) {
            if (in == null) {
                throw new IllegalStateException("Test data not found on classpath: " + classpathResource);
            }
            CollectionType listType = MAPPER.getTypeFactory().constructCollectionType(List.class, rowType);
            List<T> rows = MAPPER.readValue(in, listType);
            if (rows.isEmpty()) {
                throw new IllegalStateException("Test data has no rows: " + classpathResource);
            }
            Object[][] out = new Object[rows.size()][1];
            for (int i = 0; i < rows.size(); i++) {
                out[i][0] = rows.get(i);
            }
            return out;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read test data: " + classpathResource, e);
        }
    }
}
