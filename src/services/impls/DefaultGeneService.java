package services.impls;

import com.google.api.client.http.HttpResponse;
import com.google.common.util.concurrent.*;
import com.google.inject.Inject;
import models.CodingSequence;
import models.Gene;
import models.Organism;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import services.contracts.*;
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

    private ListenableFuture<InputStream> getInputStream(HttpResponse response) {
        return executorService.submit(response::getContent);
    }

    private ListenableFuture<List<String>> extractSequences(InputStream inputStream, Gene gene) {
        return parseService.extractSequences(inputStream, gene);
    }

    private ListenableFuture<Gene> createGene(final String name, final String path, final int totalDinucleotides, final int totalTrinucleotides) {
        return executorService.submit(() -> {
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
        });
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

    private ListenableFuture<Void> returnVoid() {
        return executorService.submit(() -> null);
    }

    private ListenableFuture<Gene> compute(Organism organism, Gene gene, XSSFWorkbook workbook, List<String> sequences) {
        List<ListenableFuture<Gene>> computeFutures = new ArrayList<>();
        computeFutures.add(extractStatisticsSequenceForTrinucleotides(sequences, gene));
        computeFutures.add(extractStatisticsSequenceForDinucleotides(sequences, gene));

        ListenableFuture<List<Gene>> extractFuture = Futures.allAsList(computeFutures);
        ListenableFuture<List<Gene>> computeFuture = Futures.transformAsync(extractFuture, genes -> {
            List<ListenableFuture<Gene>> computedFuture = new ArrayList<>();
            if (genes != null) {
                for (Gene gene1 : genes) {
                    computedFuture.add(statisticsService.computeStatistics(organism, gene1, workbook));
                }
            }
            return Futures.allAsList(computedFuture);
        }, executorService);
        return Futures.transformAsync(computeFuture, genes -> returnGene(genes.get(1)), executorService);
    }

    private ListenableFuture<Void> processGene(Organism organism, XSSFWorkbook workbook, String geneId, String path) {
        final Gene[] gene = new Gene[1];
        ListenableFuture<Gene> createGeneFuture = createGene(geneId, path, 0, 0);
        ListenableFuture<HttpResponse> responseFuture = Futures.transformAsync(createGeneFuture, createdGene -> { gene[0] = createdGene; return retrieveGene(geneId); }, executorService);
        ListenableFuture<InputStream> inputStreamFuture = Futures.transformAsync(responseFuture, this::getInputStream, executorService);
        ListenableFuture<List<String>> extractSequencesFuture = Futures.transformAsync(inputStreamFuture, inputStream -> extractSequences(inputStream, gene[0]), executorService);
        ListenableFuture<Gene> computeFuture = Futures.transformAsync(extractSequencesFuture, sequences -> compute(organism, gene[0], workbook, sequences), executorService);
        return Futures.transformAsync(computeFuture, computedGene -> null, executorService);
    }

    @Override
    public ListenableFuture<Void> processGenes(Organism organism, XSSFWorkbook workbook, String[] geneIds, String path) {
        List<ListenableFuture<Void>> geneFutures = new ArrayList<>();
        if (geneIds != null && geneIds.length > 0) {
            for (String geneId : geneIds) {
                geneFutures.add(processGene(organism, workbook, geneId, path));
            }
        }
        return Futures.transformAsync(Futures.allAsList(geneFutures), aVoid -> returnVoid(), executorService);
    }



    //    @Override
//    public Promise<List<Gene>, Throwable, Object> processGenes(final List<String> geneIds) {
//        if (geneIds != null) {
//            List<String> urls = new ArrayList<String>();
//            for (String geneId: geneIds) {
//                urls.add(generateUrlForGene(geneId));
//            }
//            return httpService.get(urls)
//                    .then((DonePipe<List<HttpResponse>, MultipleResults, OneReject, MasterProgress>) responses -> {
//                        List<Promise<List<String>, Throwable, Object>> sequences = new ArrayList<Promise<List<String>, Throwable, Object>>();
//                        for (int i = 0; i < geneIds.size(); i++) {
//                            try {
//                                sequences.add(parseService.extractSequences(responses.get(i).getContent()));
//                            } catch (IOException e) {
//                                sequences.add(null);
//                            }
//                        }
//                        return deferredManager.when(sequences.toArray(new Promise[sequences.size()]));
//                    })
//                    .then((DonePipe<MultipleResults, List<Gene>, Throwable, Object>) oneResults -> {
//                        List<Gene> genes = new ArrayList<Gene>();
//                        for (int i = 0; i < geneIds.size(); i++) {
//                            genes.add(new Gene(geneIds.get(i), 0, 0));
//                        }
//                        return new DeferredObject<List<Gene>, Throwable, Object>().resolve(genes).promise();
//                    });
//        } else {
//            return new DeferredObject<List<Gene>, Throwable, Object>()
//                    .resolve(null)
//                    .promise();
//        }
//    }
//
//    @Override
//    public Promise<Gene, Throwable, Object> createGene(final String name, final int totalDinucleotides, final int totalTrinucleotides) {
//        return deferredManager.when(new DeferredCallable<Gene, Object>() {
//            @Override
//            public Gene call() throws Exception {
//                Gene gene = new Gene(name, totalDinucleotides, totalTrinucleotides);
//
//                gene.setTrinuStatPhase0(initLinkedHashMap());
//                gene.setTrinuStatPhase1(initLinkedHashMap());
//                gene.setTrinuStatPhase2(initLinkedHashMap());
//
//                gene.setTrinuProbaPhase0(initLinkedHashMapProba());
//                gene.setTrinuProbaPhase1(initLinkedHashMapProba());
//                gene.setTrinuProbaPhase2(initLinkedHashMapProba());
//
//                gene.setDinuStatPhase0(initLinkedHashMapDinucleo());
//                gene.setDinuStatPhase1(initLinkedHashMapDinucleo());
//
//                gene.setDinuProbaPhase0(initLinkedHashMapDinucleoProba());
//                gene.setDinuProbaPhase1(initLinkedHashMapDinucleoProba());
//
//                return gene;
//            }
//        });
//    }
//
//    @Override
//    public Promise<Gene, Throwable, Void> extractStatisticsSequenceForTrinucleotides(final String sequence, final Gene gene) {
//        return deferredManager.when(() -> {
//            if (sequence.length() % 3 != 0) {
//                throw new Exception("Invalid sequence.");
//            }
//            String codon0, codon1, codon2;
//
//            int j = 0;
//
//            for (int i = 0; i < sequence.length() - 3; i += 3) {
//                codon0 = sequence.substring(i, i + 3);
//                codon1 = sequence.substring(i + 1, i + 4);
//                codon2 = sequence.substring(i + 2, i + 5);
//                gene.getTrinuStatPhase0().put(codon0, gene.getTrinuStatPhase0().get(codon0) + 1);
//                gene.getTrinuStatPhase1().put(codon1, gene.getTrinuStatPhase1().get(codon1) + 1);
//                gene.getTrinuStatPhase2().put(codon2, gene.getTrinuStatPhase2().get(codon2) + 1);
//                j++;
//            }
//
//            gene.setTotalTrinucleotide(gene.getTotalTrinucleotide() + j);
//
//            return gene;
//        });
//    }
//
//    @Override
//    public Promise<Gene, Throwable, Void> extractStatisticsSequenceForDinucleotides(final String sequence, final Gene gene) {
//        return deferredManager.when(() -> {
//            String codon0, codon1;
//            int j = 0;
//
//            for (int i = 0; i < sequence.length()-(3+sequence.length()%2)+1; i += 2) {
//                codon0 = sequence.substring(i, i + 2);
//                codon1 = sequence.substring(i+1, i + 3);
//                gene.getDinuStatPhase0().put(codon0, gene.getDinuStatPhase0().get(codon0) + 1);
//                gene.getDinuStatPhase1().put(codon1, gene.getDinuStatPhase1().get(codon1) + 1);
//                j ++;
//            }
//
//            gene.setTotalDinucleotide(gene.getTotalDinucleotide() + j);
//
//            return gene;
//        });
//    }
//
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
