package services.impls;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;
import models.Gene;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import services.contracts.FileService;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

public class DefaultFileService implements FileService {
    private final ListeningExecutorService executorService;

    @Inject
    public DefaultFileService(ListeningExecutorService listeningExecutorService) {
        this.executorService = listeningExecutorService;
    }

    private DefaultMutableTreeNode buildTreeRoot(DefaultMutableTreeNode root, File file) {
        if (root == null) {
            root = new DefaultMutableTreeNode(file.getName());
        }
        DefaultMutableTreeNode children = new DefaultMutableTreeNode(file.getName());
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (file.isDirectory()) {
                        root.add(buildTreeRoot(children, f));
                    } else {
                        root.add(children);
                    }
                }
            }
        }
        return root;
    }

    @Override
    public ListenableFuture<TreeModel> buildTree(String path) {
        return executorService.submit(() -> {
            DefaultMutableTreeNode root = buildTreeRoot(new DefaultMutableTreeNode(), new File(path));
            return new DefaultTreeModel(root);
        });
    }

    @Override
    public ListenableFuture<XSSFWorkbook> createWorkbook() {
        return executorService.submit((Callable<XSSFWorkbook>) XSSFWorkbook::new);
    }

    @Override
    public ListenableFuture<Void> writeWorkbook(Gene gene, final XSSFWorkbook workbook, final String path, final String fileName) {
        ListenableFuture<XSSFSheet> createSheetFuture = executorService.submit((Callable<XSSFSheet>) workbook::createSheet);
        ListenableFuture<XSSFSheet> fillDinuFuture = Futures.transformAsync(createSheetFuture, sheet -> fillFileDinu(gene, workbook, sheet), executorService);
        ListenableFuture<XSSFSheet> fillTrinuFuture = Futures.transformAsync(fillDinuFuture, sheet -> fillFileTrinu(gene, workbook, sheet), executorService);
        return Futures.transformAsync(fillTrinuFuture, sheet -> {
            FileOutputStream stream = new FileOutputStream(path + fileName + ".xlsx");
            workbook.write(stream);
            stream.close();
            workbook.close();
            return null;
        });
    }


