package io.github.mobinrt.csvparser.app;

import picocli.CommandLine.Command;

@Command(
        name = "csv-parser",
        mixinStandardHelpOptions = true,
        version = "csv-parser 1.0.0",
        description = "Stream-parse CSV files and store rows in MySQL (dynamic table + error table)."
)
public final class RootCommand implements Runnable {

    @Override
    public void run() {
        // Root command does nothing. Use subcommands.
    }
}
