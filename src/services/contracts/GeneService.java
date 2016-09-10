package services.contracts;

import models.Gene;
import org.jdeferred.Promise;

public interface GeneService {
    Promise<Gene, Throwable, Void> createGene(String name, int totalDinucleotides, int totalTrinucleotides);
    Promise<Gene, Throwable, Void> extractStatisticsSequenceForTrinucleotides(String sequence, Gene gene);
    Promise<Gene, Throwable, Void> extractStatisticsSequenceForDinucleotides(String sequence, Gene gene);
}
