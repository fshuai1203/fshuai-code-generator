package com.fshuai.generator;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class StaticGenerator {

    /**
     * 拷贝文件
     *
     * @param inputPath
     * @param outputPath
     */
    public static void copyFilesByHutool(String inputPath, String outputPath) {
        FileUtil.copy(inputPath, outputPath, false);
    }

    /**
     * 递归遍历去复制文件夹
     *
     * @param inputFile
     * @param outputFile
     */
    public static void copyFileByRecursive(File inputFile, File outputFile) throws IOException {
        if (inputFile.isDirectory()) {
            // 如果是文件夹的话，循环复制文件夹下的文件夹
            File destOutPutFile = new File(outputFile, inputFile.getName());
            if (!destOutPutFile.exists()) {
                destOutPutFile.mkdir();
            }
            File[] files = inputFile.listFiles();
            if (ArrayUtil.isEmpty(files)) {
                return;
            }
            for (File file : files) {
                copyFileByRecursive(file, destOutPutFile);
            }
        } else {
            // 是文件的话直接复制到对应目录中
            Path destPath = outputFile.toPath().resolve(inputFile.getName());
            Files.copy(inputFile.toPath(), destPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static void copyFileByRecursive(String inputPath, String outputPath) {
        File inputFile = new File(inputPath);
        File outputFile = new File(outputPath);
        try {
            copyFileByRecursive(inputFile, outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // 获取项目路径以及上层文件
        String projectPath = System.getProperty("user.dir");
        File parentFile = new File(projectPath).getParentFile();
        // 获取ACM示例代码模板目录
        String inputPath = new File(parentFile, "fshuai-gengerator-demo-projects/acm-template").getAbsolutePath();
        // 保存项目到根目录
        String outPutPath = projectPath;
        copyFilesByHutool(inputPath, outPutPath);
    }

}
