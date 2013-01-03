package co.fs.evo.test.utils;

import static junit.framework.Assert.*;

import org.junit.Test;

import co.fs.evo.utils.EvoHelper;

public class EvoHelperTest {

    @Test
    public void testIsValidName() {
        assertTrue(EvoHelper.isValidName("valid"));
        assertTrue(EvoHelper.isValidName("valid1"));
        assertTrue(EvoHelper.isValidName("v2l1d"));
        assertTrue(EvoHelper.isValidName("abc123"));
        assertTrue(EvoHelper.isValidName("123"));
        assertTrue(EvoHelper.isValidName("valid.app"));
        assertFalse(EvoHelper.isValidName("NOTVALID"));
        assertFalse(EvoHelper.isValidName("notValid"));
        assertFalse(EvoHelper.isValidName("@#$#$"));
        assertFalse(EvoHelper.isValidName("not$valid"));
        assertFalse(EvoHelper.isValidName("897sdfQ"));
        assertFalse(EvoHelper.isValidName("not#$#valid.app"));
        assertFalse(EvoHelper.isValidName("name.not.valid"));
    }
}
