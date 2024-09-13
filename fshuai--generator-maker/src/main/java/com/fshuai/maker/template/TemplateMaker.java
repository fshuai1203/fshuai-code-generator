package com.fshuai.maker.template;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
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
import java.util.*;
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
        modelInfo.setFieldName("package");
        modelInfo.setType("String");

        String searchStr = "Common";

        String fileInputPath1 = "src/main/java/com/yupi/springbootinit/common";
        String fileInputPath2 = "src/main/java/com/yupi/springbootinit/constant";

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
        templateMakerFileConfig.setFiles(Arrays.asList(fileInfoConfig1, fileInfoConfig2));

        TemplateMakerFileConfig.FileGroupConfig fileGroupConfig = new TemplateMakerFileConfig.FileGroupConfig();
        fileGroupConfig.setCondition("outputText");
        fileGroupConfig.setGroupKey("test");
        fileGroupConfig.setGroupName("测试分组");
        templateMakerFileConfig.setFileGroupConfig(fileGroupConfig);

        Long id = makeTemplate(meta, originProjectPath, templateMakerFileConfig, modelInfo, searchStr, 1834525792556003328L);
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
        // 如果未提供ID，生成一个唯一ID
        if (id == null) {
            // 工作空间隔离
            // 使用雪花算法分配唯一ID，作为工作空间名称
            id = IdUtil.getSnowflakeNextId();
        }

        // 获取当前工作目录路径
        String projectPath = System.getProperty("user.dir");
        // 设置临时文件夹路径
        String tempDirPath = projectPath + File.separator + ".temp";
        // 设置模版路径
        String templatePath = tempDirPath + File.separator + id;

        // 如果模版路径不存在，则进行首次制作
        if (!FileUtil.exist(templatePath)) {
            // 创建新的目录
            FileUtil.mkdir(templatePath);
            // 复制源项目到模版路径
            FileUtil.copy(originProjectPath, templatePath, true);
        }

        // 获取源项目的根目录
        String sourceRootPath = templatePath + File.separator + FileUtil.getLastPathEle(Paths.get(originProjectPath)).toString();

        // 获取要修改的文件列表
        List<TemplateMakerFileConfig.FileInfoConfig> fileConfigInfoList = templateMakerFileConfig.getFiles();

        // 初始化新的文件信息列表
        List<Meta.FileConfigDTO.FileInfo> newFileInfoList = new ArrayList<>();

        // 遍历文件配置信息列表
        for (TemplateMakerFileConfig.FileInfoConfig fileInfoConfig : fileConfigInfoList) {
            // 获取文件路径
            String inputFilePath = fileInfoConfig.getPath();
            // 拼接绝对路径
            String inputFileAbsolutePath = sourceRootPath + File.separator + inputFilePath;

            // 文件过滤
            // 如果文件路径不是绝对路径，转换为绝对路径
            if (!inputFilePath.startsWith(sourceRootPath)) {
                inputFilePath = sourceRootPath + File.separator + inputFilePath;
            }

            // 过滤文件
            List<File> filterFiles = FileFilter.doFilter(inputFilePath, fileInfoConfig.getFilterConfigList());

            // 处理过滤后的文件
            for (File file : filterFiles) {
                // 生成文件模版
                Meta.FileConfigDTO.FileInfo fileInfo = makeFileTemplate(file, modelInfo, searchStr, sourceRootPath);
                // 添加到文件信息列表
                newFileInfoList.add(fileInfo);
            }
        }

        // 文件分组
        TemplateMakerFileConfig.FileGroupConfig fileGroupConfig = templateMakerFileConfig.getFileGroupConfig();
        // 如果是文件分组
        if (fileGroupConfig != null) {
            String condition = fileGroupConfig.getCondition();
            String groupKey = fileGroupConfig.getGroupKey();
            String groupName = fileGroupConfig.getGroupName();

            // 新增分组配置
            Meta.FileConfigDTO.FileInfo groupFileInfo = new Meta.FileConfigDTO.FileInfo();
            groupFileInfo.setType(FileTypeEnum.GROUP.getValue());
            groupFileInfo.setCondition(condition);
            groupFileInfo.setGroupKey(groupKey);
            groupFileInfo.setGroupName(groupName);
            // 将文件都放到一个分组当中
            groupFileInfo.setFiles(newFileInfoList);

            //更新文件列表
            newFileInfoList = new ArrayList<>();
            newFileInfoList.add(groupFileInfo);

        }

        // 生成配置文件
        String metaOutputPath = sourceRootPath + File.separator + "meta.json";

        // 如果配置文件不存在，则创建新配置
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
            // 如果配置文件存在，则更新旧配置
            Meta oldMeta = JSONUtil.toBean(FileUtil.readUtf8String(metaOutputPath), Meta.class);
            // 复制newMeta新增信息到oldMeta中
            BeanUtil.copyProperties(newMeta, oldMeta, CopyOptions.create().ignoreNullValue());
            newMeta = oldMeta;
            // 更新文件和模型信息
            List<Meta.FileConfigDTO.FileInfo> fileInfoList = newMeta.getFileConfig().getFiles();
            fileInfoList.addAll(newFileInfoList);
            List<Meta.ModelConfig.ModelInfo> modelInfoList = newMeta.getModelConfig().getModels();
            modelInfoList.add(modelInfo);

            // 去重
            newMeta.getFileConfig().setFiles(distinctFiles(fileInfoList));
            newMeta.getModelConfig().setModels(distinctModels(modelInfoList));
        }

        // 写入元信息文件
        FileUtil.writeUtf8String(JSONUtil.toJsonPrettyStr(newMeta), metaOutputPath);
        // 返回生成的模版ID
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
        // 策略:同分组内的文件merge，不同分组的进行保留

        //1.  对于有分组的，按照组进行划分
        Map<String, List<Meta.FileConfigDTO.FileInfo>> groupKeyFileInfoListMap = fileInfoList
                .stream()
                .filter(fileInfo -> StrUtil.isNotBlank(fileInfo.getGroupKey()))
                .collect(Collectors.groupingBy(Meta.FileConfigDTO.FileInfo::getGroupKey));

        //groupKeyFileInfoListMap groupKey:各个文件列表的列表，接下来的任务就是把文件的列表的列表变成列表
        // 2. 同组内的文件进行合并
        Map<String, Meta.FileConfigDTO.FileInfo> groupKeyMergedFileInfoListMap = new HashMap<>();

        for (Map.Entry<String, List<Meta.FileConfigDTO.FileInfo>> entry : groupKeyFileInfoListMap.entrySet()) {
            List<Meta.FileConfigDTO.FileInfo> tempFileInfoList = entry.getValue();
            // 根据临时文件信息列表生成新的文件信息列表，用于记录每个文件的初始处理状态
            ArrayList<Meta.FileConfigDTO.FileInfo> newFileInfoList = new ArrayList<>(tempFileInfoList
                    .stream()
                    // 将嵌套的文件信息列表展平为文件信息流(将多个FileInfo的file列表信息合并成一个file列表信息)
                    .flatMap(fileInfo -> fileInfo.getFiles().stream())
                    // 通过文件的输入路径进行分组，并在有键冲突时保留右侧值(按照file的输入路径进行去重)
                    .collect(Collectors.toMap(Meta.FileConfigDTO.FileInfo::getInputPath, o -> o, (e, r) -> r))
                    .values());

            // 使用新的group进行配置
            Meta.FileConfigDTO.FileInfo newFileInfo = CollUtil.getLast(tempFileInfoList);
            newFileInfo.setFiles(newFileInfoList);
            String groupKey = entry.getKey();
            groupKeyMergedFileInfoListMap.put(groupKey, newFileInfo);

        }

        // 3.将文件分组添加到结果列表中
        List<Meta.FileConfigDTO.FileInfo> resultList = new ArrayList<>(groupKeyMergedFileInfoListMap.values());

        // 4.将未分组的文件添加到结果列表中
        List<Meta.FileConfigDTO.FileInfo> noGroupFileInfoList = fileInfoList
                .stream()
                .filter(fileInfo -> StrUtil.isBlank(fileInfo.getGroupKey()))
                .collect(Collectors.toList());

        resultList.addAll(new ArrayList<>(noGroupFileInfoList
                .stream()
                .collect(Collectors.toMap(Meta.FileConfigDTO.FileInfo::getInputPath, o -> o, (e, r) -> r))
                .values()
        ));

        return resultList;
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
