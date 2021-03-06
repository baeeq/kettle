package org.pentaho.di.trans.steps.excelinput.ods;

import java.util.List;

import org.odftoolkit.odfdom.doc.OdfDocument;
import org.odftoolkit.odfdom.doc.OdfSpreadsheetDocument;
import org.odftoolkit.odfdom.doc.table.OdfTable;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.spreadsheet.KSheet;
import org.pentaho.di.core.spreadsheet.KWorkbook;
import org.pentaho.di.core.vfs.KettleVFS;

public class OdfWorkbook implements KWorkbook {

  private String filename;
  private String encoding;
  private OdfDocument document;
  
  public OdfWorkbook(String filename, String encoding) throws KettleException {
    this.filename = filename;
    this.encoding = encoding;
    
    try {
      document = OdfSpreadsheetDocument.loadDocument(KettleVFS.getInputStream(filename));
    } catch(Exception e) {
      throw new KettleException(e);
    }
  }
  
  public void close() {
    // not needed here
  }
  
  @Override
  public KSheet getSheet(String sheetName) {
    OdfTable table = document.getTableByName(sheetName);
    if (table==null) return null;
    return new OdfSheet(table);
  }
  
  public String[] getSheetNames() {
    List<OdfTable> list = document.getTableList();
    int nrSheets = list.size();
    String[] names = new String[nrSheets];
    for (int i=0;i<nrSheets;i++) {
      names[i] = list.get(i).getTableName();
    }
    return names;
  }
  
  public String getFilename() {
    return filename;
  }
  
  public String getEncoding() {
    return encoding;
  }
  
  public int getNumberOfSheets() {
    return document.getTableList().size();
  }
  
  public KSheet getSheet(int sheetNr) {
    OdfTable table = document.getTableList().get(sheetNr);
    if (table==null) return null;
    return new OdfSheet(table);
  }
}
