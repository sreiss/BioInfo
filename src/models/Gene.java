package models;

import java.util.LinkedHashMap;
import java.util.Set;

public class Gene extends NucleotidesHolder {

    private String name;
    private String type;
    private String path;

    public Gene(String name, String type, String path, int totalDinucleotide, int totalTrinucleotide)
    {
        this.name = name;
        this.type = type;
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public String getType() {
        return type;
    }
}
