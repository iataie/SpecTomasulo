
/**
 * Ismail Ataie
 * CDA5155-Spring 2018
 * Assignment 4
 * Speculative Dynamic Scheduled Pipeline Simulator
 * copy all java files, with java extention, into some folder like f1.
 * copy trace.config to that folder, as well.
 * $cd <f1 path>
 * compile code: $javac *.java 
 * execute code: $ java SpecTomasulo <inputfile >outputfile 
 */
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.*;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

//main class that contains main method for doing simualtion
public class SpecTomasulo {

    //main mathod of main class for parsing instructions and starting simulation
    //reading config file and trace file and creating a class named processor to
    //create stracture of simluated processor.main loop of simulator calling 
    //methods of commit,writeback,execute,issue 
    //to simulate speculative Tomasulo algorithm.
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        Processor pro;
        parser parser1 = new parser();

        configurator cfg = new configurator();
        try {
            cfg.readConfigFile();

        } catch (IOException ex) {
            Logger.getLogger(SpecTomasulo.class.getName()).log(Level.SEVERE,
                    null, ex);
        }

        // create processor to start simulation
        pro = new Processor(cfg.eff_Addr_Buff, cfg.fp_Adds_Buff,
                cfg.fp_muls_Buff,
                cfg.ints_Buff, cfg.fp_add_Lat, cfg.fp_sub_Lat, cfg.fp_mul_Lat,
                cfg.fp_div_Lat, cfg.reorder_Buff, 500);

        Scanner input = new Scanner(System.in);
        while (input.hasNextLine()) {

            String instruction = input.nextLine();
            if (instruction.compareTo("") != 0) {
                if (parser1.parse(instruction)) {
                    pro.inserInst(parser1.operation, parser1.source1,
                            parser1.source2, parser1.dest, parser1.memAddress,
                            parser1.memLabel, instruction);
                } else {
                    System.out.println("Error in parsing instruction");
                }
            }
        }

        pro.clock = 0;

        boolean cnt = true;
        while (cnt) {//main loop of simulator
            pro.clock++;
            cnt = pro.doCommitStage();
            pro.doWritebackStage();
            pro.doExecuteStage();
            pro.doIssueStage();
        }
        cfg.printConfigParamValues();

        pro.IQ.print();

        System.out.println("\n\nDelays");
        System.out.println("------");
        System.out.println("reorder buffer delays: " + pro.robDelay);
        System.out.println("reservation station delays: " + pro.rsvDelay);
        System.out.println("data memory conflict delays: "
                + pro.memBusyWaitCycles);
        int trueDepDelay = pro.IQ.trueDepDelay() + pro.RAWDealy;

        System.out.println("true dependence delays: " + trueDepDelay);

    }

}

//A class for parsing instructions
//returns in its properties the instruction which is parsed 
class parser {

    InstType operation;
    Register source1;
    Register source2;
    Register dest;
    int memAddress;
    String memLabel;

    boolean parse(String strCmnd) {
        if (isLW(strCmnd) || isLdotS(strCmnd) || isSW(strCmnd) || isSdotS(
                strCmnd)
                || isDADD(strCmnd) || isDSUB(strCmnd) || isBEQ(strCmnd)
                || isBNE(strCmnd) || isADDdotS(strCmnd) || isSUBdotS(strCmnd)
                || isMULdotS(strCmnd) || isDIVdotS(strCmnd)) {
            return true;
        }
        return false;

    }

    //method for parsing LW instructions
    //accept a parameter as a string to parse it
    boolean isLW(String strCmnd) {
        Pattern p1 = Pattern.compile(
                "(^LW)\\s+(R\\d+)\\s*,\\s*[\\d]+\\s*\\((R\\d+)\\)\\s*:(\\d+)");
        Matcher matcherLW = p1.matcher(strCmnd);
        if (matcherLW.find()) {
            operation = InstType.LW;
            dest = Register.valueOf(matcherLW.group(2));
            source1 = Register.valueOf(matcherLW.group(3));
            memAddress = Integer.valueOf(matcherLW.group(4)).intValue();
            return true;
        }

        return false;
    }

    //method for parsing L.S instructions
    //accept a parameter as a string to parse it
    boolean isLdotS(String strCmnd) {
        Pattern p1 = Pattern.compile(
              "(^L\\.S)\\s+(F\\d+)\\s*,\\s*[\\d]+\\s*\\((R\\d+)\\)\\s*:(\\d+)");
        Matcher matcherLW = p1.matcher(strCmnd);
        if (matcherLW.find()) {
            operation = InstType.LdotS;
            dest = Register.valueOf(matcherLW.group(2));
            source1 = Register.valueOf(matcherLW.group(3));
            memAddress = Integer.valueOf(matcherLW.group(4)).intValue();
            return true;
        }

        return false;
    }

