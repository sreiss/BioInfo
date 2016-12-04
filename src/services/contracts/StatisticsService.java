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

    ListenableFuture<Gene> computeStatistics(Kingdom kingdom, Organism organism, Gene gene);

    ListenableFuture<Sum> computeSum(Kingdom kingdom, Organism organism, Sum sum, Gene gene);

    ListenableFuture<Sum> computeProbabilitiesFromSum(Organism organism, Sum organismSum);

}
