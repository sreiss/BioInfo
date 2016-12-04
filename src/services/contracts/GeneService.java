package services.contracts;

import com.google.common.util.concurrent.ListenableFuture;
import models.Gene;
import models.Kingdom;
import models.Organism;
import models.Sum;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.HashMap;
import java.util.List;

public interface GeneService {
    ListenableFuture<Gene> processGene(Kingdom kingdom, Organism organism, Tuple<String, String> geneId);
}
