package com.fshuai.cli.command;

import cn.hutool.core.bean.BeanUtil;
import com.fshuai.generator.MainGenerator;
import com.fshuai.model.MainTemplateConfig;
import lombok.Data;
import picocli.CommandLine;

import java.util.concurrent.Callable;

/**
 * 生成文件指令
 */
@CommandLine.Command(name = "generate", description = "生成代码", mixinStandardHelpOptions = true)
@Data
public class GenerateCommand implements Callable {

    @CommandLine.Option(names = {"-l", "--loop"}, description = "是否循环", interactive = true, arity = "0..1", echo = true)
    private boolean loop;

    @CommandLine.Option(names = {"-a", "--author"}, description = "作者", interactive = true, arity = "0..1", echo = true)
    private String author = "fshuai";


    @CommandLine.Option(names = {"-o", "--outputText"}, description = "输出文本", interactive = true, arity = "0..1", echo = true)
    private String outputText = "sum = ";

    // 调用子命令后的逻辑处理部分
    @Override
    public Integer call() throws Exception {
        MainTemplateConfig mainTemplateConfig = new MainTemplateConfig();
        BeanUtil.copyProperties(this, mainTemplateConfig);
        System.out.println("配置信息：" + mainTemplateConfig);
        // 执行生成文件
        MainGenerator.doGenerator(mainTemplateConfig);
        return 0;
    }
}
