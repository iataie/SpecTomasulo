
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
import java.util.ArrayList;
import java.util.HashMap;

//A class for defining reservation stations
//contains all structures such as buffers for reservation stations and
//their latencies.
public class ReservationStations {

    ArrayList<ReservationStation> resvStations; //reservation stations
    HashMap<InstType, Integer> latencies;       //latancy of each function
    HashMap<InstType, FunctionalUnitType> inst2FU;//map of instruction to FUs

    //Constructor of class to initialize reservation stations and latancies
    // mapping of instruction to fuctional units(FU).
    public ReservationStations() {
        resvStations = new ArrayList<ReservationStation>();
        latencies = new HashMap<InstType, Integer>();
        inst2FU = new HashMap<InstType, FunctionalUnitType>();
        setInst2FU(InstType.LW, FunctionalUnitType.Eff_Addr);
        setInst2FU(InstType.LdotS, FunctionalUnitType.Eff_Addr);
        setInst2FU(InstType.SW, FunctionalUnitType.Eff_Addr);
        setInst2FU(InstType.SdotS, FunctionalUnitType.Eff_Addr);
        setInst2FU(InstType.DADD, FunctionalUnitType.Int);
        setInst2FU(InstType.DSUB, FunctionalUnitType.Int);
        setInst2FU(InstType.BEQ, FunctionalUnitType.Int);
        setInst2FU(InstType.BNE, FunctionalUnitType.Int);
        setInst2FU(InstType.ADDdotS, FunctionalUnitType.FP_Adder);
        setInst2FU(InstType.SUBdotS, FunctionalUnitType.FP_Adder);
        setInst2FU(InstType.MULdotS, FunctionalUnitType.FP_Mult);
        setInst2FU(InstType.DIVdotS, FunctionalUnitType.FP_Mult);
    }

    public void setLatency(InstType insTypP, Integer latancyP) {
        latencies.put(insTypP, latancyP);
    }

    public Integer getLatency(InstType insTypP) {
        return latencies.get(insTypP);
    }

    public void setInst2FU(InstType instTypeP, FunctionalUnitType fUTypeP) {
        inst2FU.put(instTypeP, fUTypeP);
    }

    public void createResvStation(FunctionalUnitType fUTypeP) {
        ReservationStation rsv = new ReservationStation(fUTypeP);
        resvStations.add(rsv);
    }

    public ReservationStation getRSVS(int idx) {
        return resvStations.get(idx);
    }

    public void clearResvStation(int idx) {
        resvStations.get(idx).clear();
    }

    /*
    This method inserts new request into a free reservation station and if it is
    possible (both source operands are available) schedules its exceution 
    for the next clock cycle. 
     */
    public boolean reserve2S(int robIndxP,
            InstType instP, SourceOperand srcOpr1P, SourceOperand srcOpr2P,
            int clock) {
        int rsvStIdx = getFree(instP);
        if (rsvStIdx >= 0) {
            resvStations.get(rsvStIdx).inst = instP;
            resvStations.get(rsvStIdx).source1 = srcOpr1P;
            resvStations.get(rsvStIdx).source2 = srcOpr2P;

            resvStations.get(rsvStIdx).robDestIndx = robIndxP;

            resvStations.get(rsvStIdx).issueTime = clock;
            if (srcOpr1P.isReady() && srcOpr2P.isReady()) {
                resvStations.get(rsvStIdx).startExeTime = clock + 1;
                resvStations.get(rsvStIdx).endExeTime = getLatency(instP)
                        + clock;
                resvStations.get(rsvStIdx).state = State.Ready;

            } else {
                resvStations.get(rsvStIdx).state = State.Waiting;
            }

            return true;
        }

        return false;
    }

    /*
    This method inserts new request into a free reservation station and if it is
    possible (source operand is available) schedules its exceutionfor the next
    clock cycle. 
     */
    public boolean reserve1S(int robIndxP,
            InstType instP, SourceOperand srcOpr1P, int memAddrP,
            int clock) {
        int rsvStIdx = getFree(instP);
        if (rsvStIdx >= 0) {
            resvStations.get(rsvStIdx).inst = instP;
            resvStations.get(rsvStIdx).source1 = srcOpr1P;
            resvStations.get(rsvStIdx).memAddr = memAddrP;
            resvStations.get(rsvStIdx).robDestIndx = robIndxP;

            resvStations.get(rsvStIdx).issueTime = clock;
            if (srcOpr1P.isReady()) {
                resvStations.get(rsvStIdx).startExeTime = clock + 1;
                resvStations.get(rsvStIdx).endExeTime = getLatency(instP)
                        + clock;
                resvStations.get(rsvStIdx).state = State.Ready;

            } else {
                resvStations.get(rsvStIdx).state = State.Waiting;
            }

            return true;
        }

        return false;
    }

