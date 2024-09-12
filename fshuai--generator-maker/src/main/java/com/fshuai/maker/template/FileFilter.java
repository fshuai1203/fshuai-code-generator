package com.fshuai.maker.template;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import com.fshuai.maker.template.enums.FileFilterRangeEnum;
import com.fshuai.maker.template.enums.FileFilterRuleEnum;
import com.fshuai.maker.template.model.FileFilterConfig;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class FileFilter {


    /**
     * 根据过滤配置对指定文件路径下的文件进行过滤
     *
     * @param filePath 文件路径，可以是单个文件或者一个目录
     * @param fileFilterConfigList 文件过滤配置列表，包含各个文件类型的过滤规则
     * @return 过滤后的文件列表
     */
    public static List<File> doFilter(String filePath, List<FileFilterConfig> fileFilterConfigList) {
        // 获取指定文件路径下所有的文件列表
        List<File> fileList = FileUtil.loopFiles(filePath);

        // 对文件列表进行流式处理，根据文件过滤配置对每个文件进行过滤，并收集过滤后的文件到新的列表中
        return fileList.stream()
                .filter(file -> doSingleFileFilter(fileFilterConfigList, file))
                .collect(Collectors.toList());
    }


    /**
     * 根据文件过滤配置列表对单个文件进行过滤校验
     *
     * @param fileFilterConfigList 文件过滤配置列表，包含过滤范围、规则和值
     * @param file                 待校验的文件
     * @return 如果文件通过所有过滤规则的校验返回true，否则返回false
     */
    private static boolean doSingleFileFilter(List<FileFilterConfig> fileFilterConfigList, File file) {

        // 获取文件名
        String fileName = file.getName();
        // 读取文件内容，假设文件内容以UTF-8编码
        String fileContent = FileUtil.readUtf8String(file);

        // 所有过滤校验结束的结果，默认为true，即默认文件通过过滤
        boolean result = true;

        // 如果文件过滤配置列表为空，则认为文件通过过滤
        if (CollUtil.isEmpty(fileFilterConfigList)) {
            return true;
        }

        // 遍历文件过滤配置列表
        for (FileFilterConfig fileFilterConfig : fileFilterConfigList) {
            // 获取当前过滤配置的范围、规则和值
            String range = fileFilterConfig.getRange();
            String rule = fileFilterConfig.getRule();
            String value = fileFilterConfig.getValue();

            // 根据范围值获取对应的枚举类型
            FileFilterRangeEnum fileFilterRangeEnum = FileFilterRangeEnum.getEnumByValue(range);
            // 如果范围枚举类型为空，则跳过当前配置
            if (fileFilterRangeEnum == null) {
                continue;
            }

            // 根据过滤范围确定要过滤的内容是文件名还是文件内容
            String content = fileName;
            switch (fileFilterRangeEnum) {
                case FILE_NAME:
                    // 如果范围是文件名，则使用文件名作为过滤内容
                    content = fileName;
                    break;
                case FILE_CONTENT:
                    // 如果范围是文件内容，则使用文件内容作为过滤内容
                    content = fileContent;
                    break;
                default:
            }

            // 根据规则值获取对应的枚举类型
            FileFilterRuleEnum filterRuleEnum = FileFilterRuleEnum.getEnumByValue(rule);
            // 如果规则枚举类型为空，则跳过当前配置
            if (filterRuleEnum == null) {
                continue;
            }
            // 根据过滤规则对内容进行过滤校验
            switch (filterRuleEnum) {
                case CONTAINS:
                    // 检查内容是否包含指定值
                    result = content.contains(value);
                    break;
                case STARTS_WITH:
                    // 检查内容是否以指定值开始
                    result = content.startsWith(value);
                    break;
                case ENDS_WITH:
                    // 检查内容是否以指定值结束
                    result = content.endsWith(value);
                    break;
                case REGEX:
                    // 检查内容是否匹配指定的正则表达式
                    result = content.matches(value);
                    break;
                case EQUALS:
                    // 检查内容是否等于指定值
                    result = content.equals(value);
                    break;
                default:
            }

            // 如果当前过滤规则的校验结果为false，则立即返回false
            if (!result) {
                return false;
            }
        }

        // 所有过滤规则的校验结果都为true，则返回true
        return true;

    }


}
