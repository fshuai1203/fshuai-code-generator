package com.fshuai.maker.template;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.fshuai.maker.meta.Meta;
import com.fshuai.maker.meta.enums.FileGenerateTypeEnum;
import com.fshuai.maker.meta.enums.FileTypeEnum;
import com.fshuai.maker.template.enums.FileFilterRangeEnum;
import com.fshuai.maker.template.enums.FileFilterRuleEnum;
import com.fshuai.maker.template.model.FileFilterConfig;
import com.fshuai.maker.template.model.TemplateMakerFileConfig;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TemplateMaker {

    public static void main(String[] args) {
        Meta meta = new Meta();
        // 1. 项目的基本信息
        //  "name": "acm-template-pro-generator",
        //  "description": "ACM 示例模板生成器",
        meta.setName("acm-template-pro-generator");
        meta.setDescription("ACM 示例模板生成器");

        String projectPath = System.getProperty("user.dir");
        String originProjectPath = new File(projectPath).getParent() + File.separator + "fshuai-gengerator-demo-projects/springboot-init-master";


        // 第一次替换
        // 输入模型参数信息
//        Meta.ModelConfig.ModelInfo modelInfo = new Meta.ModelConfig.ModelInfo();
//        modelInfo.setFieldName("outputText");
//        modelInfo.setType("String");
//        modelInfo.setDefaultValue("sum=");

//        String searchStr = "Sum: ";

        // 第二次替换
        Meta.ModelConfig.ModelInfo modelInfo = new Meta.ModelConfig.ModelInfo();
        modelInfo.setFieldName("className");
        modelInfo.setType("String");

        String searchStr = "BaseResponse";

        String fileInputPath1 = "src/main/java/com/yupi/springbootinit/common";
        String fileInputPath2 = "src/main/java/com/yupi/springbootinit/controller";

        TemplateMakerFileConfig templateMakerFileConfig = new TemplateMakerFileConfig();
        TemplateMakerFileConfig.FileInfoConfig fileInfoConfig1 = new TemplateMakerFileConfig.FileInfoConfig();
        fileInfoConfig1.setPath(fileInputPath1);
        List<FileFilterConfig> filterConfigList = new ArrayList<>();
        FileFilterConfig fileFilterConfig = FileFilterConfig.builder()
                .range(FileFilterRangeEnum.FILE_NAME.getValue())
                .rule(FileFilterRuleEnum.CONTAINS.getValue())
                .value("Base")
                .build();
        filterConfigList.add(fileFilterConfig);
        fileInfoConfig1.setFilterConfigList(filterConfigList);

        TemplateMakerFileConfig.FileInfoConfig fileInfoConfig2 = new TemplateMakerFileConfig.FileInfoConfig();
        fileInfoConfig2.setPath(fileInputPath2);
        templateMakerFileConfig.setFiles(Arrays.asList(fileInfoConfig1,fileInfoConfig2));

        Long id = makeTemplate(meta, originProjectPath, templateMakerFileConfig, modelInfo, searchStr, null);
        System.out.println(id);
    }

    /**
     * 生成模版
     *
     * @param newMeta                 元信息
     * @param originProjectPath       源项目路径
     * @param templateMakerFileConfig 文件列表信息(包括文件过滤字段)
     * @param modelInfo               模型信息
     * @param searchStr               要替换的字符串
     * @param id                      生成的临时文件夹的名称(ID)
     * @return
     */
    private static Long makeTemplate(Meta newMeta,
                                     String originProjectPath,
                                     TemplateMakerFileConfig templateMakerFileConfig,
                                     Meta.ModelConfig.ModelInfo modelInfo,
                                     String searchStr,
                                     Long id) {
        if (id == null) {
            // 工作空间隔离
            // 使用雪花算法分配唯一ID，作为工作空间名称
            id = IdUtil.getSnowflakeNextId();
        }

        String projectPath = System.getProperty("user.dir");
        String tempDirPath = projectPath + File.separator + ".temp";
        String templatePath = tempDirPath + File.separator + id;

        // 1-非首次制作，不需要复制文件
        if (!FileUtil.exist(templatePath)) {
            FileUtil.mkdir(templatePath);
            FileUtil.copy(originProjectPath, templatePath, true);
        }

        // 要挖挖坑项目的根目录
        String sourceRootPath = templatePath + File.separator + FileUtil.getLastPathEle(Paths.get(originProjectPath)).toString();

        // 获取要修改的文件
        List<TemplateMakerFileConfig.FileInfoConfig> fileConfigInfoList = templateMakerFileConfig.getFiles();

        List<Meta.FileConfigDTO.FileInfo> newFileInfoList = new ArrayList<>();

        for (TemplateMakerFileConfig.FileInfoConfig fileInfoConfig : fileConfigInfoList) {

            // 输入信息
            String inputFilePath = fileInfoConfig.getPath();
            String inputFileAbsolutePath = sourceRootPath + File.separator + inputFilePath;

            // 文件过滤
            // 过滤器要求文件信息配置中是绝对路径
            if (!inputFilePath.startsWith(sourceRootPath)) {
                // 相对路径转变为绝对路径
                inputFilePath = sourceRootPath + File.separator + inputFilePath;
            }

            // 过滤后的文件列表不会存在目录
            List<File> filterFiles = FileFilter.doFilter(inputFilePath, fileInfoConfig.getFilterConfigList());

            filterFiles.forEach(file -> {
                // 输入为单个文件
                Meta.FileConfigDTO.FileInfo fileInfo = makeFileTemplate(file, modelInfo, searchStr, sourceRootPath);
                newFileInfoList.add(fileInfo);
            });

        }

        // 生成配置文件
        String metaOutputPath = sourceRootPath + File.separator + "meta.json";

        if (!FileUtil.exist(metaOutputPath)) {

            // 构造文件配置参数
            Meta.FileConfigDTO fileConfig = new Meta.FileConfigDTO();
            newMeta.setFileConfig(fileConfig);
            fileConfig.setSourceRootPath(sourceRootPath);
            List<Meta.FileConfigDTO.FileInfo> fileInfoList = new ArrayList<>();
            fileConfig.setFiles(fileInfoList);
            fileInfoList.addAll(newFileInfoList);

            Meta.ModelConfig modelConfig = new Meta.ModelConfig();
            newMeta.setModelConfig(modelConfig);
            List<Meta.ModelConfig.ModelInfo> modelInfoList = new ArrayList<>();
            modelConfig.setModels(modelInfoList);
            modelInfoList.add(modelInfo);
        } else {
            // 非首次制作
            Meta oldMeta = JSONUtil.toBean(FileUtil.readUtf8String(metaOutputPath), Meta.class);
            // 复制newMeta新增信息到oldMeta中
            BeanUtil.copyProperties(newMeta, oldMeta, CopyOptions.create().ignoreNullValue());
            newMeta = oldMeta;
            // 追加信息
            List<Meta.FileConfigDTO.FileInfo> fileInfoList = newMeta.getFileConfig().getFiles();
            fileInfoList.addAll(fileInfoList);
            List<Meta.ModelConfig.ModelInfo> modelInfoList = newMeta.getModelConfig().getModels();
            modelInfoList.add(modelInfo);

            // 去重
            newMeta.getFileConfig().setFiles(distinctFiles(fileInfoList));
            newMeta.getModelConfig().setModels(distinctModels(modelInfoList));

        }

        // 写入元信息文件
        FileUtil.writeUtf8String(JSONUtil.toJsonPrettyStr(newMeta), metaOutputPath);
        return id;
    }

    private static Meta.FileConfigDTO.FileInfo makeFileTemplate(File inputFile,
                                                                Meta.ModelConfig.ModelInfo modelInfo,
                                                                String searchStr,
                                                                String sourceRootPath) {
        // 要挖坑的文件，把绝对路径替换成相对路径
        String fileInputPath = inputFile.getAbsolutePath().replace(sourceRootPath + "/", "");

        String fileOutputPath = fileInputPath + ".ftl";


        // 2-非首次制作，在已有模版上进行挖坑
        String fileInputAbsolutePath = inputFile.getAbsolutePath();
        String fileOutputAbsolutePath = inputFile.getAbsolutePath() + ".ftl";

        String fileContent = null;
        if (!FileUtil.exist(fileOutputAbsolutePath)) {
            // 首次制作
            // 使用字符串替换，生成模版文件

            fileContent = FileUtil.readUtf8String(fileInputAbsolutePath);
        } else {
            // 非首次制作
            fileContent = FileUtil.readUtf8String(fileOutputAbsolutePath);
        }

        String replacement = String.format("${%s}", modelInfo.getFieldName());
        String newFileContent = StrUtil.replace(fileContent, searchStr, replacement);


        // 生成元信息配置文件
        // 3-非首次制作，在原有元信息配置上进行修改
        // 文件配置信息，无论修改还是新增，文件信息一定是存在的
        Meta.FileConfigDTO.FileInfo fileInfo = new Meta.FileConfigDTO.FileInfo();
        fileInfo.setInputPath(fileInputPath);
        fileInfo.setOutputPath(fileOutputPath);
        fileInfo.setType(FileTypeEnum.FILE.getValue());

        // 对于没有修改的文件则直接静态生成
        if (newFileContent.equals(fileContent)) {
            // 输出路径等于输入路径
            fileInfo.setOutputPath(fileInputPath);
            fileInfo.setGenerateType(FileGenerateTypeEnum.STATIC.getValue());
        } else {
            // 生成模版文件
            fileInfo.setGenerateType(FileGenerateTypeEnum.DYNAMIC.getValue());
            // 输出模版文件
            FileUtil.writeUtf8String(newFileContent, fileOutputAbsolutePath);
        }
        return fileInfo;
    }

    // 文件去重
    private static List<Meta.FileConfigDTO.FileInfo> distinctFiles(List<Meta.FileConfigDTO.FileInfo> fileInfoList) {
        List<Meta.FileConfigDTO.FileInfo> newFileInfoList = new ArrayList<>(
                fileInfoList.stream()
                        .collect(Collectors.toMap(Meta.FileConfigDTO.FileInfo::getInputPath,
                                output -> output,
                                (exist, replacement) -> replacement)).values()
        );
        return newFileInfoList;
    }

    // 模型去重
    private static List<Meta.ModelConfig.ModelInfo> distinctModels(List<Meta.ModelConfig.ModelInfo> modelInfoList) {
        List<Meta.ModelConfig.ModelInfo> newFileInfoList = new ArrayList<>(
                modelInfoList.stream()
                        .collect(Collectors.toMap(Meta.ModelConfig.ModelInfo::getFieldName,
                                output -> output,
                                (exist, replacement) -> replacement)).values()
        );
        return newFileInfoList;
    }


}
