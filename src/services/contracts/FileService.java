package services.contracts;

import com.google.common.util.concurrent.ListenableFuture;
import models.Gene;
import models.Organism;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import javax.swing.tree.TreeModel;
import java.io.IOException;
import java.util.List;

public interface FileService {
    ListenableFuture<TreeModel> buildTree(String path);

    XSSFWorkbook createWorkbook();

    XSSFSheet fillWorkbook(Organism organism, Gene gene, XSSFWorkbook workbook);

    void writeWorkbook(XSSFWorkbook workbook, String path, String fileName) throws IOException;

    List<Boolean> createDirectories(List<String> paths);

    XSSFWorkbook fillWorkbookSum(Organism organism, XSSFWorkbook workbook);
}
