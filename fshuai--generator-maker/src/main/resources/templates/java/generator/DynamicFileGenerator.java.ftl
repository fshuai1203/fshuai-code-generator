package ${basePackage}.generator.file;

import cn.hutool.core.io.FileUtil;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
* 动态文件生成
*/
public class DynamicFileGenerator {


    /**
    * 生成动态文件
    */
    public static void doGenerate(String inputPath, String outputPath, Object model) throws IOException, TemplateException {

        // new 出 Configuration 对象，参数为 FreeMarker 版本号
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_32);

        // 制定模板所在的路径
        configuration.setDirectoryForTemplateLoading(new File(inputPath).getParentFile());

        // 设定模版文件的字符集
        configuration.setDefaultEncoding("utf-8");

        // 创建模版对象，加载指定模版
        String templateName = new File(inputPath).getName();
        Template template = configuration.getTemplate(templateName);

        // 文件不存在则创建文件和父目录
        if (!FileUtil.exist(outputPath)) {
            FileUtil.touch(outputPath);
        }


        FileWriter out = new FileWriter(outputPath);
        template.process(model, out);

        out.close();
    }
}

