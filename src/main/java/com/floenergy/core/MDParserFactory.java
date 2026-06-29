package com.floenergy.core;

public class MDParserFactory {

    private Nem12Parser nem12Parser;

    public MDParserFactory(){
    nem12Parser = new Nem12Parser();
    }

    public MDParser getParser(String type){

        switch (type){
           case "NEM12", "nem12": return  nem12Parser;
            default: throw new RuntimeException("Parser Not Supported " + type );
        }
    }
}
