package services.impls;

import models.Gene;
import services.contracts.StatisticsService;

import java.util.LinkedHashMap;
import java.util.Set;

public class DefaultStatisticsService implements StatisticsService {

    private void computeStatsSequenceForTrinucleotides(String sequence, Gene gene) {

    }

    public static class TrinuStatRunnable implements Runnable
    {
        private String seq;
        private Gene gen;

        public TrinuStatRunnable(String s, Gene g)
        {
            this.seq = s;
            this.gen = g;
        }

        @Override
        public void run()
        {
            this.gen = Cds.statsSequenceTrinucleo(this.seq, this.gen);
            this.gen.calculProbaTrinu();
        }
    }

    public static class TrinuProbaRunnable implements Runnable
    {
        private Gene gen;

        public TrinuProbaRunnable(Gene g)
        {
            this.gen = g;
        }

        @Override
        public void run()
        {
            gen.calculProbaTrinu();
        }
    }

    public static class DinuStatRunnable implements Runnable
    {
        private String seq;
        private Gene gen;

        public DinuStatRunnable(String s, Gene g)
        {
            this.seq = s;
            this.gen = g;
        }

        @Override
        public void run()
        {
            this.gen = Cds.statsSequenceDinucleo(this.seq, this.gen);
            this.gen.calculProbaDinu();
        }
    }

    public static class DinuProbaRunnable implements Runnable
    {
        private Gene gen;

        public DinuProbaRunnable(Gene g)
        {
            this.gen = g;
        }

        @Override
        public void run()
        {
            gen.calculProbaDinu();
        }
    }

    public void computeStatistics(Gene gene) {
        trinu = new Thread(new TrinuDinuRunnable.TrinuStatRunnable(sequence,g));
        dinu = new Thread(new TrinuDinuRunnable.DinuStatRunnable(sequence,g));

        threadList.add(trinu);
        threadList.add(dinu);

        executorService.submit(trinu);
        executorService.submit(dinu);

        for (Thread thread : threadList) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

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

    public void calculProbaTrinu()
    {
        Set<String> keys = this.trinuStatPhase0.keySet();
        double tmp1, tmp2 = (double) this.totalTrinucleotide;
        for(String key: keys){
            tmp1 = (double) this.trinuStatPhase0.get(key);
//        	this.trinuProbaPhase0.put(key, Math.round(((tmp1 / tmp2) * 100.0) * 100.0) / 100.0);
            this.trinuProbaPhase0.put(key, (tmp1 / tmp2) * 100.0);

            tmp1 = (double) this.trinuStatPhase1.get(key);
//        	this.trinuProbaPhase1.put(key, Math.round(((tmp1 / tmp2) * 100.0) * 100.0) / 100.0);
            this.trinuProbaPhase1.put(key, (tmp1 / tmp2) * 100.0);

            tmp1 = (double) this.trinuStatPhase2.get(key);
//        	this.trinuProbaPhase2.put(key, Math.round(((tmp1 / tmp2) * 100.0) * 100.0) / 100.0);
            this.trinuProbaPhase2.put(key, (tmp1 / tmp2) * 100.0);
        }
    }

    public void calculProbaDinu()
    {
        Set<String> keys = this.dinuStatPhase0.keySet();
        double tmp1, tmp2 = (double) this.totalDinucleotide;
        for(String key: keys){
            tmp1 = (double) this.dinuStatPhase0.get(key);
//            this.dinuProbaPhase0.put(key, Math.round(((tmp1 / tmp2) * 100.0) * 100.0) / 100.0);
            this.dinuProbaPhase0.put(key, (tmp1 / tmp2) * 100.0);

            tmp1 = (double) this.dinuStatPhase1.get(key);
//            this.dinuProbaPhase1.put(key, Math.round(((tmp1 / tmp2) * 100.0) * 100.0) / 100.0);
            this.dinuProbaPhase1.put(key, (tmp1 / tmp2) * 100.0);
        }
    }
}