    //method for parsing SW instructions
    //accept a parameter as a string to parse it
    boolean isSW(String strCmnd) {
        Pattern p1 = Pattern.compile(
                "(^SW)\\s+(R\\d+)\\s*,\\s*[\\d]+\\s*\\((R\\d+)\\)\\s*:(\\d+)");
        Matcher matcherSW = p1.matcher(strCmnd);
        if (matcherSW.find()) {
            operation = InstType.SW;
            source1 = Register.valueOf(matcherSW.group(3));
            //dest = Register.valueOf(matcherSW.group(3));
            memAddress = Integer.valueOf(matcherSW.group(4)).intValue();
            return true;
        }

        return false;
    }

    //method for parsing S.S instructions
    //accept a parameter as string to parse it
    boolean isSdotS(String strCmnd) {
        Pattern p1 = Pattern.compile(
              "(^S\\.S)\\s+(F\\d+)\\s*,\\s*[\\d]+\\s*\\((R\\d+)\\)\\s*:(\\d+)");
        Matcher matcherLW = p1.matcher(strCmnd);
        if (matcherLW.find()) {
            operation = InstType.SdotS;
            source1 = Register.valueOf(matcherLW.group(3));
            //dest = Register.valueOf(matcherLW.group(3));
            memAddress = Integer.valueOf(matcherLW.group(4)).intValue();
            return true;
        }

        return false;
    }

    //method for parsing DADD instructions
    //accept a parameter as string to parse it
    boolean isDADD(String strCmnd) {
        Pattern p1 = Pattern.compile(
                "(^DADD)\\s+(R\\d+)\\s*,\\s*(R\\d+)\\s*,\\s*(R\\d+)");
        Matcher matcherLW = p1.matcher(strCmnd);
        if (matcherLW.find()) {
            operation = InstType.DADD;
            dest = Register.valueOf(matcherLW.group(2));
            source1 = Register.valueOf(matcherLW.group(3));
            source2 = Register.valueOf(matcherLW.group(4));
            return true;
        }

        return false;
    }

    //method for parsing DSUB instructions
    //accept a parameter as string to parse it
    boolean isDSUB(String strCmnd) {
        Pattern p1 = Pattern.compile(
                "(^DSUB)\\s+(R\\d+)\\s*,\\s*(R\\d+)\\s*,\\s*(R\\d+)");
        Matcher matcherLW = p1.matcher(strCmnd);
        if (matcherLW.find()) {
            operation = InstType.DSUB;
            dest = Register.valueOf(matcherLW.group(2));
            source1 = Register.valueOf(matcherLW.group(3));
            source2 = Register.valueOf(matcherLW.group(4));
            return true;
        }

        return false;
    }

    //method for parsing BEQ instructions
    //accept a parameter as string to parse it
    boolean isBEQ(String strCmnd) {
        Pattern p1 = Pattern.compile(
                "(^BEQ)\\s+(R\\d+)\\s*,\\s*(R\\d+)\\s*,\\s*(\\w+)");
        Matcher matcherLW = p1.matcher(strCmnd);
        if (matcherLW.find()) {
            operation = InstType.BEQ;

            source1 = Register.valueOf(matcherLW.group(2));
            source2 = Register.valueOf(matcherLW.group(3));
            memLabel = matcherLW.group(4);

            return true;
        }

        return false;
    }

    //method for parsing BNE instructions
    //accept a parameter as string to parse it
    boolean isBNE(String strCmnd) {
        Pattern p1 = Pattern.compile(
                "(^BNE)\\s+(R\\d+)\\s*,\\s*(R\\d+)\\s*,\\s*(\\w+)");
        Matcher matcherLW = p1.matcher(strCmnd);
        if (matcherLW.find()) {
            operation = InstType.BNE;

            source1 = Register.valueOf(matcherLW.group(2));
            source2 = Register.valueOf(matcherLW.group(3));
            memLabel = matcherLW.group(4);

            return true;
        }

        return false;
    }

    //method for parsing ADD.S instructions
    //accept a parameter as string to parse it
    boolean isADDdotS(String strCmnd) {
        Pattern p1 = Pattern.compile(
                "(^ADD.S)\\s+(F\\d+)\\s*,\\s*(F\\d+)\\s*,\\s*(F\\d+)");
        Matcher matcherLW = p1.matcher(strCmnd);
        if (matcherLW.find()) {
            operation = InstType.ADDdotS;
            dest = Register.valueOf(matcherLW.group(2));
            source1 = Register.valueOf(matcherLW.group(3));
            source2 = Register.valueOf(matcherLW.group(4));
            return true;
        }

        return false;
    }

    //method for parsing SUB.S instructions
    //accept a parameter as string to parse it
    boolean isSUBdotS(String strCmnd) {
        Pattern p1 = Pattern.compile(
                "(^SUB.S)\\s+(F\\d+)\\s*,\\s*(F\\d+)\\s*,\\s*(F\\d+)");
        Matcher matcherLW = p1.matcher(strCmnd);
        if (matcherLW.find()) {
            operation = InstType.SUBdotS;
            dest = Register.valueOf(matcherLW.group(2));
            source1 = Register.valueOf(matcherLW.group(3));
            source2 = Register.valueOf(matcherLW.group(4));
            return true;
        }

        return false;
    }

