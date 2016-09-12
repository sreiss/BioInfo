package services.impls;

import com.google.inject.Inject;
import models.Gene;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.jdeferred.DeferredManager;
import org.jdeferred.DonePipe;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;
import org.jdeferred.multiple.MasterProgress;
import org.jdeferred.multiple.MultipleResults;
import org.jdeferred.multiple.OneReject;
import services.contracts.GeneService;
import services.contracts.StatisticsService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

public class DefaultStatisticsService implements StatisticsService {
    private final GeneService geneService;
    private final DeferredManager deferredManager;

    @Inject
    public DefaultStatisticsService(GeneService geneService, DeferredManager deferredManager) {
        this.geneService = geneService;
        this.deferredManager = deferredManager;
    }

    private Promise<Gene, Throwable, Void> computeTrinucleotidesProbabilities(final Gene gene) {
        return deferredManager.when(new Callable<Gene>() {
            @Override
            public Gene call() throws Exception {
                Set<String> keys = gene.getTrinuStatPhase0().keySet();
                double tmp1, tmp2 = (double) gene.getTotalTrinucleotide();
                for(String key: keys){
                    tmp1 = (double) gene.getTrinuStatPhase0().get(key);
                    gene.getTrinuProbaPhase0().put(key, (tmp1 / tmp2) * 100.0);

                    tmp1 = (double) gene.getTrinuStatPhase1().get(key);
                    gene.getTrinuProbaPhase1().put(key, (tmp1 / tmp2) * 100.0);

                    tmp1 = (double) gene.getTrinuStatPhase2().get(key);
                    gene.getTrinuProbaPhase2().put(key, (tmp1 / tmp2) * 100.0);
                }
                return gene;
            }
        });
    }

    private Promise<Gene, Throwable, Void> computeDinucleotideProbabilities(final Gene gene) {
        return deferredManager.when(new Callable<Gene>() {
            @Override
            public Gene call() throws Exception {
                Set<String> keys = gene.getDinuStatPhase0().keySet();
                double tmp1, tmp2 = (double) gene.getTotalDinucleotide();
                for(String key: keys){
                    tmp1 = (double) gene.getDinuStatPhase0().get(key);
                    gene.getDinuProbaPhase0().put(key, (tmp1 / tmp2) * 100.0);

                    tmp1 = (double) gene.getDinuStatPhase1().get(key);
                    gene.getDinuProbaPhase1().put(key, (tmp1 / tmp2) * 100.0);
                }
                return gene;
            }
        });
    }

    private Promise<Gene, Throwable, Void> computeTrinucleotidesProbabilities(String sequence, Gene gene) {
        return geneService.extractStatisticsSequenceForTrinucleotides(sequence, gene)
                .then(new DonePipe<Gene, Gene, Throwable, Void>() {
                    @Override
                    public Promise<Gene, Throwable, Void> pipeDone(Gene gene) {
                        return computeTrinucleotidesProbabilities(gene);
                    }
                });
    }

    private Promise<Gene, Throwable, Void> computeDinucleotideProbabilities(String sequence, Gene gene) {
        return geneService.extractStatisticsSequenceForDinucleotides(sequence, gene)
                    .then(new DonePipe<Gene, Gene, Throwable, Void>() {
                        @Override
                        public Promise<Gene, Throwable, Void> pipeDone(Gene gene) {
                            return computeDinucleotideProbabilities(gene);
                        }
                    });
    }

    public Promise<Gene, Throwable, Void> computeStatistics(final List<String> sequences) {
        return geneService.createGene("", 0, 0)
                .then(new DonePipe<Gene, List<Promise<Gene, Throwable, Void>>, Throwable, Void>() {
                    @Override
                    public Promise<List<Promise<Gene, Throwable, Void>>, Throwable, Void> pipeDone(Gene gene) {
                        List<Promise<Gene, Throwable, Void>> promises = new ArrayList<Promise<Gene, Throwable, Void>>();
                        for (String sequence: sequences) {
                            promises.add(computeTrinucleotidesProbabilities(sequence, gene));
                            promises.add(computeDinucleotideProbabilities(sequence, gene));
                        }
                        return new DeferredObject<List<Promise<Gene, Throwable, Void>>, Throwable, Void>().resolve(promises);
                    }
                })
                .then(new DonePipe<List<Promise<Gene,Throwable,Void>>, MultipleResults, OneReject, MasterProgress>() {
                    @Override
                    public Promise<MultipleResults, OneReject, MasterProgress> pipeDone(List<Promise<Gene, Throwable, Void>> promises) {
                        return deferredManager.when(promises.toArray(new Promise[promises.size()]));
                    }
                })
                .then(new DonePipe<MultipleResults, Gene, Throwable, Void>() {
                    @Override
                    public Promise<Gene, Throwable, Void> pipeDone(MultipleResults oneResults) {
                        return new DeferredObject<Gene, Throwable, Void>().resolve(new Gene("", 0, 0));
                    }
                });
    }

    /*
    public void file() {
        XSSFCell tmpCell;
        XSSFRow row;

        if((row = this.CurrentSheet.getRow(0)) == null)
            row = this.CurrentSheet.createRow(0);

        row.createCell(0).setCellValue("Trinucleotide");
        row.createCell(1).setCellValue("Nombre Phase 0");
        row.createCell(2).setCellValue("Proba Phase 0");
        row.createCell(3).setCellValue("Nombre Phase 1");
        row.createCell(4).setCellValue("Proba Phase 1");
        row.createCell(5).setCellValue("Nombre Phase 2");
        row.createCell(6).setCellValue("Proba Phase 2");
    }

    public void computeProbabilities(int total, LinkedHashMap<String, Integer> Stat, LinkedHashMap<String, Double> Proba)
    {
        Set<String> keys = Stat.keySet();
        double tmp1, tmp2 = (double) total;
        for(String key: keys){
            tmp1 = (double) Stat.get(key);
//            Proba.put(key, Math.round(((tmp1 / tmp2) * 100.0) * 100.0) / 100.0);
            Proba.put(key, (tmp1 / tmp2) * 100.0);
        }
    }
    */
}
