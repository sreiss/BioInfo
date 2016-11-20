package services.contracts;

import com.google.common.util.concurrent.ListenableFuture;
import models.Organism;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.List;

public interface GeneService {
    String generateUrlForGene(String id);
//    Promise<Gene, Throwable, Object> createGene(String name, int totalDinucleotides, int totalTrinucleotides);
//    Promise<Gene, Throwable, Void> extractStatisticsSequenceForTrinucleotides(String sequence, Gene gene);
//    Promise<Gene, Throwable, Void> extractStatisticsSequenceForDinucleotides(String sequence, Gene gene);
//    Promise<List<Gene>, Throwable, Object> processGenes(List<String> geneIds);
    ListenableFuture<XSSFWorkbook> processGenes(Organism organism, XSSFWorkbook workbook, List<Tuple<String, String>> geneIds, String path);
}
