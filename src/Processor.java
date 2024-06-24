
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
//A class for simuating processor architecture
//contains ReservationStations, ReorderBuffer,RegisterFile, and Register file.
//this class simulate four stage of Speculative Tomasulo algorithm.
public class Processor {

    ReservationStations RsvSts; //Reservation staions
    ReorderBuffer RoB;          //Reorder buffer
    RegisterFile RegFile;       // Registers
    InstructionQueue IQ;        //Instruction Queue

    boolean MemBusy = false;    //Flag for memory busy
    int memBusyWaitCycles = 0;  //Counter of memory busy cycles
    boolean writeAccess = false;//Flag for accessing Store instruction
    int robDelay = 0;           //Counter of Reorder Buffer delays
    int rsvDelay = 0;           //Counter for Reservation Stations delays
    boolean nextChance = false; //flag for doing recommite cycle
    int RAWDealy = 0;

    int clock = 1;              //Clocck od Processor

    //Constructor of class to initialize class based on parameters
    //These parameters will set buffers and delays.
    public Processor(int effAddrCountP, int fpAddCountP, int fpMulCountP,
            int intCountP, int fpAddLatP, int fpSubLatP, int fpMulLatP,
            int fpDivLat, int robBuffSize, int instQueueSizeP) {

        RegFile = new RegisterFile();
        RoB = new ReorderBuffer(robBuffSize);
        RsvSts = new ReservationStations();
        IQ = new InstructionQueue(instQueueSizeP);

        for (int i = 0; i < effAddrCountP; i++) {
            RsvSts.createResvStation(FunctionalUnitType.Eff_Addr);
        }

        for (int i = 0; i < fpAddCountP; i++) {
            RsvSts.createResvStation(FunctionalUnitType.FP_Adder);
        }

        for (int i = 0; i < fpMulCountP; i++) {
            RsvSts.createResvStation(FunctionalUnitType.FP_Mult);
        }

        for (int i = 0; i < intCountP; i++) {
            RsvSts.createResvStation(FunctionalUnitType.Int);
        }

        RsvSts.setLatency(InstType.LdotS, 1);
        RsvSts.setLatency(InstType.SdotS, 1);
        RsvSts.setLatency(InstType.LW, 1);
        RsvSts.setLatency(InstType.SW, 1);

        RsvSts.setLatency(InstType.DADD, 1);
        RsvSts.setLatency(InstType.DSUB, 1);

        RsvSts.setLatency(InstType.BEQ, 1);
        RsvSts.setLatency(InstType.BNE, 1);

        RsvSts.setLatency(InstType.ADDdotS, fpAddLatP);
        RsvSts.setLatency(InstType.SUBdotS, fpSubLatP);
        RsvSts.setLatency(InstType.MULdotS, fpMulLatP);
        RsvSts.setLatency(InstType.DIVdotS, fpDivLat);

    }

    //Inserting an instruction and fields into Instruction Queue
    //accept instruction and operands and puts into Instruction Queue
    public void inserInst(InstType opP, Register src1P, Register src2P,
            Register dstP,
            int memAddP, String memLblP, String instStrP) {
        IQ.insert(opP, src1P, src2P, dstP, memAddP, memLblP, instStrP);
    }

    //This method simulate issue stage of the alogithm
    //it fetchs an instruction and if reorder buffer and its resveration staion
    //is free will put the instruction in the reservation station buffer and ROB 
    public boolean doIssueStage() {
        SourceOperand op1, op2 = null;

        if (IQ.currentInst() == null) {
            return false;
        }

        if (RoB.isFull()) {
            robDelay++;
        }
        if (!RoB.isFull() && RsvSts.getFree(IQ.currentInst().operation) < 0) {
            rsvDelay++;
        }

        //if both reorder bufer and reservation station have free slots  
        if (RoB.isFull() != true && RsvSts.getFree(IQ.currentInst().operation)
                >= 0) {
            QueueItem FetchedInst = IQ.fetch();
            //      --------Reservation Station--------------------
            //
            int robIdx = RoB.insert(FetchedInst.operation, FetchedInst.destReg,
                    FetchedInst.instStr, IQ.pc - 1);

            //Prepare oprand1 and operand2            
            //check registerfile for source 1
            op1 = new SourceOperand(FetchedInst.srcReg1);
            op1.setRobIdx(RegFile.getRegVal(FetchedInst.srcReg1));
            //check registerfile for source 2
            if (hasTwoSources(FetchedInst.operation)) {
                op2 = new SourceOperand(FetchedInst.srcReg2);
                op2.setRobIdx(RegFile.getRegVal(FetchedInst.srcReg2));
            }

            if (op1.robIdx >= 0) //if op1 is busy 
            {
                op1.setReady(RoB.getReady(op1.robIdx));
            } else {
                op1.setReady(true);
            }

            if (hasTwoSources(FetchedInst.operation)) {
                if (op2.robIdx >= 0) {
                    op2.setReady(RoB.getReady(op2.robIdx));
                } else {
                    op2.setReady(true);
                }
                RsvSts.reserve2S(robIdx, FetchedInst.operation, op1, op2, clock);
            } else {

                RsvSts.reserve1S(robIdx, FetchedInst.operation, op1,
                        FetchedInst.memAddress, clock);
            }
            //---------------------ROB & RegFile-------------------------                
            //Update rob destination and regfile value
            if (!isStoreInst(FetchedInst.operation) && !isBranchInst(
                    FetchedInst.operation)) {
                RegFile.setRegVal(FetchedInst.destReg, robIdx);
                RoB.buff[robIdx].destReg = FetchedInst.destReg;
            }
            if (isStoreInst(FetchedInst.operation)) {
                RoB.buff[robIdx].destMemAdd = FetchedInst.memAddress;
            }

        }
        return true;
    }

