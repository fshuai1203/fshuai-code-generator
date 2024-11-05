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
import com.fshuai.maker.template.model.TemplateMakerConfig;
import com.fshuai.maker.template.model.TemplateMakerFileConfig;
import com.fshuai.maker.template.model.TemplateMakerModelConfig;
import com.fshuai.maker.template.model.TemplateMakerOutputConfig;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class TemplateMaker {


    public static long makeTemplate(TemplateMakerConfig templateMakerConfig) {
        return makeTemplate(
                templateMakerConfig.getMeta(),
                templateMakerConfig.getOriginProjectPath(),
                templateMakerConfig.getFileConfig(),
                templateMakerConfig.getModelConfig(),
                templateMakerConfig.getOutputConfig(),
                templateMakerConfig.getId());
    }

    /**
     * 生成模版
     *
     * @param newMeta                  元信息
     * @param originProjectPath        源项目路径
     * @param templateMakerFileConfig  文件列表信息(包括文件过滤字段)
     * @param templateMakerModelConfig 模型配置信息
     * @param id                       生成的临时文件夹的名称(ID)
     * @return
     */
    public static long makeTemplate(Meta newMeta,
                                    String originProjectPath,
                                    TemplateMakerFileConfig templateMakerFileConfig,
                                    TemplateMakerModelConfig templateMakerModelConfig,
                                    TemplateMakerOutputConfig templateMakerOutputConfig,
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
        String sourceRootPath = FileUtil.loopFiles(new File(templatePath), 1, null)
                .stream()
                .filter(File::isDirectory)
                .findFirst()
                .orElseThrow(RuntimeException::new)
                .getAbsolutePath();

        // 制作文件模版
        List<Meta.FileConfigDTO.FileInfo> newFileInfoList = makeFileTemplates(templateMakerFileConfig, templateMakerModelConfig, sourceRootPath);

        // 处理模型信息
        List<Meta.ModelConfig.ModelInfo> newModelInfoList = getModelInfoList(templateMakerModelConfig);


        // 生成配置文件,与项目同级
        String metaOutputPath = templatePath + File.separator + "meta.json";

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
            modelInfoList.addAll(newModelInfoList);
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
            modelInfoList.addAll(newModelInfoList);

            // 去重
            newMeta.getFileConfig().setFiles(distinctFiles(fileInfoList));
            newMeta.getModelConfig().setModels(distinctModels(modelInfoList));
        }

        //  额外的输出配置
        if (templateMakerOutputConfig != null) {
            // 文件外层和分组去重
            if (templateMakerOutputConfig.isRemoveGroupFilesFromRoot()) {
                List<Meta.FileConfigDTO.FileInfo> fileInfoList = newMeta.getFileConfig().getFiles();
                newMeta.getFileConfig().setFiles(TemplateMakerUtils.removeGroupFilesFromRoot(fileInfoList));
            }
        }

        // 写入元信息文件
        FileUtil.writeUtf8String(JSONUtil.toJsonPrettyStr(newMeta), metaOutputPath);
        // 返回生成的模版ID
        return id;
    }

    /**
     * 从模板制作模型配置中提取模型信息列表
     *
     * 此方法主要用于处理模板制作模型配置，将其转换为模型信息列表（ModelInfo），
     * 并根据配置中的分组信息（如果有）进行相应的处理
     *
     * @param templateMakerModelConfig 模板制作模型配置对象，包含模型配置数据
     * @return 返回一个ModelInfo对象列表，包含处理后的模型信息
     */
    private static List<Meta.ModelConfig.ModelInfo> getModelInfoList(TemplateMakerModelConfig templateMakerModelConfig) {

        // 本次新增的模型配置列表
        List<Meta.ModelConfig.ModelInfo> newModelInfoList = new ArrayList<>();

        // 非空验证
        if (templateMakerModelConfig == null) {
            return newModelInfoList;
        }

        // 模型分组
        List<TemplateMakerModelConfig.ModelInfoConfig> models = templateMakerModelConfig.getModels();

        if (CollUtil.isEmpty(models)) {
            return newModelInfoList;
        }

        // 转换为配置接受的ModelInfo对象
        List<Meta.ModelConfig.ModelInfo> inputModelInfoList = models.stream()
                .map(modelInfoConfig -> {
                    Meta.ModelConfig.ModelInfo modelInfo = new Meta.ModelConfig.ModelInfo();
                    BeanUtil.copyProperties(modelInfoConfig, modelInfo);
                    return modelInfo;
                })
                .collect(Collectors.toList());
        // 如果是模型分组就把当前模型放入分组中
        TemplateMakerModelConfig.ModelGroupConfig modelGroupConfig = templateMakerModelConfig.getModelGroupConfig();

        if (modelGroupConfig != null) {
            String condition = modelGroupConfig.getCondition();
            String groupKey = modelGroupConfig.getGroupKey();
            String groupName = modelGroupConfig.getGroupName();
            // 新增分组配置
            Meta.ModelConfig.ModelInfo groupModelInfo = new Meta.ModelConfig.ModelInfo();
            groupModelInfo.setCondition(condition);
            groupModelInfo.setGroupKey(groupKey);
            groupModelInfo.setGroupName(groupName);

            // 模型全放到一个分组内
            groupModelInfo.setModels(inputModelInfoList);
            newModelInfoList.add(groupModelInfo);
        } else {
            //  如果不是分组，直接添加模型信息到列表中
            newModelInfoList.addAll(inputModelInfoList);
        }
        return newModelInfoList;
    }

    /**
     * 根据模板制作文件配置和模型配置创建文件模板
     *
     * @param templateMakerFileConfig 模板制作文件配置
     * @param templateMakerModelConfig 模板制作模型配置
     * @param sourceRootPath 源文件根路径
     * @return 返回新的文件信息列表
     */
    private static List<Meta.FileConfigDTO.FileInfo> makeFileTemplates(TemplateMakerFileConfig templateMakerFileConfig, TemplateMakerModelConfig templateMakerModelConfig, String sourceRootPath) {
        // 初始化新的文件信息列表
        List<Meta.FileConfigDTO.FileInfo> newFileInfoList = new ArrayList<>();

        // 非空校验
        if (templateMakerFileConfig == null) {
            return newFileInfoList;
        }

        // 获取要修改的文件列表
        List<TemplateMakerFileConfig.FileInfoConfig> fileConfigInfoList = templateMakerFileConfig.getFiles();

        if (CollUtil.isEmpty(fileConfigInfoList)) {
            return newFileInfoList;
        }

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

            // 不处理已经生成的FTL模版文件
            List<File> fileList = filterFiles.stream()
                    .filter(file -> !file.getAbsolutePath().endsWith(".ftl"))
                    .collect(Collectors.toList());

            // 处理过滤后的文件
            for (File file : fileList) {
                // 生成文件模版
                Meta.FileConfigDTO.FileInfo fileInfo = makeFileTemplate(templateMakerModelConfig, file, sourceRootPath, fileInfoConfig);
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
        return newFileInfoList;
    }

    /**
     * 创建文件模板信息
     *
     * @param templateMakerModelConfig 模板制作模型配置
     * @param inputFile 输入文件
     * @param sourceRootPath 源文件根路径
     * @param fileInfoConfig 文件信息配置
     * @return 返回处理后的文件信息对象
     */
    private static Meta.FileConfigDTO.FileInfo makeFileTemplate(TemplateMakerModelConfig templateMakerModelConfig,
                                                                File inputFile,
                                                                String sourceRootPath,
                                                                TemplateMakerFileConfig.FileInfoConfig fileInfoConfig) {

        // 要挖坑的文件，把绝对路径替换成相对路径
        String fileInputPath = inputFile.getAbsolutePath().replace(sourceRootPath + "/", "");

        String fileOutputPath = fileInputPath + ".ftl";


        // 2-非首次制作，在已有模版上进行挖坑
        String fileInputAbsolutePath = inputFile.getAbsolutePath();
        String fileOutputAbsolutePath = inputFile.getAbsolutePath() + ".ftl";

        String fileContent = null;
        boolean hasTemplateFile = FileUtil.exist(fileOutputAbsolutePath);
        if (!hasTemplateFile) {
            // 首次制作
            // 使用字符串替换，生成模版文件

            fileContent = FileUtil.readUtf8String(fileInputAbsolutePath);
        } else {
            // 非首次制作
            fileContent = FileUtil.readUtf8String(fileOutputAbsolutePath);
        }

        // 支持多个模型:对于同一个文件，遍历模型进行多轮替换
        TemplateMakerModelConfig.ModelGroupConfig modelGroupConfig = templateMakerModelConfig.getModelGroupConfig();
        String newFileContent = fileContent;
        String replacement;

        for (TemplateMakerModelConfig.ModelInfoConfig modelInfoConfig : templateMakerModelConfig.getModels()) {
            // 不是分组，直接替换
            if (modelGroupConfig == null) {
                replacement = String.format("${%s}", modelInfoConfig.getFieldName());
            } else {
                // 是分组，则需要父级目录
                replacement = String.format("${%s.%s}", modelGroupConfig.getGroupKey(), modelInfoConfig.getFieldName());
            }
            // 多次替换
            newFileContent = StrUtil.replace(newFileContent, modelInfoConfig.getReplaceText(), replacement);
        }


        // 生成元信息配置文件
        // 3-非首次制作，在原有元信息配置上进行修改
        // 文件配置信息，无论修改还是新增，文件信息一定是存在的
        Meta.FileConfigDTO.FileInfo fileInfo = new Meta.FileConfigDTO.FileInfo();
        // 对于配置文件来说，输入应该时模版文件，输出应该是原始文件(这里的输入路径与输出路径要进行反转)
        fileInfo.setInputPath(fileOutputPath);
        fileInfo.setOutputPath(fileInputPath);
        fileInfo.setType(FileTypeEnum.FILE.getValue());
        fileInfo.setCondition(fileInfoConfig.getCondition());//给单个文件也设置condition
        // 默认设置成动态
        fileInfo.setGenerateType(FileGenerateTypeEnum.DYNAMIC.getValue());

        boolean contentEquals = newFileContent.equals(fileContent);
        // 只有之前没有模版文件，且替换后没有改变的才是静态生成
        if (!hasTemplateFile) {
            if (contentEquals) {
                // 没有模版文件，且没有发生改变
                // 输入路径没有FTL后缀
                fileInfo.setInputPath(fileInputPath);
                fileInfo.setGenerateType(FileGenerateTypeEnum.STATIC.getValue());
            } else {
                FileUtil.writeUtf8String(newFileContent, fileOutputAbsolutePath);
            }
        } else if (!contentEquals) {
            // 有模版文件，且发生了改变，则在生成模版文件
            FileUtil.writeUtf8String(newFileContent, fileOutputAbsolutePath);
        }
        //之前有模版文件，但是没有发生改变，则不需要更新

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
                    .collect(Collectors.toMap(Meta.FileConfigDTO.FileInfo::getOutputPath, o -> o, (e, r) -> r))
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

        // 策略:同分组内模型合并，不同分组保留

        // 1. 有分组的，以组为单位进行划分
        Map<String, List<Meta.ModelConfig.ModelInfo>> groupKeyModelInfoListMap = modelInfoList.stream()
                .filter(modelInfo -> StrUtil.isNotBlank(modelInfo.getGroupKey()))
                .collect(Collectors.groupingBy(Meta.ModelConfig.ModelInfo::getGroupKey));


        Map<String, Meta.ModelConfig.ModelInfo> groupKeyMergedModelInfoListMap = new HashMap<>();
        // 2. 同分组内的模型进行合并
        for (Map.Entry<String, List<Meta.ModelConfig.ModelInfo>> entry : groupKeyModelInfoListMap.entrySet()) {
            // 相同groupKey的多个模型列表
            List<Meta.ModelConfig.ModelInfo> tempModelInfoList = entry.getValue();

            ArrayList<Meta.ModelConfig.ModelInfo> newModelInfoList = new ArrayList<>(tempModelInfoList.stream()
                    .flatMap(modelInfo -> modelInfo.getModels().stream())//合并所有模型
                    .collect(Collectors.toMap(Meta.ModelConfig.ModelInfo::getFieldName, o -> o, (e, r) -> e))//合并相同的配置项
                    .values());

            // 使用新的group进行配置
            Meta.ModelConfig.ModelInfo newModelInfo = CollUtil.getLast(tempModelInfoList);
            newModelInfo.setModels(newModelInfoList);
            String groupKey = entry.getKey();
            groupKeyMergedModelInfoListMap.put(groupKey, newModelInfo);

        }

        // 3. 将模型分组添加到结果列表
        ArrayList<Meta.ModelConfig.ModelInfo> resultList = new ArrayList<>(groupKeyMergedModelInfoListMap.values());


        // 4.将未分组模型添加到结果列表
        List<Meta.ModelConfig.ModelInfo> noGroupModelInfoList = modelInfoList.stream()
                .filter(modelInfo -> StrUtil.isBlank(modelInfo.getGroupKey()))
                .collect(Collectors.toList());

        resultList.addAll(new ArrayList<>(noGroupModelInfoList
                .stream()
                .collect(Collectors.toMap(Meta.ModelConfig.ModelInfo::getFieldName, o -> o, (e, r) -> e))
                .values()
        ));

        return resultList;


    }


}
