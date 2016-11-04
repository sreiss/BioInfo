package services.contracts;

import com.google.common.util.concurrent.ListenableFuture;
import models.Gene;
import models.Organism;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import javax.swing.tree.TreeModel;
import java.util.List;

public interface FileService {
    ListenableFuture<TreeModel> buildTree(String path);

    ListenableFuture<XSSFWorkbook> createWorkbook();

    ListenableFuture<XSSFWorkbook> fillWorkbook(Organism organism, Gene gene, XSSFWorkbook workbook);

    ListenableFuture<Void> writeWorkbook(XSSFWorkbook workbook, String path, String fileName);

    ListenableFuture<List<Boolean>> createDirectories(List<String> paths) throws InterruptedException;
}
