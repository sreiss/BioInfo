package services.contracts;

import models.Gene;
import org.jdeferred.Promise;

import java.util.HashMap;
import java.util.List;

public interface GeneService {
    String generateUrlForGene(String id);
    Promise<Gene, Throwable, Object> createGene(String name, int totalDinucleotides, int totalTrinucleotides);
    Promise<Gene, Throwable, Void> extractStatisticsSequenceForTrinucleotides(String sequence, Gene gene);
    Promise<Gene, Throwable, Void> extractStatisticsSequenceForDinucleotides(String sequence, Gene gene);
    Promise<List<Gene>, Throwable, Object> processGenes(List<String> geneIds);
}
