package com.fshuai.maker.meta.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
public enum FileTypeEnum {

    FILE("文件", "file"),
    DIR("目录", "dir");

    private final String text;

    private final String value;


}
