package services.contracts;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jdeferred.Promise;
import org.jdeferred.multiple.MasterProgress;
import org.jdeferred.multiple.MultipleResults;
import org.jdeferred.multiple.OneReject;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import java.io.File;
import java.util.List;

public interface FileService {
    Promise<TreeModel, Throwable, Object> buildTree(String path);
    Promise<List<File>, Throwable, Object> readDirectory(String path);
    Promise<List<Boolean>, Throwable, Object> createDirectories(List<String> paths);
    Promise<Boolean, Throwable, Void> createDirectory(String path);
    Promise<XSSFWorkbook, Throwable, Void> createWorkbook();
    Promise<Void, Throwable, Object> writeWorkbook(XSSFWorkbook workbook, String path, String fileName);
}
