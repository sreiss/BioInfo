package models;

import services.contracts.Tuple;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Contains information about an organism
 */
public class Organism {

    private String name;
    private String bioProject;
    private ProkaryoteGroup prokaryoteGroup;
    private String group;
    private String subGroup;
    private Date updatedDate;

    private Kingdom kingdom;
    private String kingdomId;

    private String path;

    public String[] id;
    private List<Tuple<String, String>> geneIds;
    private int idIndex=9;

    public Organism(String name, String bioProject, String group, String subGroup, Date updateDate, List<Tuple<String, String>> geneIds, String kingdomId) {
        this.name = name;
        this.bioProject = bioProject;
        this.group = group;
        this.subGroup = subGroup;
        this.updatedDate = updateDate;
        this.kingdomId = kingdomId;
        this.geneIds = geneIds;

//        String[] data = s.split(sep);
//
//        int nameIndex = 0;
//        int groupIndex = 0;
//        int subGroupIndex = 0;
//        int updatedDateIndex = 0;
//
//        DateFormat format = new SimpleDateFormat("dd/mm/yyyy", Locale.FRENCH);
//
//        if (kingdomId.equals(Kingdom.Eukaryota.toString())) {
//            nameIndex = 0;
//            groupIndex = 4;
//            subGroupIndex = 5;
//            updatedDateIndex = 15;
//
//            name = data[nameIndex];
//            group = data[groupIndex];
//            subGroup = data[subGroupIndex];
//            //System.out.println(data[idIndex]);
//            id = extractIds(data[idIndex]);
//        } else if (kingdomId.equals(Kingdom.PROKS)) {
//            nameIndex = 0;
//            groupIndex = 5;
//            subGroupIndex = 6;
//            idIndex=10;
//            updatedDateIndex = 16;
//            name = data[nameIndex];
//            group = data[groupIndex];
//            subGroup = data[subGroupIndex];
//            //System.out.println(data[idIndex]);
//            id = extractIds(data[idIndex]);
//        }
//        else if (kingdomId.equals(Kingdom.VIRUSES)) {
//            nameIndex = 0;
//            groupIndex = 2;
//            subGroupIndex = 3;
//            idIndex=7;
//            updatedDateIndex = 11;
//            name = data[nameIndex];
//            group = data[groupIndex];
//            subGroup = data[subGroupIndex];
//            //System.out.println(data[idIndex]);
//            id = extractIds(data[idIndex]);
//        }
//
//        if (data.length > updatedDateIndex) {
//            if (data[updatedDateIndex].compareTo("-") == 0) {
//                updatedDate = new Date();
//            } else {
//                try {
//                    updatedDate = format.parse(data[updatedDateIndex]);
//                } catch (ParseException e) {
//                    System.err.println(e.getDownloaded());
//                    e.printStackTrace();
//                }
//            }
//        }
        
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Organism) {
            Organism orga = (Organism)obj;
            return (orga.name.equals(this.name)
                    && orga.group.equals(this.group)
                    && orga.subGroup.equals(this.subGroup));
        } else {
            return false;
        }
    }

    public Kingdom getKingdom() {
        return kingdom;
    }

    public void setKingdom(Kingdom kingdom) {
        this.kingdom = kingdom;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getSubGroup() {
        return subGroup;
    }

    public void setSubGroup(String subGroup) {
        this.subGroup = subGroup;
    }

    public Date getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(Date updatedDate) {
        this.updatedDate = updatedDate;
    }

    public String getKingdomId() {
        return kingdomId;
    }

    public void setKingdomId(String kingdomId) {
        this.kingdomId = kingdomId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<Tuple<String, String>> getGeneIds() {
        return geneIds;
    }

    public void setGeneIds(List<Tuple<String, String>> geneIds) {
        this.geneIds = geneIds;
    }

    public String getBioProject() {
        return bioProject;
    }

    public void setBioProject(String bioProject) {
        this.bioProject = bioProject;
    }

    public ProkaryoteGroup getProkaryoteGroup() {
        return prokaryoteGroup;
    }

    public void setProkaryoteGroup(ProkaryoteGroup prokaryoteGroup) {
        this.prokaryoteGroup = prokaryoteGroup;
    }
}
