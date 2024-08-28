package com.fshuai.maker.generator;

import cn.hutool.core.io.FileUtil;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

public class ScriptGenerator {

    /**
     * // 脚本生成
     *
     * @param outPutPath 输出脚本的位置
     * @param jarPath    jar包所在的位置
     */
    public static void doGenerate(String outPutPath, String jarPath) {
        // 直接写入脚本文件

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("#!/bin/bash").append("\n");
        stringBuilder.append(String.format("java -jar %s \"$@\"", jarPath)).append("\n");

        FileUtil.writeBytes(stringBuilder.toString().getBytes(StandardCharsets.UTF_8), outPutPath);

        // 添加可执行权限
        try {
            Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rwxrwxrwx");
            Files.setPosixFilePermissions(Paths.get(outPutPath), permissions);
        } catch (Exception e) {
            System.out.println("添加可执行权限失败");
        }
    }

    public static void main(String[] args) {
        String outputPath = System.getProperty("user.dir") + File.separator + "generator";
        doGenerate(outputPath, "");
    }

}