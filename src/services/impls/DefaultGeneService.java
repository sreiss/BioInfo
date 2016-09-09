package services.impls;

import com.google.inject.Inject;
import models.Gene;
import org.jdeferred.DeferredManager;
import org.jdeferred.Promise;
import services.contracts.GeneService;

import java.util.concurrent.Callable;

public class DefaultGeneService implements GeneService {
    private final DeferredManager deferredManager;

    @Inject
    public DefaultGeneService(DeferredManager deferredManager) {
        this.deferredManager = deferredManager;
    }

    public Promise<Gene, Throwable, Void> extractStatisticsSequenceForTrinucleotides(final String sequence, final Gene gene) {
        return deferredManager.when(new Callable<Gene>() {
            @Override
            public Gene call() throws Exception {
                if (sequence.length() % 3 != 0) {
                    throw new Exception("Invalid sequence.");
                }
                String codon0, codon1, codon2;

                int j = 0;

                for (int i = 0; i < sequence.length() - 3; i += 3) {
                    codon0 = sequence.substring(i, i + 3);
                    codon1 = sequence.substring(i + 1, i + 4);
                    codon2 = sequence.substring(i + 2, i + 5);
                    gene.trinuStatPhase0.put(codon0, gene.trinuStatPhase0.get(codon0) + 1);
                    gene.trinuStatPhase1.put(codon1, gene.trinuStatPhase1.get(codon1) + 1);
                    gene.trinuStatPhase2.put(codon2, gene.trinuStatPhase2.get(codon2) + 1);
                    j++;
                }

                gene.totalTrinucleotide += j;

                return gene;
            }
        });
    }

}
