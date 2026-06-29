package com.floenergy.core;

import com.floenergy.model.NMIDataDetail;
import com.floenergy.model.NMIIntervalRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

public class MeterDataReader {

    private MDParserFactory mdParserFactory;
    private Path outputFilePath;

    public MeterDataReader(){
        mdParserFactory = new MDParserFactory();
        outputFilePath = (Path.of("output.sql"));
    }
    public MeterDataReader(Path outputFilePath){
        mdParserFactory = new MDParserFactory();
        this.outputFilePath = outputFilePath;
    }

    public void read(Path inputFile) {
    try(BufferedReader reader = Files.newBufferedReader(inputFile);
        SqlWriter sqlWriter = new SqlWriter(outputFilePath)
    ){
        String line;
        int lineNumber=1;
        String headerLine = reader.readLine();
        //fetch parser from header
        MDParser mdParser = getParser(getFields(headerLine,lineNumber));
        NMIDataDetail  nmiDataDetail = null;
        while((line = reader.readLine())!=null){
           String[] fields = getFields(line,lineNumber++);
           String recordIndicator = mdParser.extractRecordIndicator(fields);
           try {
               switch (recordIndicator) {
                   case "200":
                       nmiDataDetail = mdParser.parseNMIDataDetail(fields);
                       break;
                   case "300":
                       List<NMIIntervalRecord> nmiIntervalRecordList = mdParser.parseNMIIntervalRecord(nmiDataDetail, fields, lineNumber);
                       sqlWriter.write(nmiIntervalRecordList);
                       break;
                   case "400": // Do Nothing
                   case "500":  // Do Nothing
                       break;
                   case "900":  // end of file reading;
                       return;
                   default:
                       throw new RuntimeException("Unsupported record indicator " + recordIndicator);

               }
           }catch (Exception ex){
               System.out.println("Error occurred at line number " + lineNumber + " with message "+ ex.getMessage());
           }
        }
    } catch (IOException e) {
        throw new RuntimeException("Failed to read input file" + e.getMessage());
    }

    }
    private MDParser getParser(String[] fields){
        String recordType = fields[0];
        if(!"100".equals(recordType)){
            throw new RuntimeException("Invalid Header");
        }
        String nemType = fields[1];
        MDParser mdParser = mdParserFactory.getParser(nemType);
        return mdParser;
    }

    private static String[] getFields(String line, int lineNumber) {
        if(line == null){
            throw new RuntimeException("line is invalid at lineNumber "+lineNumber);
        }
        String[] fields = line.split(",",-1);
        return fields;
    }

}
