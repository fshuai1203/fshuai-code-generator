package ${basePackage}.cli.command;

import cn.hutool.core.bean.BeanUtil;
import ${basePackage}.generator.MainGenerator;
import ${basePackage}.model.DataModel;
import lombok.Data;
import picocli.CommandLine;

import java.util.concurrent.Callable;
<#--生成式选项-->
<#macro generateOption indent modelInfo>
${indent}@CommandLine.Option(names = {<#if modelInfo.abbr??>"-${modelInfo.abbr}",</#if>"--${modelInfo.fieldName}" },<#if modelInfo.description??>description = "${modelInfo.description}",</#if> interactive = true, arity = "0..1", echo = true)
${indent}private ${modelInfo.type} ${modelInfo.fieldName};
</#macro>

<#-- 生成式命令调用-->
<#macro generateCommand indent modelInfo>
${indent}System.out.println("请输入${modelInfo.groupName}配置");
${indent}CommandLine commandLine = new CommandLine(${modelInfo.type}Command.class);
${indent}commandLine.execute(${modelInfo.allArgStr});
</#macro>
/**
* 生成文件指令
*/
@CommandLine.Command(name = "generate", description = "生成代码", mixinStandardHelpOptions = true)
@Data
public class GenerateCommand implements Callable {

<#list modelConfig.models as modelInfo>
<#if modelInfo.groupKey??>

    static DataModel.${modelInfo.type} ${modelInfo.groupKey} = new DataModel.${modelInfo.type}();

    @CommandLine.Command(name = "${modelInfo.groupKey}")
    @Data
    public static class ${modelInfo.type}Command implements Runnable {

    <#list modelInfo.models as subModelInfo>
        <@generateOption indent="        " modelInfo=subModelInfo/>
    </#list>

        @Override
        public void run() {
        <#list modelInfo.models as subModelInfo>
            ${modelInfo.groupKey}.${subModelInfo.fieldName} = ${subModelInfo.fieldName};
        </#list>
        }
    }

<#else>
<@generateOption indent="    " modelInfo=modelInfo/>
</#if>
</#list>

    @Override
    public Integer call() throws Exception {
    <#list modelConfig.models as modelInfo>
        <#if modelInfo.groupKey??>
        <#if modelInfo.condition??>
         if (${modelInfo.condition}) {
             <@generateCommand indent="            " modelInfo=modelInfo/>
         }
        <#else>
            <#list modelInfo.models as subModelInfo>
                <@generateCommand indent="    " modelInfo=modelInfo/>
            </#list>
        </#if>
        </#if>
    </#list>
        DataModel dataModel = new DataModel();
        BeanUtil.copyProperties(this, dataModel);
        <#list modelConfig.models as modelInfo>
        <#if modelInfo.groupKey??>
        dataModel.${modelInfo.groupKey} = ${modelInfo.groupKey};
        MainGenerator.doGenerate(dataModel);
        </#if>
        </#list>
        return 0;
    }

}
