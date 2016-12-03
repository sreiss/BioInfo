package services.impls;

import com.google.api.client.http.HttpResponse;
import com.google.common.base.Function;
import com.google.common.util.concurrent.*;
import com.google.inject.Inject;
import models.CodingSequence;
import models.Gene;
import models.Organism;
import models.Sum;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import services.contracts.*;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultGeneService implements GeneService {
    private final int PROCESS_STACK_SIZE = 20;
    private final StatisticsService statisticsService;
    private final HttpService httpService;
    private final ParseService parseService;
    private final ListeningExecutorService executorService;
    private final ProgramStatsService programStatsService;
    private final ProgressService progressService;
    private HashMap<Organism, AtomicInteger> organismOffsets = new HashMap<>();

    @Inject
    public DefaultGeneService(StatisticsService statisticsService, HttpService httpService, ParseService parseService, ListeningExecutorService listeningExecutorService, ProgramStatsService programStatsService, ProgressService progressService) {
        this.statisticsService = statisticsService;
        this.httpService = httpService;
        this.parseService = parseService;
        this.executorService = listeningExecutorService;
        this.programStatsService = programStatsService;
        this.progressService = progressService;
    }

    public String generateUrlForGene(String id) {
        return "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=nuccore&id="+id+"&rettype=fasta_cds_na&retmode=text";
    }

    private ListenableFuture<HttpResponse> retrieveGene(String geneId) {
        String url = generateUrlForGene(geneId);
        return httpService.get(url, geneId);
    }

    private ListenableFuture<List<String>> extractSequences(InputStream inputStream, Gene gene) {
        return parseService.extractSequences(inputStream, gene);
    }

    private Gene createGene(final String name, final String type, final String path, final int totalDinucleotides, final int totalTrinucleotides) {

        Gene gene = new Gene(name, type, path, totalDinucleotides, totalTrinucleotides);

        gene.setTrinuStatPhase0(initLinkedHashMap());
        gene.setTrinuStatPhase1(initLinkedHashMap());
        gene.setTrinuStatPhase2(initLinkedHashMap());

        gene.setTrinuProbaPhase0(initLinkedHashMapProba());
        gene.setTrinuProbaPhase1(initLinkedHashMapProba());
        gene.setTrinuProbaPhase2(initLinkedHashMapProba());

        gene.setTrinuPrefPhase0(initLinkedHashMap());
        gene.setTrinuPrefPhase2(initLinkedHashMap());
        gene.setTrinuPrefPhase1(initLinkedHashMap());

        gene.setDinuStatPhase0(initLinkedHashMapDinucleo());
        gene.setDinuStatPhase1(initLinkedHashMapDinucleo());

        gene.setDinuProbaPhase0(initLinkedHashMapDinucleoProba());
        gene.setDinuProbaPhase1(initLinkedHashMapDinucleoProba());

        return gene;
    }

    private Sum createSum(final String type, final String path, final int totalDinucleotides, final int totalTrinucleotides) {

        Sum sum = new Sum(type, path, totalDinucleotides, totalTrinucleotides);

        sum.setTrinuStatPhase0(initLinkedHashMap());
        sum.setTrinuStatPhase1(initLinkedHashMap());
        sum.setTrinuStatPhase2(initLinkedHashMap());

        sum.setTrinuProbaPhase0(initLinkedHashMapProba());
        sum.setTrinuProbaPhase1(initLinkedHashMapProba());
        sum.setTrinuProbaPhase2(initLinkedHashMapProba());

        sum.setTrinuPrefPhase0(initLinkedHashMap());
        sum.setTrinuPrefPhase2(initLinkedHashMap());
        sum.setTrinuPrefPhase1(initLinkedHashMap());

        sum.setDinuStatPhase0(initLinkedHashMapDinucleo());
        sum.setDinuStatPhase1(initLinkedHashMapDinucleo());

        sum.setDinuProbaPhase0(initLinkedHashMapDinucleoProba());
        sum.setDinuProbaPhase1(initLinkedHashMapDinucleoProba());

        return sum;
    }

    private ListenableFuture<Gene> extractStatisticsSequenceForTrinucleotides(final String sequence, final Gene gene) {
        return executorService.submit(() -> {
            if (sequence.length() % 3 != 0) {
                throw new Exception("Invalid sequence.");
            }
            String codon0, codon1, codon2;

            int j = 0;
            
            /*LinkedHashMap<String, Integer> beforeCount0 = gene.getTrinuStatPhase0();
            LinkedHashMap<String, Integer> beforeCount1 = gene.getTrinuStatPhase1();
            LinkedHashMap<String, Integer> beforeCount2 = gene.getTrinuStatPhase2();
            LinkedHashMap<String, Integer> afterCount0 = initLinkedHashMap();
            LinkedHashMap<String, Integer> afterCount1 = initLinkedHashMap();
            LinkedHashMap<String, Integer> afterCount2 = initLinkedHashMap();
*/
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
  /*          
            for(String s : beforeCount0.keySet()) {
            	afterCount0.put(s, gene.getTrinuStatPhase0().get(s) - beforeCount0.get(s));
            	afterCount1.put(s, gene.getTrinuStatPhase1().get(s) - beforeCount1.get(s));
            	afterCount2.put(s, gene.getTrinuStatPhase2().get(s) - beforeCount2.get(s));
            	
            	int max = Math.max(beforeCount0.get(s), Math.max(beforeCount1.get(s), beforeCount2.get(s)));
            	gene.getTrinuPrefPhase0().put(s, gene.getTrinuPrefPhase0().get(s) + (beforeCount0.get(s) == max ? 1 : 0));
            	gene.getTrinuPrefPhase1().put(s, gene.getTrinuPrefPhase1().get(s) + (beforeCount1.get(s) == max ? 1 : 0));
            	gene.getTrinuPrefPhase2().put(s, gene.getTrinuPrefPhase2().get(s) + (beforeCount2.get(s) == max ? 1 : 0));
            }
*/
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

    private ListenableFuture<XSSFSheet> computeSum(Organism organism, Gene gene, HashMap<String, Sum> organismSums, XSSFSheet sheet) {
        organismSums.putIfAbsent(gene.getType(), createSum(gene.getType(), organism.getPath(), 0, 0));
        return statisticsService.computeSum(organism, gene, organismSums, sheet);
    }

    private ListenableFuture<XSSFSheet> processGene(Organism organism, XSSFWorkbook workbook, String geneId, String type, String path, HashMap<String, Sum> organismSums) {
        Gene gene = createGene(geneId, type, path, 0, 0);
        ListenableFuture<HttpResponse> responseFuture = retrieveGene(geneId);
        ListenableFuture<InputStream> inputStreamFuture = Futures.transformAsync(responseFuture, httpResponse -> {
            try {
                progressService.getCurrentDownloadProgress().getProgress().incrementAndGet();
                progressService.getCurrentDownloadProgress().setDownloaded(geneId);
                progressService.invalidateDownloadProgress();
                return Futures.immediateFuture(httpResponse.getContent());
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        });
        ListenableFuture<List<String>> extractSequencesFuture = Futures.transformAsync(inputStreamFuture, inputStream -> extractSequences(inputStream, gene), executorService);
        ListenableFuture<XSSFSheet> computeFuture = Futures.transformAsync(extractSequencesFuture, sequences -> compute(organism, gene, workbook, sequences), executorService);
        ListenableFuture<XSSFSheet> sumFuture = Futures.transformAsync(computeFuture, sheet -> computeSum(organism, gene, organismSums, sheet), executorService);
        return Futures.transform(sumFuture, new Function<XSSFSheet, XSSFSheet>() {
            @Nullable
            @Override
            public XSSFSheet apply(@Nullable XSSFSheet sheet) {
                return sheet;
            }
        }, executorService);
    }

    @Override
    public ListenableFuture<XSSFWorkbook> processGenes(Organism organism, XSSFWorkbook workbook, List<Tuple<String, String>> geneIds, String path, HashMap<String, Sum> organismSums) {
        if (geneIds != null && geneIds.size() > 0) {
            List<ListenableFuture<XSSFSheet>> geneFutures = new ArrayList<>();

            organismOffsets.putIfAbsent(organism, new AtomicInteger());
            AtomicInteger currentOffset = organismOffsets.get(organism);

            int startIndex = currentOffset.get();
            int endIndex;
            boolean isLastLoop;
            if ((startIndex + PROCESS_STACK_SIZE) < geneIds.size()) {
                endIndex = (startIndex + PROCESS_STACK_SIZE);
                isLastLoop = false;
            } else {
                endIndex = geneIds.size();
                isLastLoop = true;
            }

            progressService.getCurrentDownloadProgress().getTotal().addAndGet(geneIds.size());
            progressService.invalidateDownloadProgress();
            for (Tuple<String, String> geneId: geneIds.subList(startIndex, endIndex)) {
                // X is the gene name, Y the type (chromosome, mitochondrion...)
                geneFutures.add(processGene(organism, workbook, geneId.getT1(), geneId.getT2(), path, organismSums));
            }
            ListenableFuture<XSSFWorkbook> workbookFuture = Futures.transform(Futures.successfulAsList(geneFutures), new Function<List<XSSFSheet>, XSSFWorkbook>() {
                @Nullable
                @Override
                public XSSFWorkbook apply(@Nullable List<XSSFSheet> unused) {
                    return workbook;
                }
            }, executorService);

            if (isLastLoop) {
                return workbookFuture;
            } else {
                currentOffset.set(currentOffset.get() + PROCESS_STACK_SIZE);
                return Futures.transformAsync(workbookFuture, kingdom1 -> processGenes(organism, workbook, geneIds, path, organismSums), executorService);
            }
        } else {
            return executorService.submit(() -> workbook);
        }
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
