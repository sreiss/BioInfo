package services.contracts;

import com.google.common.util.concurrent.ListenableFuture;

import models.Gene;
import models.Kingdom;
import models.Organism;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface OrganismService {
    Organism createOrganism(String name, String bioProject, String group, String subGroup, Date updateDate, List<Tuple<String, String>> geneIds, String kingdomId);

    DateFormat getUpdateDateFormat();

    ListenableFuture<Organism> processOrganism(Kingdom kingdom, Organism organism);
    
    ListenableFuture<Organism> processOrganismWithoutGene(Map<String,Gene> genes, Kingdom kingdom, Organism organism);

    ListenableFuture<Organism> processOrganism(Kingdom kingdom, Organism organism, HashMap<String, Gene> plasmidGenesMap);
//    ListenableFuture<List<Organism>> extractGenes(List<Organism> organisms);
//    ListenableFuture<Organism> extractGenes(Organism organism);
//    ListenableFuture<List<Organism>> processGenes(List<Organism> organisms);
}
