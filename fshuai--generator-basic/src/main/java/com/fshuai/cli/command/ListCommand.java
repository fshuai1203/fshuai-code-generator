package com.fshuai.cli.command;

import cn.hutool.core.io.FileUtil;
import picocli.CommandLine;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * 遍历模板文件列表
 */
@CommandLine.Command(name = "list", description = "获取模板文件", mixinStandardHelpOptions = true)
public class ListCommand implements Runnable {

    @Override
    public void run() {
        //先复制静态文件
        // 获取项目路径以及上层文件
        String projectPath = System.getProperty("user.dir");
        System.out.println("项目路径：" + projectPath);
        File parentFile = new File(projectPath).getParentFile();
        // 获取ACM示例代码模板目录
        String inputPath = new File(parentFile, "fshuai-gengerator-demo-projects/acm-template").getAbsolutePath();

        List<File> files = FileUtil.loopFiles(inputPath);

        for (File file : files) {
            System.out.println(file);
        }
    }
}
