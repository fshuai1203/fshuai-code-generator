package com.fshuai.generator;

import com.fshuai.model.MainTemplateConfig;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 动态文件生成
 */
public class DynamicGenerator {

    public static void main(String[] args) throws IOException, TemplateException {

        String projectPath = System.getProperty("user.dir");
        String inputPath = projectPath + File.separator + "src/main/resources/templates/MainTemplate.java.ftl";
        String outputPath = projectPath + File.separator + "MainTemplate.java";

        // 创建数据模型
        MainTemplateConfig mainTemplateConfig = new MainTemplateConfig();
        mainTemplateConfig.setAuthor("fshuai");
        mainTemplateConfig.setLoop(false);
        mainTemplateConfig.setOutputText("求和结果：");

        doGenerator(inputPath, outputPath, mainTemplateConfig);
    }

    /**
     * 生成文件
     */
    public static void doGenerator(String inputPath, String outputPath, Object model) throws IOException, TemplateException {

        // new 出 Configuration 对象，参数为 FreeMarker 版本号
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_32);

        // 制定模板所在的路径
        configuration.setDirectoryForTemplateLoading(new File(inputPath).getParentFile());

        // 设定模版文件的字符集
        configuration.setDefaultEncoding("utf-8");

        // 创建模版对象，加载指定模版
        String templateName = new File(inputPath).getName();
        Template template = configuration.getTemplate(templateName);


        FileWriter out = new FileWriter(outputPath);
        template.process(model, out);

        out.close();
    }
}

