package services.contracts;

import com.google.common.util.concurrent.ListenableFuture;
import models.Gene;
import models.Kingdom;
import models.Organism;
import models.Sum;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.HashMap;

public interface StatisticsService {
    ListenableFuture<XSSFSheet> computeStatistics(Organism organism, Gene gene, XSSFWorkbook workbook);

    ListenableFuture<Gene> computeStatistics(Kingdom kingdom, Organism organism, Gene gene);

    ListenableFuture<Sum> computeSum(Kingdom kingdom, Organism organism, Sum sum, Gene gene);

    ListenableFuture<XSSFSheet> computeSum(Organism organism, Gene gene, HashMap<String, Sum> organismSums, XSSFSheet sheet);
    ListenableFuture<XSSFWorkbook> computeProbabilitiesFromSum(Organism organism, HashMap<String, Sum> organismSums, XSSFWorkbook workbook);

    ListenableFuture<Sum> computeProbabilitiesFromSum(Organism organism, Sum organismSum);
}
