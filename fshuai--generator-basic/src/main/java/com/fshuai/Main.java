package com.fshuai;

import com.fshuai.cli.CommandExecutor;

public class Main {
    public static void main(String[] args) {
//        args = new String[]{"generate","-l","-a","-o"};
//        args = new String[]{"list"};
//        args = new String[]{"config"};
        CommandExecutor commandExecutor = new CommandExecutor();
        commandExecutor.doExecute(args);
    }
}