package co.fs.evo.test.common;

import static junit.framework.Assert.*;

import org.junit.Test;

import co.fs.evo.common.StringUtils;

public class StringUtilsTest {

    @Test
    public void testIsValidIdentifier() {
        assertTrue(StringUtils.isValidIndexName("valid"));
        assertTrue(StringUtils.isValidIndexName("valid1"));
        assertTrue(StringUtils.isValidIndexName("v2l1d"));
        assertTrue(StringUtils.isValidIndexName("abc123"));
        assertTrue(StringUtils.isValidIndexName("123"));
        assertTrue(StringUtils.isValidIndexName("valid.app"));
        assertFalse(StringUtils.isValidIndexName("NOTVALID"));
        assertFalse(StringUtils.isValidIndexName("notValid"));
        assertFalse(StringUtils.isValidIndexName("@#$#$"));
        assertFalse(StringUtils.isValidIndexName("not$valid"));
        assertFalse(StringUtils.isValidIndexName("897sdfQ"));
        assertFalse(StringUtils.isValidIndexName("not#$#valid.app"));
        assertFalse(StringUtils.isValidIndexName("name.not.valid"));
    }
}
