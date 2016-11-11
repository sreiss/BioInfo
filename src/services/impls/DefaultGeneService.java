package services.impls;

import com.google.api.client.http.HttpResponse;
import com.google.common.base.Function;
import com.google.common.util.concurrent.*;
import com.google.inject.Inject;
import models.CodingSequence;
import models.Gene;
import models.Organism;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import services.contracts.*;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class DefaultGeneService implements GeneService {
    private final StatisticsService statisticsService;
    private final HttpService httpService;
    private final ParseService parseService;
    private final ListeningExecutorService executorService;

    @Inject
    public DefaultGeneService(StatisticsService statisticsService, HttpService httpService, ParseService parseService, ListeningExecutorService listeningExecutorService) {
        this.statisticsService = statisticsService;
        this.httpService = httpService;
        this.parseService = parseService;
        this.executorService = listeningExecutorService;
    }

    public String generateUrlForGene(String id) {
        return "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=nuccore&id="+id+"&rettype=fasta_cds_na&retmode=text";
    }

    private ListenableFuture<HttpResponse> retrieveGene(String geneId) {
        String url = generateUrlForGene(geneId);
        return httpService.get(url);
    }

    private ListenableFuture<List<String>> extractSequences(InputStream inputStream, Gene gene) {
        return parseService.extractSequences(inputStream, gene);
    }

    private Gene createGene(final String name, final String path, final int totalDinucleotides, final int totalTrinucleotides) {

        Gene gene = new Gene(name, path, totalDinucleotides, totalTrinucleotides);

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

    private ListenableFuture<Gene> extractStatisticsSequenceForTrinucleotides(final String sequence, final Gene gene) {
        return executorService.submit(() -> {
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
        });
    }

    private ListenableFuture<Gene> extractStatisticsSequenceForDinucleotides(final String sequence, final Gene gene) {
        return executorService.submit(() -> {
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
        });
    }

    private ListenableFuture<Gene> extractStatisticsSequenceForDinucleotides(List<String> sequences, Gene gene) {
        List<ListenableFuture<Gene>> extractFutures = new ArrayList<>();
        for (String sequence: sequences) {
            extractFutures.add(extractStatisticsSequenceForDinucleotides(sequence, gene));
        }
        return Futures.transformAsync(Futures.allAsList(extractFutures), genes -> returnGene(gene), executorService);
    }


    private ListenableFuture<Gene> extractStatisticsSequenceForTrinucleotides(List<String> sequences, Gene gene) {
        List<ListenableFuture<Gene>> extractFutures = new ArrayList<>();
        for (String sequence: sequences) {
            extractFutures.add(extractStatisticsSequenceForTrinucleotides(sequence, gene));
        }
        return Futures.transformAsync(Futures.allAsList(extractFutures), genes -> returnGene(gene), executorService);
    }

    private ListenableFuture<Gene> returnGene(Gene gene) {
        return executorService.submit(() -> gene);
    }

    private ListenableFuture<XSSFSheet> compute(Organism organism, Gene gene, XSSFWorkbook workbook, List<String> sequences) {
        ListenableFuture<Gene> dinuFuture = extractStatisticsSequenceForDinucleotides(sequences, gene);
        ListenableFuture<Gene> trinuFuture = Futures.transformAsync(dinuFuture, computedGene -> extractStatisticsSequenceForTrinucleotides(sequences, computedGene), executorService);
        return Futures.transformAsync(trinuFuture, computedGene -> statisticsService.computeStatistics(organism, computedGene, workbook), executorService);
    }

    private ListenableFuture<XSSFSheet> processGene(Organism organism, XSSFWorkbook workbook, String geneId, String path) {
        Gene gene = createGene(geneId, path, 0, 0);
        ListenableFuture<HttpResponse> responseFuture = retrieveGene(geneId);
        ListenableFuture<InputStream> inputStreamFuture = Futures.transform(responseFuture, new Function<HttpResponse, InputStream>() {
            @Nullable
            @Override
            public InputStream apply(HttpResponse httpResponse) {
                try {
                    return httpResponse.getContent();
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        });
        ListenableFuture<List<String>> extractSequencesFuture = Futures.transformAsync(inputStreamFuture, inputStream -> extractSequences(inputStream, gene), executorService);
        return Futures.transformAsync(extractSequencesFuture, sequences -> compute(organism, gene, workbook, sequences), executorService);
    }

    @Override
    public ListenableFuture<XSSFWorkbook> processGenes(Organism organism, XSSFWorkbook workbook, String[] geneIds, String path) {
        List<ListenableFuture<XSSFSheet>> geneFutures = new ArrayList<>();
        if (geneIds != null && geneIds.length > 0) {
            for (String geneId : geneIds) {
                geneFutures.add(processGene(organism, workbook, geneId, path));
            }
        }
        return Futures.transform(Futures.successfulAsList(geneFutures), new Function<List<XSSFSheet>, XSSFWorkbook>() {
            @Nullable
            @Override
            public XSSFWorkbook apply(@Nullable List<XSSFSheet> unused) {
                return workbook;
            }
        }, executorService);
    }

    private LinkedHashMap<String, Integer> initLinkedHashMap() {
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

    private LinkedHashMap<String, Double> initLinkedHashMapProba() {
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

    private LinkedHashMap<String, Integer> initLinkedHashMapDinucleo() {
        LinkedHashMap<String, Integer> hash = new LinkedHashMap<String, Integer>();
        for (CodingSequence.Nucleotide s0 : CodingSequence.Nucleotide.values()) {
            for (CodingSequence.Nucleotide s1 : CodingSequence.Nucleotide.values()) {
                hash.put(s0.toString() + s1.toString(), 0);
            }
        }
        return hash;
    }

    private LinkedHashMap<String, Double> initLinkedHashMapDinucleoProba() {
        LinkedHashMap<String, Double> hash = new LinkedHashMap<String, Double>();
        for (CodingSequence.Nucleotide s0 : CodingSequence.Nucleotide.values()) {
            for (CodingSequence.Nucleotide s1 : CodingSequence.Nucleotide.values()) {
                hash.put(s0.toString() + s1.toString(), 0.0);
            }
        }
        return hash;
    }

}
