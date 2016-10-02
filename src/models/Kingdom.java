package models;

import java.util.ArrayList;
import java.util.List;

public enum Kingdom {
    Eukaryota("euks", "Eukaryota"),
    Prokaryotes("proks", "Prokaryotes"),
    Viruses("viruses", "Viruses");

    private final String id;
    private final String label;
    private List<Organism> organisms = new ArrayList<Organism>();

    Kingdom(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public boolean equals(String id) {
        return id.equals(this.id);
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public List<Organism> getOrganisms() {
        return organisms;
    }

    public void setOrganisms(List<Organism> organisms) {
        this.organisms = organisms;
    }
}
