package services.contracts;

import models.Gene;
import models.Organism;
import org.jdeferred.Promise;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

public interface OrganismService {
    Promise<Organism, Throwable, Object> createOrganism(String name, String group, String subGroup, Date updateDate, String[] geneIds, String kingdomId);
    Promise<List<Organism>, Throwable, Object> extractGenes(List<Organism> organisms);
    Promise<Organism, Throwable, Object> extractGenes(Organism organism);
}
