package services.contracts;

import com.google.common.util.concurrent.ListenableFuture;
import models.Kingdom;
import models.Organism;
import java.util.Date;

public interface OrganismService {
    Organism createOrganism(String name, String group, String subGroup, Date updateDate, String[] geneIds, String kingdomId);
    ListenableFuture<Kingdom> processOrganisms(Kingdom kingdom);
//    ListenableFuture<List<Organism>> extractGenes(List<Organism> organisms);
//    ListenableFuture<Organism> extractGenes(Organism organism);
//    ListenableFuture<List<Organism>> processGenes(List<Organism> organisms);
}