    //This method simulate execute stage of the alogithm
    //if operand of instruction is ready it will start execution of operation
    //it for different instruction has different steps.
    public boolean doExecuteStage() {
        //change state of ready reservation to busy for all except load 
        // for load change first to BusyAddr and then BusyMem
        // 
        for (int i = 0; i < RsvSts.resvStations.size(); i++) {
            if (RsvSts.getRSVS(i).state != State.Free) {
                if (RsvSts.getRSVS(i).startExeTime == clock) {
                    if (RsvSts.getRSVS(i).isLoadInst()) {//for load instruction
                        RsvSts.getRSVS(i).state = State.BusyAddr;
                    } else {
                        RsvSts.getRSVS(i).state = State.Busy;
                    }
                }
                //for the follwing parts Should memBusyCycles be updated <++++
                //for load inst to change state into memory read
                if (RsvSts.getRSVS(i).isLoadInst()
                        && RsvSts.getRSVS(i).endExeTime < clock
                        && RsvSts.getRSVS(i).state == State.BusyAddr
                        && !MemBusy
                        && !RoB.isStoreBefore(RsvSts.getRSVS(i).robDestIndx,
                                RsvSts.getRSVS(i).memAddr)) {
                    RsvSts.getRSVS(i).state = State.BusyMem;
                    RsvSts.getRSVS(i).memTime = clock;
                    MemBusy = true;
                }
                //for updating memBusyCycles if memory is not ready for load 
                //instruction
                if (RsvSts.getRSVS(i).isLoadInst()
                        && RsvSts.getRSVS(i).endExeTime < clock
                        && RsvSts.getRSVS(i).state == State.BusyAddr
                        && MemBusy
                        && !RoB.isStoreBefore(RsvSts.getRSVS(i).robDestIndx,
                                RsvSts.getRSVS(i).memAddr)) {
                    memBusyWaitCycles++;
                }

                if (RsvSts.getRSVS(i).isLoadInst()
                        && RsvSts.getRSVS(i).endExeTime < clock
                        && RsvSts.getRSVS(i).state == State.BusyAddr
                        && !MemBusy
                        && !RoB.isStoreBefore(RsvSts.getRSVS(i).robDestIndx,
                                RsvSts.getRSVS(i).memAddr)) {
                    RsvSts.getRSVS(i).state = State.BusyMem;
                    RsvSts.getRSVS(i).memTime = clock;
                    MemBusy = true;
                }
                ////////////////////////////////////////////
                if (RsvSts.getRSVS(i).isLoadInst()
                        && RsvSts.getRSVS(i).endExeTime < clock
                        && RsvSts.getRSVS(i).state == State.BusyAddr
                        && !MemBusy
                        && RoB.isStoreBefore(RsvSts.getRSVS(i).robDestIndx,
                                RsvSts.getRSVS(i).memAddr)) {
                    RAWDealy++;
                }

                ////////////////////////////////////////////
                //for store instructions
                if (RsvSts.getRSVS(i).isStoreInst()
                        && RsvSts.getRSVS(i).endExeTime < clock) {

                    RsvSts.getRSVS(i).state = State.Free;

                    RsvSts.clearResvStation(i);
                }

                //////////////// 
                if (RsvSts.getRSVS(i).isStoreInst()
                        && RsvSts.getRSVS(i).endExeTime == clock) {
                    RoB.buff[RsvSts.getRSVS(i).robDestIndx].ready = true;

                    RoB.buff[RsvSts.getRSVS(i).robDestIndx].issueTime = RsvSts
                            .getRSVS(i).issueTime;
                    RoB.buff[RsvSts.getRSVS(i).robDestIndx].startExeTime
                            = RsvSts.getRSVS(i).startExeTime;
                    RoB.buff[RsvSts.getRSVS(i).robDestIndx].endExeTime = RsvSts
                            .getRSVS(i).endExeTime;

                }

                ////////////////////
                //for branch instructions
                if (RsvSts.getRSVS(i).isBranchInst()
                        && RsvSts.getRSVS(i).endExeTime < clock) {
                    RoB.buff[RsvSts.getRSVS(i).robDestIndx].ready = true;

                    RoB.buff[RsvSts.getRSVS(i).robDestIndx].issueTime = RsvSts
                            .getRSVS(i).issueTime;
                    RoB.buff[RsvSts.getRSVS(i).robDestIndx].startExeTime
                            = RsvSts.getRSVS(i).startExeTime;
                    RoB.buff[RsvSts.getRSVS(i).robDestIndx].endExeTime = RsvSts
                            .getRSVS(i).endExeTime;

                    RsvSts.getRSVS(i).state = State.Free;

                    RsvSts.clearResvStation(i);
                }

            }
        }
        return true;
    }