    /*
    This method gets an instruction and if there is a free reservation station 
    for it returns index of it.
    If there is not any free reservation station then returns -1.
     */
    public int getFree(InstType instP) {
        for (int i = 0; i < resvStations.size(); i++) {
            if (resvStations.get(i).function == inst2FU.get(instP)) {
                if (resvStations.get(i).state == State.Free) {
                    return i;
                }
            }
        }
        return -1;
    }

    //A method for updating reservation stations from result of a functional
    //unit that has finished its execution.
    //Accept cuurent clock and updates appropriate reservation stations.
    //It also frees the finished functional unit for the next instructions.
    public int updateFromCDB(int clock) {
        int updaterIdx;
        updaterIdx = findWriter2CDB(clock);
        if (updaterIdx >= 0) {
            for (int i = 0; i < resvStations.size(); i++) {
                if (resvStations.get(i).state == State.Waiting) {
                    if (resvStations.get(i).source1.robIdx == resvStations.get(
                            updaterIdx).robDestIndx) {
                        resvStations.get(i).source1.setReady(true);
                    }
                    if (hasTwoSources(resvStations.get(i).inst)) {
                        if (resvStations.get(i).source2.robIdx == resvStations
                                .get(updaterIdx).robDestIndx) {
                            resvStations.get(i).source2.setReady(true);
                        }
                    }
                }
            }
            // if(resvStations.get(updaterIdx).)
            resvStations.get(updaterIdx).state = State.Free;//<---------------- 
            UpdateReadyState(clock);
        }

        return updaterIdx;
    }

    //A method to find candidate fuctional unit to update CDB
    //Accept current clock and will find appropriate FU.
    public int findWriter2CDB(int clock) {
        int candidIdx = -1;
        for (int i = 0; i < resvStations.size(); i++) {
            //for load instruction
            if (resvStations.get(i).state == State.BusyMem && (resvStations.get(
                    i).memTime < clock)) {
                if (candidIdx < 0) {
                    candidIdx = i;
                } else if (resvStations.get(i).issueTime < resvStations.get(
                        candidIdx).issueTime) {
                    candidIdx = i;
                }
            }
            //for other instruction
            if (resvStations.get(i).state == State.Busy && !resvStations.get(i)
                    .isStoreInst()
                    && !resvStations.get(i).isBranchInst()
                    && (resvStations.get(i).endExeTime < clock)) {
                if (candidIdx < 0) {
                    candidIdx = i;
                } else if (resvStations.get(i).issueTime < resvStations.get(
                        candidIdx).issueTime) {
                    candidIdx = i;
                }
            }
        }
        return candidIdx;
    }

    //A method to update readiness of reservation stations to start their 
    //operation in next clock cycle.
    //Accept current clock cycle and update start time and end time of FUs.
    public void UpdateReadyState(int clock) {
        for (int i = 0; i < resvStations.size(); i++) {
            if (resvStations.get(i).state == State.Waiting) {
                InstType insti = resvStations.get(i).inst;
                if (hasTwoSources(insti)) {
                    if (resvStations.get(i).source1.isReady() && resvStations
                            .get(i).source2.isReady()) {
                        resvStations.get(i).state = State.Ready;
                        resvStations.get(i).startExeTime = clock + 1;
                        resvStations.get(i).endExeTime = getLatency(insti)
                                + clock;
                    }
                } else {//load and store instructions
                    if (resvStations.get(i).source1.isReady()) {
                        resvStations.get(i).state = State.Ready;
                        resvStations.get(i).startExeTime = clock + 1;
                        resvStations.get(i).endExeTime = getLatency(insti)
                                + clock;
                    }
                }
            }
        }
    }

    //A method to check whether instruction has two source operands.
    //Accept an instruction and return true if it has two source instruction
    //type such as divide and add.
    boolean hasTwoSources(InstType instP) {
        switch (instP) {
            case BEQ:
            case BNE:
            case DADD:
            case DSUB:
            case DIVdotS:
            case MULdotS:
            case ADDdotS:
            case SUBdotS:
                return true;
        }
        return false;
    }

    //A method to check whether instruction is store instruction.
    //Accept an instruction and return true if it is a store instruction.
    boolean isStoreInst(InstType instP) {
        switch (instP) {
            case SW:
            case SdotS:
                return true;
        }
        return false;
    }