    //method for parsing MUL.S instruction
    //accept a parameter as string to parse it
    boolean isMULdotS(String strCmnd) {
        Pattern p1 = Pattern.compile(
                "(^MUL.S)\\s+(F\\d+)\\s*,\\s*(F\\d+)\\s*,\\s*(F\\d+)");
        Matcher matcherLW = p1.matcher(strCmnd);
        if (matcherLW.find()) {
            operation = InstType.MULdotS;
            dest = Register.valueOf(matcherLW.group(2));
            source1 = Register.valueOf(matcherLW.group(3));
            source2 = Register.valueOf(matcherLW.group(4));
            return true;
        }

        return false;
    }

    //method for parsing DIV.S instruction
    //accept a parameter as string to parse it
    boolean isDIVdotS(String strCmnd) {
        Pattern p1 = Pattern.compile(
                "(^DIV.S)\\s+(F\\d+)\\s*,\\s*(F\\d+)\\s*,\\s*(F\\d+)");
        Matcher matcherLW = p1.matcher(strCmnd);
        if (matcherLW.find()) {
            operation = InstType.DIVdotS;
            dest = Register.valueOf(matcherLW.group(2));
            source1 = Register.valueOf(matcherLW.group(3));
            source2 = Register.valueOf(matcherLW.group(4));
            return true;
        }

        return false;
    }

}

//A class for reading configuration file and return its parameters
class configurator {

    public static final int BuffersConfig = 1;
    public static final int LatenciesConfig = 2;
    ////////////////////////////////////////////

    int currentConfig;

    int eff_Addr_Buff;
    int fp_Adds_Buff;
    int fp_muls_Buff;
    int ints_Buff;
    int reorder_Buff;
    int fp_add_Lat;
    int fp_sub_Lat;
    int fp_mul_Lat;
    int fp_div_Lat;

    //method for reading config.txt file
    //this method is main method of this class to parse input file config.txt
    public void readConfigFile() throws IOException {
        int configSection;
        File file = new File(".//config.txt");

        BufferedReader br = new BufferedReader(new FileReader(file));

        String st;
        while ((st = br.readLine()) != null) {
            //System.out.println(st);
            if (st.compareTo("") != 0) {
                if ((configSection = isConfigType(st)) > 0) {
                    currentConfig = configSection;
                } else {
                    paramValue(st);
                }
            }
        }

        return;
    }

    //A method for checking section of config file
    //returns section which is buffer of latancy section
    public int isConfigType(String l) {
        if (l.replaceAll("\\s+", "").contains("buffers")) {
            return BuffersConfig;
        }
        if (l.replaceAll("\\s+", "").contains("latencies")) {
            return LatenciesConfig;
        }
        return -1;
    }

    //A method for extracting paramteters values
    //accept a string and set value of parmater in parmeter fields
    public void paramValue(String l) {
        String l1 = l.replaceAll("\\s+", "");
        //System.out.println(l1);
        l = l.replaceAll("[^\\d]", "").replaceAll("[\n\r]", "");
        // System.out.println(l);
        int parValue = Integer.parseInt(l.trim());
        //int parValue=Integer.parseInt(l1.substring(l1.indexOf(":")+1));

        if (currentConfig == BuffersConfig) {

            if (l1.contains("effaddr")) {
                eff_Addr_Buff = parValue;

                return;
            }
            if (l1.contains("fpadds")) {
                fp_Adds_Buff = parValue;

                return;
            }
            if (l1.contains("fpmuls")) {

                fp_muls_Buff = parValue;

                return;
            }
            if (l1.contains("ints")) {

                ints_Buff = parValue;

                return;
            }
            if (l1.contains("reorder")) {

                reorder_Buff = parValue;
                return;
            }
        }
        if (currentConfig == LatenciesConfig) {

            if (l1.contains("fp_add")) {
                fp_add_Lat = parValue;
                return;
            }
            if (l1.contains("fp_sub")) {
                fp_sub_Lat = parValue;
                return;
            }
            if (l1.contains("fp_mul")) {
                fp_mul_Lat = parValue;
                return;
            }
            if (l1.contains("fp_div")) {
                fp_div_Lat = parValue;
                return;
            }

        }

        return;
    }

    //A method for printing configuration file
    public void printConfigParamValues() {
        System.out.println("Configuration");
        System.out.println("-------------");
        System.out.println("buffers:");

        System.out.println("   eff addr: " + eff_Addr_Buff);
        System.out.println("    fp adds: " + fp_Adds_Buff);
        System.out.println("    fp muls: " + fp_muls_Buff);
        System.out.println("       ints: " + ints_Buff);
        System.out.println("    reorder: " + reorder_Buff);

        System.out.println("\nlatencies:");

        System.out.println("   fp add: " + fp_add_Lat);
        System.out.println("   fp sub: " + fp_sub_Lat);
        System.out.println("   fp mul: " + fp_mul_Lat);
        System.out.println("   fp div: " + fp_div_Lat);

        System.out.println("\n");

    }

}
