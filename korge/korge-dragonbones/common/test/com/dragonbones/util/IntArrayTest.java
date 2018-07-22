package com.dragonbones.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class IntArrayTest {
    @Test
    public void name() throws Exception {
        ArrayList<Integer> out = new ArrayList<>();
        for (int v : new IntArray(new int[]{1, 2, 3})) {
            out.add(v);
        }
        Assert.assertEquals(3, out.size());
        Assert.assertEquals(1, (int)out.get(0));
        Assert.assertEquals(2, (int)out.get(1));
        Assert.assertEquals(3, (int)out.get(2));
    }
}