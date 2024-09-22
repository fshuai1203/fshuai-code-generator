package com.fshuai.maker.template.model;

import lombok.Data;

@Data
public class TemplateMakerOutputConfig {

    // 控制是否从未分组文件中移除组内同名文件
    private boolean removeGroupFilesFromRoot = true;

}
