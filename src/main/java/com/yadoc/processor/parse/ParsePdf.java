package main.java.com.yadoc.processor.parse;

import main.java.com.yadoc.processor.model.MDocument;
import main.java.com.yadoc.processor.utils.PdfCustomrStripper;
import main.java.com.yadoc.processor.utils.SimpleHtmlUtils;

import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;

//FIXME will this Stripper override the other? 

public class ParsePdf implements YaParser {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d, yyyy hh:mm:ss a z");
    private static final String TYPE = "pdf";

    private String fileName;

    public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {	
		
		this.fileName = fileName;
	}

	@Override
    public MDocument parseData(InputStream is, OutputStream os) throws Exception {
		
        PDDocument document = PDDocument.load(is);
        StringWriter stringWriter = new StringWriter();
        PdfCustomrStripper stripper = new PdfCustomrStripper();
        stripper.writeText(document, stringWriter);
     
        
        Calendar creation = document.getDocumentInformation().getCreationDate();
        Calendar modification = document.getDocumentInformation().getModificationDate();
       // document.getDocumentInformation().getCreator()
        //log.debug("document name: " + fileName);
        //log.debug("document creation: " + creation);
        //log.debug("document modification: " + modification);

        MDocument simpleHtml = stripper.getSimpleHtml();
        simpleHtml.setType(TYPE);
        simpleHtml.setName(fileName);
        if (creation != null) {
            simpleHtml.setCreated(sdf.format(creation.getTime()));
        }
        if (modification != null) {
            simpleHtml.setModified(sdf.format(modification.getTime()));
        }
        SimpleHtmlUtils.clearSimpleHtml(simpleHtml);
        SimpleHtmlUtils.optimizeSimpleHtml(simpleHtml);

        return simpleHtml;
        
		//return null;
    }
}