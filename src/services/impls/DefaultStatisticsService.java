package services.impls;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;
import models.Gene;
import models.Organism;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import services.contracts.FileService;
import services.contracts.GeneService;
import services.contracts.StatisticsService;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DefaultStatisticsService implements StatisticsService {
    private final GeneService geneService;
    private final ListeningExecutorService executorService;
    private final FileService fileService;

    @Inject
    public DefaultStatisticsService(GeneService geneService, ListeningExecutorService listeningExecutorService, FileService fileService) {
        this.geneService = geneService;
        this.executorService = listeningExecutorService;
        this.fileService = fileService;
    }

    private ListenableFuture<Gene> computeTrinucleotidesProbabilities(final Gene gene) {
        return executorService.submit(() -> {
            Set<String> keys = gene.getTrinuStatPhase0().keySet();
            double tmp1, tmp2 = (double) gene.getTotalTrinucleotide();
            double result;
            for(String key: keys){
                tmp1 = (double) gene.getTrinuStatPhase0().get(key);
                result = (tmp1 / tmp2) * 100.0;
                gene.setTotalProbaTrinu0(result);
                gene.getTrinuProbaPhase0().put(key, result);

                tmp1 = (double) gene.getTrinuStatPhase1().get(key);
                result = (tmp1 / tmp2) * 100.0;
                gene.setTotalProbaTrinu1(result);
                gene.getTrinuProbaPhase1().put(key, result);

                tmp1 = (double) gene.getTrinuStatPhase2().get(key);
                result = (tmp1 / tmp2) * 100.0;
                gene.setTotalProbaTrinu2(result);
                gene.getTrinuProbaPhase2().put(key, result);
            }
            return gene;
        });
    }

    private ListenableFuture<Gene> computeDinucleotideProbabilities(final Gene gene) {
        return executorService.submit(() -> {
            Set<String> keys = gene.getDinuStatPhase0().keySet();
            double tmp1, tmp2 = (double) gene.getTotalDinucleotide();
            double result;
            for(String key: keys){
                tmp1 = (double) gene.getDinuStatPhase0().get(key);
                result = (tmp1 / tmp2) * 100.0;
                gene.setTotalProbaDinu0(result);
                gene.getDinuProbaPhase0().put(key, result);

                tmp1 = (double) gene.getDinuStatPhase1().get(key);
                result = (tmp1 / tmp2) * 100.0;
                gene.setTotalProbaDinu1(result);
                gene.getDinuProbaPhase1().put(key, result);
            }
            return gene;
        });
    }

    private ListenableFuture<Gene> returnGene(Gene gene) {
        return executorService.submit(() -> gene);
    }

    public ListenableFuture<Gene> computeStatistics(Organism organism, final Gene gene, XSSFWorkbook workbook) {
        List<ListenableFuture<Gene>> computeFutures = new ArrayList<>();
        computeFutures.add(computeTrinucleotidesProbabilities(gene));
        computeFutures.add(computeDinucleotideProbabilities(gene));
        ListenableFuture<XSSFWorkbook> saveToFileFuture = Futures.transformAsync(Futures.allAsList(computeFutures), genes -> fileService.fillWorkbook(organism, gene, workbook), executorService);
        return Futures.transformAsync(saveToFileFuture, modifiedWorkbook -> returnGene(gene), executorService);
    }

//
//    /*
//    public void file() {
//        XSSFCell tmpCell;
//        XSSFRow row;
//
//        if((row = this.CurrentSheet.getRow(0)) == null)
//            row = this.CurrentSheet.createRow(0);
//
//        row.createCell(0).setCellValue("Trinucleotide");
//        row.createCell(1).setCellValue("Nombre Phase 0");
//        row.createCell(2).setCellValue("Proba Phase 0");
//        row.createCell(3).setCellValue("Nombre Phase 1");
//        row.createCell(4).setCellValue("Proba Phase 1");
//        row.createCell(5).setCellValue("Nombre Phase 2");
//        row.createCell(6).setCellValue("Proba Phase 2");
//    }
//
//    public void computeProbabilities(int total, LinkedHashMap<String, Integer> Stat, LinkedHashMap<String, Double> Proba)
//    {
//        Set<String> keys = Stat.keySet();
//        double tmp1, tmp2 = (double) total;
//        for(String key: keys){
//            tmp1 = (double) Stat.get(key);
////            Proba.put(key, Math.round(((tmp1 / tmp2) * 100.0) * 100.0) / 100.0);
//            Proba.put(key, (tmp1 / tmp2) * 100.0);
//        }
//    }
//    */
}
