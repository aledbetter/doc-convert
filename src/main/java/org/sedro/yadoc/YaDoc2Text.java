package org.sedro.yadoc;

import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.sedro.yadoc.parse.YaParser;
import org.sedro.yadoc.parse.YaParserFactory;


/*
 * Command Line
 * Run stand alone
 * DocExtract <text/html> <filename>
 */
public class YaDoc2Text {
    public static void main(String[] args) throws Exception {
    	
        if (args.length != 2 || (args.length > 1 && args[1].startsWith("-"))) {
            System.err.println("usage: <text/html> <filename>");
            System.exit(1);
        }
        
        String infile = args[1].toLowerCase();
        
        // get converter
        YaParser converter = YaParserFactory.getConverterByFileName(infile);
        if (converter == null){
        	System.err.println("File type not supported: " + infile);
        	System.exit(2);          
        }       
        try {
	        if (args[0].equalsIgnoreCase("text")) {
	        	// to text
	            String outfile = infile+".txt";           
	            converter.convertDataText(new FileInputStream(infile), new FileOutputStream(outfile));
	        } else {
	        	// to html
	            String outfile = infile+".html";
	            converter.convertDataHtml(new FileInputStream(infile), new FileOutputStream(outfile));
	        }
        } catch (Exception e) {
            System.err.println("DocExtract "+args[0]+" "+infile+" ERROR: " + e.getMessage());
            e.printStackTrace();
        	System.exit(2);          
        }   
        
    }
}
