package com.fshuai.generator;

import com.fshuai.model.MainTemplateConfig;
import freemarker.template.TemplateException;

import java.io.File;
import java.io.IOException;

/**
 * 核心生成器
 */
public class MainGenerator {

    public static void doGenerator(Object model) throws TemplateException, IOException {
        //先复制静态文件
        // 获取项目路径以及上层文件
        String projectPath = System.getProperty("user.dir");
        File parentFile = new File(projectPath).getParentFile();
        // 获取ACM示例代码模板目录
        String inputPath = new File(parentFile, "fshuai-gengerator-demo-projects/acm-template").getAbsolutePath();

        // 保存项目到根目录
        String outPutPath = projectPath;

        StaticGenerator.copyFileByRecursive(inputPath, outPutPath);
        // 在生成动态文件进行覆盖

        String inputDynamicPath = projectPath + File.separator + "src/main/resources/templates/MainTemplate.java.ftl";
        String outputDynamicPath = projectPath + File.separator + "acm-template/src/com/fshuai/acm/MainTemplate.java";


        DynamicGenerator.doGenerator(inputDynamicPath, outputDynamicPath, model);
    }

    public static void main(String[] args) throws TemplateException, IOException {
        // 创建数据模型
        MainTemplateConfig mainTemplateConfig = new MainTemplateConfig();
        mainTemplateConfig.setAuthor("fshuai");
        mainTemplateConfig.setLoop(false);
        mainTemplateConfig.setOutputText("求和结果：");
        // 调用生成器开始生成
        doGenerator(mainTemplateConfig);
    }

}
