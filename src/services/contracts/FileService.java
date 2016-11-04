package services.contracts;

import com.google.common.util.concurrent.ListenableFuture;
import models.Gene;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import javax.swing.tree.TreeModel;
import java.util.List;

public interface FileService {
    ListenableFuture<TreeModel> buildTree(String path);

    ListenableFuture<XSSFWorkbook> createWorkbook();

    ListenableFuture<Void> writeWorkbook(Gene gene, XSSFWorkbook workbook, String path, String fileName);

    ListenableFuture<List<Boolean>> createDirectories(List<String> paths) throws InterruptedException;
}