    //A method to check whether instruction is load instruction.
    //Accept an instruction and return true if it is a load instruction.
    boolean isLoadInst(InstType instP) {
        switch (instP) {
            case LW:
            case LdotS:
                return true;
        }
        return false;
    }

    //method for printing state of all reservation stations
    public void print() {
        for (int i = 0; i < resvStations.size(); i++) {
            resvStations.get(i).print();
        }
    }

}
//////////////////////////////////////////////////////////////////////////////

//Class for definition of each reservation staion item
class ReservationStation {

    FunctionalUnitType function;    //function of reservation station
    int robDestIndx;                //rob of result of reservation station
    InstType inst;                  //instruction 
    SourceOperand source1;          //first source operand
    SourceOperand source2;          //second source operand
    int memAddr;                    //memory address of operand
    int issueTime;                  //issue time of instruction
    int startExeTime = 1000000;     //start time of execution
    int endExeTime = 1000000;       //end time of exectuion    
    int memTime = 1000000;          //time of writing into memory 
    int writeCDBTime = 1000000;     //time of writing to CDB 
    State state = State.Free;       //current state of reservation station

    public ReservationStation(FunctionalUnitType function) {
        this.function = function;
    }

    //method to clear reservation station for next usage
    public void clear() {
        robDestIndx = -1;
        inst = null;
        source1 = null;
        source2 = null;
        memAddr = -1;
        issueTime = 1000000;
        startExeTime = 1000000;
        endExeTime = 1000000;
        memTime = 1000000;
        State state = State.Free;
    }

    //A method to check whether instruction is store instruction.
    //Accept an instruction and return true if it is a store instruction.
    public boolean isStoreInst() {
        if (inst == null) {
            return false;
        }
        switch (inst) {
            case SW:
            case SdotS:
                return true;
        }
        return false;
    }

    //A method to check whether instruction is load instruction.
    //Accept an instruction and return true if it is a load instruction.
    public boolean isLoadInst() {
        if (inst == null) {
            return false;
        }
        switch (inst) {
            case LW:
            case LdotS:
                return true;
        }
        return false;
    }

    //A method to check whether instruction is branch instruction.
    //Accept an instruction and return true if it is a branch instruction.
    public boolean isBranchInst() {
        if (inst == null) {
            return false;
        }
        switch (inst) {
            case BEQ:
            case BNE:
                return true;
        }
        return false;
    }

    //method for printing state of reservation station
    public void print() {
        if (source1 != null && source2 != null) {
            System.out.println("ROB:" + robDestIndx + " FU:" + function
                    + " Inst:" + inst + " Source1:"
                    + source1.reg + "|" + source1.robIdx + " Source2:"
                    + source2.reg + "|"
                    + source2.robIdx + " ITime:" + issueTime + " STime:"
                    + startExeTime
                    + " ETime:" + endExeTime + " MemTime:" + memTime + " State:"
                    + state);
        }
        if (source1 != null && source2 == null) {
            System.out.println("ROB:" + robDestIndx + " FU: " + function
                    + " Inst:" + inst + " Source1: "
                    + source1.reg + "|" + source1.robIdx + " Source2:N/A "
                    + " ITime: " + issueTime
                    + " STime: " + startExeTime + " ETime: " + endExeTime
                    + " MemTime:" + memTime + " State: " + state);
        }

        if (source1 == null && source2 == null) {
            System.out.println("ROB:" + robDestIndx + " FU: " + function
                    + " Inst:" + inst + " ITime: " + issueTime
                    + " STime: " + startExeTime + " ETime: " + endExeTime
                    + " MemTime:" + memTime + " State: " + state);
        }
    }
}
//class for different state of each reservation station
/////////////////////////////////////////////////////////////
enum State {
    Free,
    Ready,
    Waiting,
    Busy,
    BusyAddr,
    BusyMem
}
/////////////////////////////////////////////////////////////

//class for definition of each source opernd
//each opend could be a register or index into rob buffer.
class SourceOperand {

    Register reg = null;        //register of opernad
    int robIdx = -1;            //index of rob buffer
    boolean ready = false;      //ready state of operand    

    public SourceOperand(Register regP) {
        setRegister(regP);
    }

    public SourceOperand(int robP) {
        setRobIdx(robP);
        if (robP < 0) {
            ready = true;
        }
    }

    public void setRegister(Register regP) {
        reg = regP;
    }

    public void setRobIdx(int robP) {
        robIdx = robP;
    }

    public void setReady(boolean rdy) {
        ready = rdy;
    }

    public boolean isReady() {
        return ready;
    }

}
