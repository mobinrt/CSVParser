package io.github.mobinrt.csvparser.domain.model;

public final class ColumnDef {

    private final String name;
    private final String type; //optional

    public ColumnDef(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }
}