    //This method simulate Writeback to CDB stage of the alogithm
    //if one instruction is finished execution phase and result is ready and CDB
    //is free it will write into CDB.    
    public boolean doWritebackStage() {

        //1-update Reservation stations
        int rsvUpdaterIdx = RsvSts.updateFromCDB(clock);
        //2- update ROB    
        if (rsvUpdaterIdx >= 0) {
            ReservationStation rsvUpdater = RsvSts.resvStations.get(
                    rsvUpdaterIdx);
            int robIdx = rsvUpdater.robDestIndx;
            RoB.buff[robIdx].ready = true;
            //update ROB time fields such as issue time,....
            RoB.buff[robIdx].issueTime = rsvUpdater.issueTime;
            RoB.buff[robIdx].startExeTime = rsvUpdater.startExeTime;
            RoB.buff[robIdx].endExeTime = rsvUpdater.endExeTime;
            RoB.buff[robIdx].memReadTime = rsvUpdater.memTime;
            RoB.buff[robIdx].writeCDBTime = clock;
            //release memory for the next loads and stores memory access 
            if (rsvUpdater.isLoadInst()) {
                MemBusy = false;
            }

            //clean up reservation station
            RsvSts.clearResvStation(rsvUpdaterIdx);

        }
        return true;
    }

    //This method simulate commit stage of the alogithm
    //if operation is in front of ORB and is ready it will copy its result into
    //memory or registers.
    public boolean doCommitStage() {

        if (writeAccess) {
            writeAccess = false;
            MemBusy = false;
        }

        if (RoB.isempty()) {
            if (IQ.isEmpty()) {
                return false;
            } else {
                return true;
            }
        }

        if (!RoB.getHead().ready) {
            nextChance = true;
            return true;
        }

        if (RoB.getHead().isStoreInst()) {
            if (!MemBusy) {
                MemBusy = true;
                writeAccess = true;

                ReorderBufferElement robHead = RoB.getHead();
                IQ.buff[robHead.indxIQ].updateStat(robHead.issueTime,
                        robHead.startExeTime, robHead.endExeTime,
                        robHead.memReadTime, robHead.writeCDBTime, clock);

                int crob = RoB.remove();
                RoB.buff[crob].clear();

            } else {
                memBusyWaitCycles++;
            }
        } else {
            if (!RoB.getHead().isBranchInst()) {
                if (RegFile.getRegVal(RoB.getHead().destReg) == RoB.head) {
                    RegFile.setRegVal(RoB.getHead().destReg, -1);
                }
            }

            ReorderBufferElement robHead = RoB.getHead();
            IQ.buff[robHead.indxIQ].updateStat(robHead.issueTime,
                    robHead.startExeTime, robHead.endExeTime,
                    robHead.memReadTime, robHead.writeCDBTime, clock);

            int crob = RoB.remove();
            RoB.buff[crob].clear();

        }

        return true;
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

    //A method to check whether instruction is branch instruction.
    //Accept an instruction and return true if it is a branch instruction.
    boolean isBranchInst(InstType instP) {
        switch (instP) {
            case BEQ:
            case BNE:
                return true;
        }
        return false;
    }

}
