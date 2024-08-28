package ${basePackage}.cli.command;

import cn.hutool.core.io.FileUtil;
import picocli.CommandLine;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

/**
* 遍历模板文件列表
*/
@CommandLine.Command(name = "list", description = "获取模板文件列表", mixinStandardHelpOptions = true)
public class ListCommand implements Runnable {

    @Override
    public void run() {
        String projectPath = "${fileConfig.inputRootPath}";

        List<File> files = FileUtil.loopFiles(projectPath);

        for (File file : files) {
            System.out.println(file);
        }
    }
}