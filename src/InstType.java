/**
 * Ismail Ataie
 * CDA5155-Spring 2018
 * Assignment 4
 * Speculative Dynamic Scheduled Pipeline Simulator
 * copy all java files, with java extention, into some folder like f1.
 * copy trace.config to that folder, as well.
 * $cd <f1 path>
 * compile code:
 *             $javac *.java
 * execute code:
 *             $ java SpecTomasulo <inputfile >outputfile
 */

//A class for different type of instructions
//All defined instruction are in this class of enumertors

public enum InstType {
    LW,LdotS,SW,SdotS,
    DADD,DSUB,
    BEQ,BNE,
    ADDdotS,SUBdotS,MULdotS,DIVdotS  
}
