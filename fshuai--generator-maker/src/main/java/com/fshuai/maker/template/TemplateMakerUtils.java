package com.fshuai.maker.template;

import cn.hutool.core.util.StrUtil;
import com.fshuai.maker.meta.Meta;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 模版制作工具类
 */
public class TemplateMakerUtils {

    /**
     * 从为分组文件中移除组内的同名文件
     *
     * @param fileInfoList 输入文件集合
     * @return 移除组外同名文件的集合
     */
    public static List<Meta.FileConfigDTO.FileInfo> removeGroupFilesFromRoot(List<Meta.FileConfigDTO.FileInfo> fileInfoList) {

        // 1.先获取所有分组
        List<Meta.FileConfigDTO.FileInfo> groupFileInfoList = fileInfoList.stream()
                .filter(fileInfo -> StrUtil.isNotBlank(fileInfo.getGroupKey()))
                .collect(Collectors.toList());
        // 2.获取所有分组内的列表
        List<Meta.FileConfigDTO.FileInfo> groupInnerFileInfoList = groupFileInfoList.stream()
                .flatMap(fileInfo -> fileInfo.getFiles().stream())
                .collect(Collectors.toList());
        // 3.获取所有分组内文件输入路径集合
        List<String> fileInputPathSet = groupInnerFileInfoList.stream()
                .map(Meta.FileConfigDTO.FileInfo::getInputPath)
                .collect(Collectors.toList());
        // 4.利用上述集合，移除所有输入路径，在集合中的外层文件
        return fileInfoList.stream()
                .filter(fileInfo -> !fileInputPathSet.contains(fileInfo.getInputPath()))
                .collect(Collectors.toList());
    }
}
