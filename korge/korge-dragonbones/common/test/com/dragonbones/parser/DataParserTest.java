package com.dragonbones.parser;

import com.dragonbones.model.DragonBonesData;
import com.dragonbones.util.StreamUtil;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class DataParserTest {
    @Test
    public void name() throws Exception {
        DragonBonesData data = DataParser.Companion.parseDragonBonesDataJson(
                StreamUtil.INSTANCE.getResourceString("Dragon/Dragon_ske.json", StandardCharsets.UTF_8)
        );
        System.out.println(data);
    }
}