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

//class for definition of functional units
//Each functional unit has a value in this enumeration class
public enum FunctionalUnitType {
    Eff_Addr,
    FP_Adder,
    FP_Mult,
    Int    
}
