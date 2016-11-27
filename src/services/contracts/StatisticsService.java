package services.contracts;

import com.google.common.util.concurrent.ListenableFuture;
import models.Gene;
import models.Organism;
import models.Sum;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.HashMap;

public interface StatisticsService {
    ListenableFuture<XSSFSheet> computeStatistics(Organism organism, Gene gene, XSSFWorkbook workbook);
    ListenableFuture<XSSFSheet> computeSum(Organism organism, Gene gene, HashMap<String, Sum> organismSums, XSSFSheet sheet);
}
