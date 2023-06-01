package hardwar.branch.prediction.judged.PAp;


import hardwar.branch.prediction.shared.*;
import hardwar.branch.prediction.shared.devices.*;

import java.util.Arrays;

public class PAp implements BranchPredictor {

    private final int branchInstructionSize;

    private final ShiftRegister SC; // saturating counter register

    private final RegisterBank PABHR; // per address branch history register

    private final Cache<Bit[], Bit[]> PAPHT; // Per Address Predication History Table

    public PAp() {
        this(4, 2, 8);
    }

    public PAp(int BHRSize, int SCSize, int branchInstructionSize) {
        // TODO: complete the constructor
        this.branchInstructionSize = branchInstructionSize;

        // Initialize the PABHR with the given bhr and branch instruction size
        PABHR = new RegisterBank(branchInstructionSize ,BHRSize);

        // Initializing the PAPHT with BranchInstructionSize as PHT Selector and 2^BHRSize row as each PHT entries
        // number and SCSize as block size

        PAPHT = new PerAddressPredictionHistoryTable(branchInstructionSize,1<<BHRSize ,SCSize);
        // Initialize the SC register
        SC = new SIPORegister("",SCSize,null);
    }

    @Override
    public BranchResult predict(BranchInstruction branchInstruction) {
        // TODO: complete Task 1


        Bit[] add = getCacheEntry(branchInstruction.getInstructionAddress(),PABHR.read(branchInstruction.getInstructionAddress()).read());

        PAPHT.putIfAbsent(add,getDefaultBlock());

        Bit[]read = PAPHT.get(add);


        SC.load(read);


        if (SC.read()[0].getValue())
            return BranchResult.TAKEN;
        return BranchResult.NOT_TAKEN;
    }

    @Override
    public void update(BranchInstruction instruction, BranchResult actual) {
        // TODO:complete Task 2
        boolean n;
        n = actual.equals(BranchResult.TAKEN);


        PAPHT.put(PABHR.read(instruction.getInstructionAddress()).read(), CombinationalLogic.count(SC.read(),n,CountMode.SATURATING));
        


        ShiftRegister nn = PABHR.read(instruction.getInstructionAddress());
        nn.insert(Bit.of(n));
        PABHR.write(instruction.getInstructionAddress() , nn.read());
    }


    private Bit[] getCacheEntry(Bit[] branchAddress, Bit[] BHRValue) {
        // Concatenate the branch address bits with the BHR bits
        Bit[] cacheEntry = new Bit[branchAddress.length + BHRValue.length];
        System.arraycopy(branchAddress, 0, cacheEntry, 0, branchInstructionSize);
        System.arraycopy(BHRValue, 0, cacheEntry, branchAddress.length, BHRValue.length);
        return cacheEntry;
    }

    /**
     * @return a zero series of bits as default value of cache block
     */
    private Bit[] getDefaultBlock() {
        Bit[] defaultBlock = new Bit[SC.getLength()];
        Arrays.fill(defaultBlock, Bit.ZERO);
        return defaultBlock;
    }

    @Override
    public String monitor() {
        return "PAp predictor snapshot: \n" + PABHR.monitor() + SC.monitor() + PAPHT.monitor();
    }
}
