package services.impls;

import com.google.inject.Inject;
import models.CodingSequence;
import models.Gene;
import org.jdeferred.Deferred;
import org.jdeferred.DeferredCallable;
import org.jdeferred.DeferredManager;
import org.jdeferred.Promise;
import services.contracts.GeneService;

import java.util.LinkedHashMap;
import java.util.concurrent.Callable;

public class DefaultGeneService implements GeneService {
    private final DeferredManager deferredManager;

    public String generateUrlForGene(String id) {
        return "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=nuccore&id="+id+"&rettype=fasta_cds_na&retmode=text";
    }

    @Inject
    public DefaultGeneService(DeferredManager deferredManager) {
        this.deferredManager = deferredManager;
    }

    public Promise<Gene, Throwable, Object> createGene(final String name, final int totalDinucleotides, final int totalTrinucleotides) {
        return deferredManager.when(new DeferredCallable<Gene, Object>() {
            @Override
            public Gene call() throws Exception {
                Gene gene = new Gene(name, totalDinucleotides, totalTrinucleotides);

                gene.setTrinuStatPhase0(initLinkedHashMap());
                gene.setTrinuStatPhase1(initLinkedHashMap());
                gene.setTrinuStatPhase2(initLinkedHashMap());

                gene.setTrinuProbaPhase0(initLinkedHashMapProba());
                gene.setTrinuProbaPhase1(initLinkedHashMapProba());
                gene.setTrinuProbaPhase2(initLinkedHashMapProba());

                gene.setDinuStatPhase0(initLinkedHashMapDinucleo());
                gene.setDinuStatPhase1(initLinkedHashMapDinucleo());

                gene.setDinuProbaPhase0(initLinkedHashMapDinucleoProba());
                gene.setDinuProbaPhase1(initLinkedHashMapDinucleoProba());

                return gene;
            }
        });
    }

    @Override
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
                    gene.getTrinuStatPhase0().put(codon0, gene.getTrinuStatPhase0().get(codon0) + 1);
                    gene.getTrinuStatPhase1().put(codon1, gene.getTrinuStatPhase1().get(codon1) + 1);
                    gene.getTrinuStatPhase2().put(codon2, gene.getTrinuStatPhase2().get(codon2) + 1);
                    j++;
                }

                gene.setTotalTrinucleotide(gene.getTotalTrinucleotide() + j);

                return gene;
            }
        });
    }

    @Override
    public Promise<Gene, Throwable, Void> extractStatisticsSequenceForDinucleotides(final String sequence, final Gene gene) {
        return deferredManager.when(new Callable<Gene>() {
            @Override
            public Gene call() throws Exception {
                String codon0, codon1;
                int j = 0;

                for (int i = 0; i < sequence.length()-(3+sequence.length()%2)+1; i += 2) {
                    codon0 = sequence.substring(i, i + 2);
                    codon1 = sequence.substring(i+1, i + 3);
                    gene.getDinuStatPhase0().put(codon0, gene.getDinuStatPhase0().get(codon0) + 1);
                    gene.getDinuStatPhase1().put(codon1, gene.getDinuStatPhase1().get(codon1) + 1);
                    j ++;
                }

                gene.setTotalDinucleotide(gene.getTotalDinucleotide() + j);

                return gene;
            }
        });
    }

    public static LinkedHashMap<String, Integer> initLinkedHashMap() {
        LinkedHashMap<String, Integer> hash = new LinkedHashMap<String, Integer>();
        for (CodingSequence.Nucleotide s0 : CodingSequence.Nucleotide.values()) {
            for (CodingSequence.Nucleotide s1 : CodingSequence.Nucleotide.values()) {
                for (CodingSequence.Nucleotide s2 : CodingSequence.Nucleotide.values()) {
                    hash.put(s0.toString() + s1.toString() + s2.toString(), 0);
                }
            }
        }
        return hash;
    }

    public static LinkedHashMap<String, Double> initLinkedHashMapProba() {
        LinkedHashMap<String, Double> hash = new LinkedHashMap<String, Double>();
        for (CodingSequence.Nucleotide s0 : CodingSequence.Nucleotide.values()) {
            for (CodingSequence.Nucleotide s1 : CodingSequence.Nucleotide.values()) {
                for (CodingSequence.Nucleotide s2 : CodingSequence.Nucleotide.values()) {
                    hash.put(s0.toString() + s1.toString() + s2.toString(), 0.0);
                }
            }
        }
        return hash;
    }

    public static LinkedHashMap<String, Integer> initLinkedHashMapDinucleo() {
        LinkedHashMap<String, Integer> hash = new LinkedHashMap<String, Integer>();
        for (CodingSequence.Nucleotide s0 : CodingSequence.Nucleotide.values()) {
            for (CodingSequence.Nucleotide s1 : CodingSequence.Nucleotide.values()) {
                hash.put(s0.toString() + s1.toString(), 0);
            }
        }
        return hash;
    }

    public static LinkedHashMap<String, Double> initLinkedHashMapDinucleoProba() {
        LinkedHashMap<String, Double> hash = new LinkedHashMap<String, Double>();
        for (CodingSequence.Nucleotide s0 : CodingSequence.Nucleotide.values()) {
            for (CodingSequence.Nucleotide s1 : CodingSequence.Nucleotide.values()) {
                hash.put(s0.toString() + s1.toString(), 0.0);
            }
        }
        return hash;
    }

}
