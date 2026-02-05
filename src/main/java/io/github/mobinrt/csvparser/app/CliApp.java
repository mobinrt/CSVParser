package io.github.mobinrt.csvparser.app;

import io.github.mobinrt.csvparser.app.commands.ParseCommand;
import picocli.CommandLine;

public class CliApp {

    public static void main(String[] args) {
        int exitCode = new CommandLine(new RootCommand())
                .addSubcommand("parse", new ParseCommand())
                .execute(args);
        System.exit(exitCode);
    }
}
