package co.diji.cloud9.utils;

import static junit.framework.Assert.*;

import org.junit.Test;

public class C9HelperTest {

    @Test
    public void testIsValidName() {
        assertTrue(C9Helper.isValidName("valid"));
        assertTrue(C9Helper.isValidName("valid1"));
        assertTrue(C9Helper.isValidName("v2l1d"));
        assertTrue(C9Helper.isValidName("abc123"));
        assertTrue(C9Helper.isValidName("123"));
        assertFalse(C9Helper.isValidName("NOTVALID"));
        assertFalse(C9Helper.isValidName("notValid"));
        assertFalse(C9Helper.isValidName("@#$#$"));
        assertFalse(C9Helper.isValidName("not$valid"));
        assertFalse(C9Helper.isValidName("897sdfQ"));
    }
}
