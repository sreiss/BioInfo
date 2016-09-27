package services.contracts;

import models.Organism;
import org.jdeferred.Promise;

import java.util.Date;

public interface OrganismService {
    Promise<Organism, Throwable, Object> createOrganism(String name, String group, String subGroup, Date updateDate, String[] geneIds, String kingdomId);
}
