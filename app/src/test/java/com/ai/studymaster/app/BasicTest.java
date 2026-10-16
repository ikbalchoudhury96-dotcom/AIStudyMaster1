package com.ai.studymaster.app;

import static org.junit.Assert.*;
import org.junit.Test;

public class BasicTest {

    @Test
    public void math_works() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void mainActivity_exists() throws Exception {
        Class<?> c = Class.forName(
            "com.ai.studymaster.app.MainActivity",
            false,
            getClass().getClassLoader());
        assertNotNull(c);
    }
}