//    @Override
//    public Promise<List<File>, Throwable, Object> readDirectory(final String path) {
//        return deferredManager.when(new DeferredCallable<List<File>, Object>() {
//            @Override
//            public List<File> call() throws Exception {
//                List<File> files = null;
//                File file = new File(path);
//                if (file.isDirectory()) {
//                    File[] filesArray = file.listFiles();
//                    if (filesArray != null) {
//                        files = new ArrayList<File>(Arrays.asList(filesArray));
//                    } else {
//                        throw new FileNotFoundException(path);
//                    }
//                } else {
//                    throw new NotDirectoryException(path);
//                }
//                return files;
//            }
//        });
//    }

    @Override
    public ListenableFuture<List<Boolean>> createDirectories(final List<String> paths) throws InterruptedException {
        List<ListenableFuture<Boolean>> createDirectoryFutures = new ArrayList<>();
        for (String path: paths) {
            createDirectoryFutures.add(createDirectory(path));
        }
        return Futures.allAsList(createDirectoryFutures);
    }

    private ListenableFuture<Boolean> createDirectory(final String path) {
        return executorService.submit(() -> new File(path).mkdirs());
    }

    private CellStyle buildCellStyleForProba(Workbook workbook) {
        CellStyle probaStyle = workbook.createCellStyle();
        probaStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("0.00"));
        return probaStyle;
    }

    private CellStyle buildCellStyleForNumber(Workbook workbook) {
        CellStyle numberStyle = workbook.createCellStyle();
        numberStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("0"));
        return numberStyle;
    }

    private ListenableFuture<XSSFSheet> fillFileTrinu(Gene g, XSSFWorkbook workbook, XSSFSheet sheet) {
        return executorService.submit(() -> {
            CellStyle numberStyle = buildCellStyleForNumber(workbook);
            CellStyle probaStyle = buildCellStyleForProba(workbook);
            XSSFCell tmpCell;
            XSSFRow row;

            if ((row = sheet.getRow(0)) == null)
                row = sheet.createRow(0);

            row.createCell(0).setCellValue("Trinucleotide");
            row.createCell(1).setCellValue("Nombre Phase 0");
            row.createCell(2).setCellValue("Proba Phase 0");
            row.createCell(3).setCellValue("Nombre Phase 1");
            row.createCell(4).setCellValue("Proba Phase 1");
            row.createCell(5).setCellValue("Nombre Phase 2");
            row.createCell(6).setCellValue("Proba Phase 2");

            int i = 1;
            Set<String> keys = g.getTrinuStatPhase0().keySet();

            for (String key : keys) {
                if ((row = sheet.getRow(i)) == null)
                    row = sheet.createRow(i);

                // Set Trinicludotide
                row.createCell(0).setCellValue(key);

                // NB phase 0
                tmpCell = row.createCell(1);
                tmpCell.setCellValue(g.getTrinuStatPhase0().get(key));
                tmpCell.setCellType(CellType.NUMERIC);
                tmpCell.setCellStyle(numberStyle);
                //tmp0 += g.trinuStatPhase0.get(key);

                // Proba phase 0
                tmpCell = row.createCell(2);
                tmpCell.setCellValue(g.getDinuProbaPhase0().get(key));
                tmpCell.setCellType(CellType.NUMERIC);
                tmpCell.setCellStyle(probaStyle);

                // NB phase 1
                tmpCell = row.createCell(3);
                tmpCell.setCellValue(g.getTrinuStatPhase1().get(key));
                tmpCell.setCellType(CellType.NUMERIC);
                tmpCell.setCellStyle(numberStyle);
                //tmp1 += g.trinuStatPhase1.get(key);

                // Proba phase 1
                tmpCell = row.createCell(4);
                tmpCell.setCellValue(g.getTrinuProbaPhase1().get(key));
                tmpCell.setCellType(CellType.NUMERIC);
                tmpCell.setCellStyle(probaStyle);
                //tmpCell.setCellType(XSSFCell.CELL_TYPE_NUMERIC);

                // NB phase 2
                tmpCell = row.createCell(5);
                tmpCell.setCellValue(g.getTrinuStatPhase2().get(key));
                tmpCell.setCellType(CellType.NUMERIC);
                tmpCell.setCellStyle(numberStyle);
                //tmp2 += g.trinuStatPhase2.get(key);

                // Proba phase 2
                tmpCell = row.createCell(6);
                tmpCell.setCellValue(g.getTrinuStatPhase2().get(key));
                tmpCell.setCellType(CellType.NUMERIC);
                tmpCell.setCellStyle(probaStyle);

                i++;
            }

            row = sheet.createRow(i);
            row.createCell(0).setCellValue("Total");

            tmpCell = row.createCell(1);
            tmpCell.setCellValue(g.getTotalTrinucleotide());
            tmpCell.setCellType(CellType.NUMERIC);

            tmpCell = row.createCell(3);
            tmpCell.setCellValue(g.getTotalTrinucleotide());
            tmpCell.setCellType(CellType.NUMERIC);

            tmpCell = row.createCell(5);
            tmpCell.setCellValue(g.getTotalTrinucleotide());
            tmpCell.setCellType(CellType.NUMERIC);

            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            sheet.autoSizeColumn(2);
            sheet.autoSizeColumn(3);
            sheet.autoSizeColumn(4);
            sheet.autoSizeColumn(5);
            sheet.autoSizeColumn(6);

            return sheet;
        });
    }

    private ListenableFuture<XSSFSheet> fillFileDinu(Gene g, XSSFWorkbook workbook, XSSFSheet sheet) {
        return executorService.submit(() -> {
            CellStyle numberStyle = buildCellStyleForNumber(workbook);
            CellStyle probaStyle = buildCellStyleForProba(workbook);
            XSSFCell tmpCell;
            XSSFRow row;

            if((row = sheet.getRow(0)) == null )
                row = sheet.createRow(0);

            row.createCell(8).setCellValue("Dinucleotide");
            row.createCell(9).setCellValue("Nombre Phase 0");
            row.createCell(10).setCellValue("Proba Phase 0");
            row.createCell(11).setCellValue("Nombre Phase 1");
            row.createCell(12).setCellValue("Proba Phase 1");

            int i = 1;
            Set<String> keys = g.getDinuProbaPhase0().keySet();

            for(String key: keys)
            {
                if((row = sheet.getRow(i)) == null)
                    row = sheet.createRow(i);

                // Set Dinicludotide
                row.createCell(8).setCellValue(key);

                // NB phase 0
                tmpCell = row.createCell(9);
                tmpCell.setCellValue(g.getDinuStatPhase0().get(key));
                tmpCell.setCellType(CellType.NUMERIC);
                tmpCell.setCellStyle(numberStyle);

                // Proba phase 0
                tmpCell = row.createCell(10);
                tmpCell.setCellValue(g.getDinuProbaPhase0().get(key));
                tmpCell.setCellType(CellType.NUMERIC);
                tmpCell.setCellStyle(probaStyle);

                // NB phase 1
                tmpCell = row.createCell(11);
                tmpCell.setCellValue(g.getDinuStatPhase1().get(key));
                tmpCell.setCellType(CellType.NUMERIC);
                tmpCell.setCellStyle(numberStyle);

                // Proba phase 1
                tmpCell = row.createCell(12);
                tmpCell.setCellValue(g.getDinuProbaPhase1().get(key));
                tmpCell.setCellType(CellType.NUMERIC);
                tmpCell.setCellStyle(probaStyle);

                i++;
            }

            if((row = sheet.getRow(i)) == null )
                row = sheet.createRow(i);

            row.createCell(8).setCellValue("Total");

            tmpCell = row.createCell(9);
            tmpCell.setCellValue(g.getTotalDinucleotide());
            tmpCell.setCellType(CellType.NUMERIC);

            tmpCell = row.createCell(11);
            tmpCell.setCellValue(g.getTotalTrinucleotide());
            tmpCell.setCellType(CellType.NUMERIC);

            sheet.autoSizeColumn(8);
            sheet.autoSizeColumn(9);
            sheet.autoSizeColumn(10);
            sheet.autoSizeColumn(11);
            sheet.autoSizeColumn(12);

            return sheet;
        });
    }

//    @Override
//    public Promise<XSSFWorkbook, Throwable, Void> createWorkbook() {
//        return deferredManager.when(new Callable<XSSFWorkbook>() {
//            @Override
//            public XSSFWorkbook call() throws Exception {
//                return new XSSFWorkbook();
//            }
//        });
//    }
//
//    @Override
//    public Promise<Void, Throwable, Object> writeWorkbook(final XSSFWorkbook workbook, final String path, final String fileName) {
//        return deferredManager.when(new DeferredCallable<Void, Object>() {
//            @Override
//            public Void call() throws Exception {
//                FileOutputStream stream = new FileOutputStream(path + fileName);
//                workbook.write(stream);
//                stream.close();
//                workbook.close();
//                return null;
//            }
//        });
//    }

}
