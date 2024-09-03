# ${name}

> ${description}
>
>
>
> 作者${author}
>
>
>
> 基于 程序员鱼皮的 [鱼籽代码生成器项目](https://github.com/liyupi/yuzi-generator) 制作，感谢您的使用！

可以通过命令行交互输入的方式生成想要的代码



## 使用说明

执行项目根目录下的脚本文件

`generator <命令> <选项参数>`



## 示例命令

`generator generate <#list modelConfig.models as model> <#if (modelInfo.abbr) ??>- ${model.abbr}</#if></#list>`



## 参数说明

<#macro paramInfo indet modelInfo>
**${modelInfo.fieldName}**

${indet}类型:${modelInfo.type}

${indet}描述:${modelInfo.description}

${indet}默认值:${modelInfo.defaultValue?c}

<#if (modelInfo.abbr) ??>${indet}缩写:-${modelInfo.abbr}</#if>

</#macro>

<#list modelConfig.models as modelInfo>
<#if modelInfo.groupKey??>
**${modelInfo.groupName}-${modelInfo.groupKey}**
--------------
<#list modelInfo.models as subModelInfo>
<@paramInfo indet="- " modelInfo=subModelInfo/>
</#list>
-------
<#else >
<@paramInfo indet="- " modelInfo=modelInfo/>
</#if>

</#list>