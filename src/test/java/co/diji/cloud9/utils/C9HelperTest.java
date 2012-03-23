package co.diji.cloud9.utils;

import junit.framework.Assert;

import org.junit.Test;

public class C9HelperTest {

    @Test
    public void testIsValidName() {
        Assert.assertTrue(C9Helper.isValidName("valid"));
        Assert.assertTrue(C9Helper.isValidName("valid1"));
        Assert.assertTrue(C9Helper.isValidName("v2l1d"));
        Assert.assertTrue(C9Helper.isValidName("abc123"));
        Assert.assertTrue(C9Helper.isValidName("123"));
        Assert.assertEquals(false, C9Helper.isValidName("NOTVALID"));
        Assert.assertEquals(false, C9Helper.isValidName("notValid"));
        Assert.assertEquals(false, C9Helper.isValidName("@#$#$"));
        Assert.assertEquals(false, C9Helper.isValidName("not$valid"));
        Assert.assertEquals(false, C9Helper.isValidName("897sdfQ"));
    }
}
