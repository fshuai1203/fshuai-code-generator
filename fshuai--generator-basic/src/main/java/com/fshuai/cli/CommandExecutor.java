package com.fshuai.cli;

import com.fshuai.cli.command.ConfigCommand;
import com.fshuai.cli.command.GenerateCommand;
import com.fshuai.cli.command.ListCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "fshuai", mixinStandardHelpOptions = true)
public class CommandExecutor implements Runnable {

    // 使用命令行添加子命令
    private final CommandLine commandLine;

    {
        commandLine = new CommandLine(this)
                .addSubcommand(new ConfigCommand())
                .addSubcommand(new GenerateCommand())
                .addSubcommand(new ListCommand());
    }

    @Override
    public void run() {
        // 执行父命令，如果没有输入子命令，则进行提示
        System.out.println("请输入具体命令，或者输入 --help 查看命令提示");
    }

    /**
     * 执行命令
     */
    public Integer doExecute(String[] args) {
        return commandLine.execute(args);
    }
}
