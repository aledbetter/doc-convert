package com.extract.processor.parse;

import com.extract.processor.model.Element;
import com.extract.processor.model.Header;
import com.extract.processor.model.SimpleHtml;
import com.extract.processor.render.SimpleHtmlRender;
import com.extract.processor.utils.WordUtils;
import com.extract.processor.parse.Converter;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.apache.poi.POIXMLProperties;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.internal.PackagePropertiesPart;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.xmlbeans.XmlException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@Log4j2
public class Word2Html implements Converter {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d, yyyy hh:mm:ss a z");
    private static final String TYPE = "word";

    @Getter
    @Setter
    private String fileName;
    private int defFontSize;

    @Override
    public void convert(InputStream is, OutputStream os) throws IOException, OpenXML4JException, XmlException {
        XWPFDocument doc = new XWPFDocument(is);

        OPCPackage pkg = doc.getPackage();
        POIXMLProperties props = new POIXMLProperties(pkg);
        PackagePropertiesPart ppropsPart = props.getCoreProperties().getUnderlyingProperties();

        Date created = ppropsPart.getCreatedProperty().getValue();
        Date modified = ppropsPart.getModifiedProperty().getValue();
        log.debug("document name: " + fileName);
        log.debug("document created: " + created);
        log.debug("document modified: " + modified);

        SimpleHtml simpleHtml = new SimpleHtml();
        simpleHtml.setType(TYPE);
        simpleHtml.setName(fileName);

        if (created != null) {
            simpleHtml.setCreated(sdf.format(created));
        }
        if (modified != null) {
            simpleHtml.setCreated(sdf.format(modified));
        }
        simpleHtml.setElementList(new ArrayList<Element>());

        defFontSize = doc.getStyles().getDefaultRunStyle().getFontSize();
        if (defFontSize < 0) defFontSize = 10; // guess
        Iterator<IBodyElement> iterator = doc.getBodyElementsIterator();
        while (iterator.hasNext()) {
            IBodyElement element = iterator.next();
            while (element != null) {
            	element = processElement(doc, iterator, element, simpleHtml);
            }
        }
        
        if (simpleHtml.getElementList().size() > 0) {
            Set<Integer> uniques = new HashSet<Integer>();
        	// correct HX sizes based on found font sizes
        	for (Element e:simpleHtml.getElementList()) {
        		if (!(e instanceof Header)) continue;
        		uniques.add(((Header)e).getFontSize());
        	}
        	Object ul [] = uniques.toArray();
            Arrays.sort(ul);
    		//System.out.println(" SIZES: " + uniques.toString());
    		for (int i=(ul.length-1);i>=0;i--) {
    			int sz = (Integer)ul[i];
    			int level = ul.length-i;
    			if (level > 7) level = 7; // max
    			//System.out.println("    SZ["+i+"][h"+level+"]: " + sz);
    			// update all headers of this size
            	for (Element e:simpleHtml.getElementList()) {
            		if (!(e instanceof Header)) continue;
            		if (((Header)e).getFontSize() == sz) {
            			((Header)e).setLevel(level);
            		}
            	}
    		}


        }
        
        os.write(SimpleHtmlRender.render(simpleHtml).getBytes());
    }
    
 
    private IBodyElement processElement(XWPFDocument doc, Iterator<IBodyElement> iterator, IBodyElement element, SimpleHtml simpleHtml) {
        if (element instanceof XWPFParagraph) {
            XWPFParagraph paragraph = (XWPFParagraph) element;
            if (paragraph.getRuns().size() > 0) {  
                int styleFontSize = WordUtils.getStyleFontSize(doc, paragraph);
                int runFontSize = paragraph.getRuns().get(0).getFontSize();
                if (styleFontSize < 0) styleFontSize = runFontSize;
                /*
                 * if paragraphs have fontsize -1
                 *    then any specified font size over 10 is assumed to be a header
                 */
                              
                if (doc.getNumbering() != null && doc.getNumbering().numExist(paragraph.getNumID())) {
                    log.debug("list was fount");
                    //System.out.println(" LIST["+paragraph.getRuns().size()+"][f:"+styleFontSize+"]["+paragraph.getText()+"]: ");
                    IBodyElement unused = WordUtils.processList(iterator, paragraph, simpleHtml.getElementList());
                    if (unused != null) return unused;
                } else if (WordUtils.isHeader(defFontSize, styleFontSize)) {
                    log.debug("header was found");    
                    //System.out.println(" HDR[f:"+styleFontSize+"/"+runFontSize+"/"+defFontSize+"]["+paragraph.getText()+"]: ");
                    simpleHtml.getElementList().add(WordUtils.processHeader(paragraph, styleFontSize));
                } else {
                    log.debug("paragraph was found");
                    //System.out.println(" PARA[f:"+styleFontSize+"/"+runFontSize+"/"+defFontSize+"]["+paragraph.getText()+"]: ");
                    simpleHtml.getElementList().add(WordUtils.processParagraph(paragraph));
                }
            }
        } else if (element instanceof XWPFTable) {
            log.debug("table was found");
            XWPFTable table = (XWPFTable) element;
            int styleFontSize = WordUtils.getStyleFontSize(doc, table);
            if (WordUtils.isHeader(defFontSize, styleFontSize) && table.getRows().size() == 1) {
                simpleHtml.getElementList().add(WordUtils.processTableHeader(table, styleFontSize));
            } else {
            	simpleHtml.getElementList().add(WordUtils.processTable(table));
            }
        }
        return null;
    }

}