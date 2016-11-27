package services.impls;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;
import models.Gene;
import models.Organism;
import models.Sum;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import services.contracts.FileService;
import services.contracts.StatisticsService;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DefaultStatisticsService implements StatisticsService {
    private final ListeningExecutorService executorService;
    private final FileService fileService;

    @Inject
    public DefaultStatisticsService(ListeningExecutorService listeningExecutorService, FileService fileService) {
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
                gene.setTotalProbaTrinu0(gene.getTotalProbaTrinu0() + result);
                gene.getTrinuProbaPhase0().put(key, result);

                tmp1 = (double) gene.getTrinuStatPhase1().get(key);
                result = (tmp1 / tmp2) * 100.0;
                gene.setTotalProbaTrinu1(gene.getTotalProbaTrinu1() + result);
                gene.getTrinuProbaPhase1().put(key, result);

                tmp1 = (double) gene.getTrinuStatPhase2().get(key);
                result = (tmp1 / tmp2) * 100.0;
                gene.setTotalProbaTrinu2(gene.getTotalProbaTrinu2() + result);
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
                gene.setTotalProbaDinu0(gene.getTotalProbaDinu0() + result);
                gene.getDinuProbaPhase0().put(key, result);

                tmp1 = (double) gene.getDinuStatPhase1().get(key);
                result = (tmp1 / tmp2) * 100.0;
                gene.setTotalProbaDinu1(gene.getTotalProbaDinu1() + result);
                gene.getDinuProbaPhase1().put(key, result);
            }
            return gene;
        });
    }

    public ListenableFuture<XSSFSheet> computeStatistics(Organism organism, Gene gene, XSSFWorkbook workbook) {
        ListenableFuture<Gene> dinuFuture = computeDinucleotideProbabilities(gene);
        ListenableFuture<Gene> trinuFuture = Futures.transformAsync(dinuFuture, this::computeTrinucleotidesProbabilities, executorService);
        return Futures.transform(trinuFuture, new Function<Gene, XSSFSheet>() {
            @Nullable
            @Override
            public XSSFSheet apply(@Nullable Gene computedGene) {
                return fileService.fillWorkbook(organism, computedGene, workbook);
            }
        }, executorService);
    }

    public ListenableFuture<XSSFSheet> computeSum(Organism organism, Gene gene, HashMap<String, Sum> organismSums, XSSFSheet sheet) {
        return executorService.submit(() -> {
            String type = gene.getType();
            organismSums.putIfAbsent(type, new Sum(type, "", 0, 0));
            Sum sum = organismSums.get(type);

            sum.setTotalDinucleotide(sum.getTotalDinucleotide() + gene.getTotalDinucleotide());
            sum.setTotalTrinucleotide(sum.getTotalTrinucleotide() + gene.getTotalTrinucleotide());
            sum.setTotalUnprocessedCds(sum.getTotalUnprocessedCds() + gene.getTotalUnprocessedCds());
            sum.setTotalCds(sum.getTotalCds() + gene.getTotalCds());

            return sheet;
        });
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
