package com.fshuai.maker.meta;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.json.JSONUtil;

public class MetaManager {

    private static volatile Meta meta;

    private MetaManager() {
        // 私有构造函数防止外部实例化
    }

    public static Meta getMetaObject() {
        if (meta == null) {
            synchronized (MetaManager.class) {
                if (meta == null) {
                    meta = initMeta();
                }
            }
        }
        return meta;
    }

    private static Meta initMeta() {
        // 读取元数据的json
        String metaJson = ResourceUtil.readUtf8Str("meta.json");
        // 把Json数据转换位Bean
        Meta meta = JSONUtil.toBean(metaJson, Meta.class);
        // 处理和校验默认值
        MetaValidator.doValidAndFill(meta);
        Meta.FileConfigDTO fileConfig = meta.getFileConfig();
        return meta;
    }

}
