package ${basePackage}.cli.command;

import cn.hutool.core.bean.BeanUtil;
import ${basePackage}.generator.file.MainGenerator;
import ${basePackage}.model.DataModel;
import lombok.Data;
import picocli.CommandLine;

import java.util.concurrent.Callable;

/**
* 生成文件指令
*/
@CommandLine.Command(name = "generate", description = "生成代码", mixinStandardHelpOptions = true)
@Data
public class GenerateCommand implements Callable {

<#list modelConfig.models as modelInfo>
    @CommandLine.Option(names = {<#if modelInfo.abbr??>"-${modelInfo.abbr}",</#if>"--${modelInfo.fieldName}" },<#if modelInfo.description??>description = "${modelInfo.description}",</#if> interactive = true, arity = "0..1", echo = true)
    private ${modelInfo.type} ${modelInfo.fieldName};
</#list>

@Override
public Integer call() throws Exception {
    DataModel dataModel = new DataModel();
    BeanUtil.copyProperties(this, dataModel);
    MainGenerator.doGenerate(dataModel);
    return 0;
}
}
