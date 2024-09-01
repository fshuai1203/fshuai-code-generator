package com.fshuai.maker.generator.commandProcess;

import java.io.*;

public class GitGenerator {

    public static void doGenerate(String projectDir) throws IOException, InterruptedException {
        // git 初始化指令
        String gitCommand = "git init";

        CommandProcess.process(projectDir, gitCommand);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        doGenerate("/Users/fengshuai/Documents/project/fshuai-code-generator/fshuai--generator-maker/generated/acm-template-pro-generator");
    }
}
