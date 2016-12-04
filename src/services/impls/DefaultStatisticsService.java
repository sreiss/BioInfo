package services.impls;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;
import models.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import services.contracts.FileService;
import services.contracts.StatisticsService;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiConsumer;

public class DefaultStatisticsService implements StatisticsService {
    private final ListeningExecutorService executorService;
    private final FileService fileService;

    @Inject
    public DefaultStatisticsService(ListeningExecutorService listeningExecutorService, FileService fileService) {
        this.executorService = listeningExecutorService;
        this.fileService = fileService;
    }

    private <T extends NucleotidesHolder> ListenableFuture<T> computeTrinucleotidesProbabilities(final T holder) {
        return executorService.submit(() -> {
            if (holder.getTotalTrinucleotide() >  0) {
                Set<String> keys = holder.getTrinuStatPhase0().keySet();
                double tmp1, tmp2 = (double) holder.getTotalTrinucleotide();
                double result;
                for(String key: keys){
                    tmp1 = (double) holder.getTrinuStatPhase0().get(key);
                    result = (tmp1 / tmp2) * 100.0;
                    holder.setTotalProbaTrinu0(holder.getTotalProbaTrinu0() + result);
                    holder.getTrinuProbaPhase0().put(key, result);

                    tmp1 = (double) holder.getTrinuStatPhase1().get(key);
                    result = (tmp1 / tmp2) * 100.0;
                    holder.setTotalProbaTrinu1(holder.getTotalProbaTrinu1() + result);
                    holder.getTrinuProbaPhase1().put(key, result);

                    tmp1 = (double) holder.getTrinuStatPhase2().get(key);
                    result = (tmp1 / tmp2) * 100.0;
                    holder.setTotalProbaTrinu2(holder.getTotalProbaTrinu2() + result);
                    holder.getTrinuProbaPhase2().put(key, result);
                }
            }
            return holder;
        });
    }

    private <T extends NucleotidesHolder> ListenableFuture<T> computeDinucleotideProbabilities(final T holder) {
        return executorService.submit(() -> {
            if (holder.getTotalDinucleotide() > 0) {
                Set<String> keys = holder.getDinuStatPhase0().keySet();
                double tmp1, tmp2 = (double) holder.getTotalDinucleotide();
                double result;
                for(String key: keys){
                    tmp1 = (double) holder.getDinuStatPhase0().get(key);
                    result = (tmp1 / tmp2) * 100.0;
                    holder.setTotalProbaDinu0(holder.getTotalProbaDinu0() + result);
                    holder.getDinuProbaPhase0().put(key, result);

                    tmp1 = (double) holder.getDinuStatPhase1().get(key);
                    result = (tmp1 / tmp2) * 100.0;
                    holder.setTotalProbaDinu1(holder.getTotalProbaDinu1() + result);
                    holder.getDinuProbaPhase1().put(key, result);
                }
            }
            return holder;
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

    @Override
    public ListenableFuture<Gene> computeStatistics(Kingdom kingdom, Organism organism, Gene gene) {
        ListenableFuture<Gene> dinuFuture = computeDinucleotideProbabilities(gene);
        return Futures.transformAsync(dinuFuture, this::computeTrinucleotidesProbabilities, executorService);
    }

    @Override
    public ListenableFuture<Sum> computeSum(Kingdom kingdom, Organism organism, Sum sum, Gene gene) {
        return executorService.submit(() -> {
            sum.setDinuStatPhase0(addHashMaps(sum.getDinuStatPhase0(), gene.getDinuStatPhase0()));
            sum.setDinuStatPhase1(addHashMaps(sum.getDinuStatPhase1(), gene.getDinuStatPhase1()));

            sum.setTrinuStatPhase0(addHashMaps(sum.getTrinuStatPhase0(), gene.getTrinuStatPhase0()));
            sum.setTrinuStatPhase1(addHashMaps(sum.getTrinuStatPhase1(), gene.getTrinuStatPhase1()));
            sum.setTrinuStatPhase2(addHashMaps(sum.getTrinuStatPhase2(), gene.getTrinuStatPhase2()));

            sum.setTotalDinucleotide(sum.getTotalDinucleotide() + gene.getTotalDinucleotide());
            sum.setTotalTrinucleotide(sum.getTotalTrinucleotide() + gene.getTotalTrinucleotide());
            sum.setTotalUnprocessedCds(sum.getTotalUnprocessedCds() + gene.getTotalUnprocessedCds());
            sum.setTotalCds(sum.getTotalCds() + gene.getTotalCds());

            return sum;
        });
    }

    public ListenableFuture<XSSFSheet> computeSum(Organism organism, Gene gene, HashMap<String, Sum> organismSums, XSSFSheet sheet) {
        return executorService.submit(() -> {
            String type = gene.getType();
            Sum sum = organismSums.get(type);

            sum.setDinuStatPhase0(addHashMaps(sum.getDinuStatPhase0(), gene.getDinuStatPhase0()));
            sum.setDinuStatPhase1(addHashMaps(sum.getDinuStatPhase1(), gene.getDinuStatPhase1()));

            sum.setTrinuStatPhase0(addHashMaps(sum.getTrinuStatPhase0(), gene.getTrinuStatPhase0()));
            sum.setTrinuStatPhase1(addHashMaps(sum.getTrinuStatPhase1(), gene.getTrinuStatPhase1()));
            sum.setTrinuStatPhase2(addHashMaps(sum.getTrinuStatPhase2(), gene.getTrinuStatPhase2()));

            sum.setTotalDinucleotide(sum.getTotalDinucleotide() + gene.getTotalDinucleotide());
            sum.setTotalTrinucleotide(sum.getTotalTrinucleotide() + gene.getTotalTrinucleotide());
            sum.setTotalUnprocessedCds(sum.getTotalUnprocessedCds() + gene.getTotalUnprocessedCds());
            sum.setTotalCds(sum.getTotalCds() + gene.getTotalCds());

            return sheet;
        });
    }

    public ListenableFuture<XSSFWorkbook> computeProbabilitiesFromSum(Organism organism, HashMap<String, Sum> organismSums, XSSFWorkbook workbook) {
        List<ListenableFuture<Sum>> computeFutures = new ArrayList<>();
        for (Map.Entry<String, Sum> sum: organismSums.entrySet()) {
            ListenableFuture<Sum> trinuFuture = computeTrinucleotidesProbabilities(sum.getValue());
            ListenableFuture<Sum> dinuFuture = Futures.transformAsync(trinuFuture, this::computeDinucleotideProbabilities, executorService);
            computeFutures.add(dinuFuture);
        }
        return Futures.transform(Futures.successfulAsList(computeFutures), new Function<List<Sum>, XSSFWorkbook>() {
            @Nullable
            @Override
            public XSSFWorkbook apply(@Nullable List<Sum> sums) {
                fileService.fillWorkbookSum(organism, organismSums, workbook);
                return workbook;
            }
        }, executorService);
    }

    @Override
    public ListenableFuture<Sum> computeProbabilitiesFromSum(Organism organism, Sum organismSum) {
        return executorService.submit(() -> {
            computeTrinucleotidesProbabilities(organismSum).get();
            computeDinucleotideProbabilities(organismSum).get();

            return organismSum;
        });
    }

    private LinkedHashMap<String, Integer> addHashMaps(HashMap<String, Integer> hashMap1, HashMap<String, Integer> hashMap2) {
        LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
        result.putAll(hashMap1);
        result.forEach((key, value) -> {
            Integer toAdd = hashMap2.get(key);
            if (toAdd != null) {
                result.put(key, value + toAdd);
            }
        });
        return result;
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
