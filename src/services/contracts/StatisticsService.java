package services.contracts;

import com.google.common.util.concurrent.ListenableFuture;
import models.Gene;
import models.Organism;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public interface StatisticsService {
    ListenableFuture<XSSFSheet> computeStatistics(Organism organism, Gene gene, XSSFWorkbook workbook);
    ListenableFuture<XSSFWorkbook> computeSum(Organism organism, XSSFWorkbook workbook);
}
