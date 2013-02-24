package co.fs.evo.test.common;

import static junit.framework.Assert.*;

import org.junit.Test;

import co.fs.evo.common.StringUtils;
import static co.fs.evo.Constants.VALID_TYPES;
import static co.fs.evo.Constants.INVALID_INDEX_NAMES;

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
        
        /* check for these are reserved names... see INVALID_INDEX_NAMES */
        for (int i=0; i < INVALID_INDEX_NAMES.length; i++) {
        	assertFalse(StringUtils.isValidIndexName(INVALID_INDEX_NAMES[i]));
        }
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
    public void testIsValidResourceType() {
    	for (int i=0; i < VALID_TYPES.length; i++) {
    		assertTrue(StringUtils.isValidResourceType(VALID_TYPES[i]));
    	}
    	
    	assertFalse(StringUtils.isValidResourceType(""));
    	assertFalse(StringUtils.isValidResourceType("asdf"));
    }
    
    @Test
    public void testBase64() {
    	String text = "some random string of text";
    	assertEquals(text, new String(
    			StringUtils.decodeBase64(StringUtils.encodeBase64(text))));
    }
}
