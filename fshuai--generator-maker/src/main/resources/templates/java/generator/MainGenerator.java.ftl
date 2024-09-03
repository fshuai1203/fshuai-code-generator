package ${basePackage}.generator;

import ${basePackage}.model.DataModel;
import freemarker.template.TemplateException;

import java.io.File;
import java.io.IOException;
/**
* 核心生成器
*/
public class MainGenerator {

    public static void doGenerate(DataModel model) throws TemplateException, IOException {
        String inputRootPath = "${fileConfig.inputRootPath}";
        String outputRootPath = "${fileConfig.outputRootPath}";

        String inputPath;
        String outputPath;

    <#macro generateFile indent fileInfo>
    ${indent}inputPath = new File(inputRootPath, "${fileInfo.inputPath}").getAbsolutePath();
    ${indent}outputPath = new File(outputRootPath, "${fileInfo.outputPath}").getAbsolutePath();
        <#if fileInfo.generateType == "static">
    ${indent}StaticFileGenerator.copyFilesByHutool(inputPath, outputPath);
        </#if>
        <#if fileInfo.generateType == "dynamic">
    ${indent}DynamicFileGenerator.doGenerate(inputPath, outputPath, model);
        </#if>
    </#macro>


    <#list   modelConfig.models as modelInfo>
    <#if modelInfo.groupKey??>
        <#list modelInfo.models as subModelInfo>
        ${subModelInfo.type} ${subModelInfo.fieldName} = model.${modelInfo.groupKey}.${subModelInfo.fieldName};
        </#list>
    <#else >
        ${modelInfo.type} ${modelInfo.fieldName} = model.${modelInfo.fieldName};
    </#if>
    </#list>

    <#list fileConfig.files as fileInfo>

        <#if fileInfo.groupKey??>
        // groupKey = ${fileInfo.groupKey}
        </#if>
        <#if (fileInfo.condition)??>
        if (${fileInfo.condition}) {
            <#list fileInfo.files as fileInfo>
            <@generateFile "        ",fileInfo/>

            </#list>
        }
        <#else >
       <@generateFile "    ",fileInfo/>
        </#if>
    </#list>

    }

}


