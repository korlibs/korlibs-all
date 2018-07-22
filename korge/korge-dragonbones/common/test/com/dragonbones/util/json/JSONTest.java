package com.dragonbones.util.json;

import com.dragonbones.util.StreamUtil;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class JSONTest {
    @Test
    public void name() throws Exception {
        JSON.INSTANCE.parse("[true,false]");
        JSON.INSTANCE.parse("{}");
        JSON.INSTANCE.parse("{\"a\":1}");
        JSON.INSTANCE.parse("1");
        JSON.INSTANCE.parse("\"abc\"");
        JSON.INSTANCE.parse("true");
        JSON.INSTANCE.parse("false");
        JSON.INSTANCE.parse("null");
        JSON.INSTANCE.parse("[1,2,3,4]");
        JSON.INSTANCE.parse("true");
        JSON.INSTANCE.parse("false");
        JSON.INSTANCE.parse("[]");
        JSON.INSTANCE.parse("[[]]");
        JSON.INSTANCE.parse("[[[]],[]]");
        JSON.INSTANCE.parse("[ [ true, false ], 1, 2, { \"a\" : true } ]");
        JSON.INSTANCE.parse("[ [ true, false ] , 1 , 2 , { \"a\" : true } ]");

        JSON.INSTANCE.parse(StreamUtil.INSTANCE.getResourceString("NewDragon/NewDragon.json", StandardCharsets.UTF_8));
        //System.out.println();
    }
}