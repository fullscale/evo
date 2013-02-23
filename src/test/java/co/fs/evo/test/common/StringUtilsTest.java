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
        
        /* these are reserved names... see INVALID_INDEX_NAMES */
        assertFalse(StringUtils.isValidIndexName("css"));
        assertFalse(StringUtils.isValidIndexName("js"));
        assertFalse(StringUtils.isValidIndexName("img"));
        assertFalse(StringUtils.isValidIndexName("partials"));
        assertFalse(StringUtils.isValidIndexName("lib"));
    }
    
    @Test
    public void testIsSystemIndex() {
    	assertTrue(StringUtils.isSystemIndex("sys"));
    	assertFalse(StringUtils.isSystemIndex("junk"));
    }
    
    @Test
    public void testIsAppIndex() {
    	assertTrue(StringUtils.isAppIndex("app"));
    	assertFalse(StringUtils.isAppIndex("junk"));
    }
    
    @Test
    public void testBase64() {
    	String text = "some random string of text";
    	assertEquals(text, new String(
    			StringUtils.decodeBase64(StringUtils.encodeBase64(text))));
    }
}
