package com.fshuai.maker.generator.commandProcess;

import java.io.*;

public class JarGenerator {

    public static void doGenerate(String projectDir) throws IOException,
            InterruptedException {
        // 清理之前的构建并打包
        String winMavenCommand = "mvn.cmd clean package -DskipTests=true";
        String otherMavenCommand = "mvn clean package -DskipTests=true";
        String mavenCommand = otherMavenCommand;
        CommandProcess.process(projectDir, mavenCommand);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        doGenerate("/Users/fengshuai/Documents/project/fshuai-code-generator/fshuai--generator-maker/generated/acm-template-pro-generator");
    }
}
