package cpu;

import cpu.registers.RegisterController;

/**
 *
 * @author Giliardi Schmidt
 */
public class Cpu {

    private final MemoryController memoryController;

    private final Register a;
    private final Register b;
    private final Register d;
    private final Register h;
    private final Register f;
    private final Register c;
    private final Register e;
    private final Register l;

    //stack pointer - current stack position
    private final Register sp;
    //program counter - next instruction
    private final Register pc;

    private boolean flagZ;
    private boolean flagN;
    private boolean flagH;
    private boolean flagC;

    public Cpu() {
        this.memoryController = new MemoryController();

        this.a = new Register();
        this.b = new Register();
        this.d = new Register();
        this.h = new Register();
        this.f = new Register();
        this.c = new Register();
        this.e = new Register();
        this.l = new Register();
        this.sp = new Register();
        this.pc = new Register();

        init();
    }

    private void init() {
        //at startup the PC is set to 0x100
        this.pc.setData(0x100);
        //at startup the PC is set to 0xFFFE
        this.sp.setData(0xFFFE);
    }

    public void executeCicle() {
        // fetch(); decode(); execute();
        int opcode = fetch();
        Runnable operation = decode(opcode);
        execute(operation);
    }

//<editor-fold defaultstate="collapsed" desc="CPU execution cicle">
    private int fetch() {
        int opcode = memoryController.read(this.pc.getData());
        //TODO: increment PC

        return opcode;
    }

    private Runnable decode(int opcode) {
        switch (opcode) {
            case 0x00:
                return () -> nopInst();
            case 0x01:
                return () -> ldBCd16Inst();
            case 0x02:
                return () -> ldBCAInst();
            case 0x03:
                return () -> incBCInst();
            case 0x04:
                return () -> incBInst();
            case 0x05:
                return () -> decBInst();
            case 0x06:
                return () -> ldBd8Inst();
            case 0x07:
                return () -> rlcAInst();
            case 0x08:
                return () -> ldA16SPInst();
            case 0x09:
                return () -> addHLBCInst();
            case 0x0A:
                return () -> ldABCInst();
            case 0x0B:
                return () -> decBCInst();
            case 0x0C:
                return () -> incCInst();
            case 0x0D:
                return () -> decCInst();
            case 0x0E:
                return () -> ldCd8Inst();
            case 0x0F:
                return () -> rrcAInst();
            case 0x10:
                return () -> stopInst();

            case 0x20:
                return () -> jrNZr8Inst();

            case 0x30:
                return () -> jrNCr8Inst();

            case 0x40:
                return () -> ldBBInst();

            case 0x50:
                return () -> ldDBInst();

            case 0x60:
                return () -> ldHBInst();

            case 0x70:
                return () -> ldHLBInst();

            case 0x80:
                return () -> addABInst();

            case 0x90:
                return () -> subBInst();

            case 0xA0:
                return () -> andBInst();

            case 0xB0:
                return () -> orBInst();

            case 0xC0:
                return () -> retNZInst();

            case 0xD0:
                return () -> retNCInst();

            case 0xE0:
                return () -> ldha8AInst();

            case 0xF0:
                return () -> ldaAa8Inst();

        }
        throw new RuntimeException("Opcode " + opcode + " is not implemented yet or it's invalid.");
    }

    private void execute(Runnable operation) {
        operation.run();
    }

    private void consumeClock(int cicles) {

    }
//</editor-fold>

    /**
     * - Add n + Carry flag to A.
     *
     * n = A,B,C,D,E,H,L,(HL),#
     *
     * Flags affected:
     *
     * Z - Set if result is zero.
     *
     * N - Reset.
     *
     * H - Set if carry from bit 3.
     *
     * C - Set if carry from bit 7.
     */
    private void adc_A_n(Register regOne, Register regTwo) {
        int newValue, nRegValue;

        if (regTwo == null) {
            nRegValue = regOne.getData();
        } else {
            nRegValue = readCombinedRegisters(regOne, regTwo);
        }

        newValue = a.getData() + nRegValue;
        newValue += flagC ? 1 : 0;

        flagC = checkCarry(newValue, false);
        if (flagC) {
            newValue = newValue &= 0xFF;
        }

        flagZ = (newValue == 0);
        flagN = false;
        flagH = checkHalfCarry(a.getData(), nRegValue, false);

        a.load(newValue);
    }

    /**
     * ADD A,n - Add n to A.
     *
     * n = A,B,C,D,E,H,L,(HL),#
     *
     * Flags affected:
     *
     * Z - Set if result is zero.
     *
     * N - Reset.
     *
     * H - Set if carry from bit 3.
     *
     * C - Set if carry from bit 7.
     *
     */
    private void add_A_n(Register regOne, Register regTwo) {
        int newValue, nRegValue;

        if (regTwo == null) {
            nRegValue = regOne.getData();
        } else {
            nRegValue = readCombinedRegisters(regOne, regTwo);
        }

        newValue = a.getData() + nRegValue;

        flagC = checkCarry(newValue, false);
        if (flagC) {
            newValue &= 0xFF;
        }

        flagZ = (newValue == 0);
        flagN = false;
        flagH = checkHalfCarry(nRegValue, a.getData(), false);

        a.load(newValue);
    }

    /**
     * ADD HL,n - Add n to HL.
     *
     * n = BC,DE,HL
     *
     * Flags affected:
     *
     * Z - Not affected
     *
     * N - Reset.
     *
     * H - Set if carry from bit 11.
     *
     * C - Set if carry from bit 15.
     */
    private void add_HL_n(Register regOne, Register regTwo) {
        int newValue, nRegValue, oldValue;

        oldValue = readCombinedRegisters(h, l);
        nRegValue = readCombinedRegisters(regOne, regTwo);
        newValue = oldValue + nRegValue;

        flagN = false;
        flagH = checkHalfCarry(oldValue, nRegValue, true);

        flagC = checkCarry(newValue, true);
        if (flagC) {
            newValue &= 0xFFFF;
        }

        loadCombinedRegisters(h, l, newValue);

    }

    /**
     * ADD SP,n - Add n to Stack Pointer (SP).
     *
     * n = one byte signed immediate value
     *
     * Flags affected:
     *
     * Z - Reset.
     *
     * N - Reset.
     *
     * H - Set or reset according to operation.
     *
     * C - Set or reset according to operation.
     */
    private void add_SP_n() {
        int pcValue, newValue;

        pcValue = memoryController.read(pc.getData());
        pcValue = getSignedInt(pcValue);

        newValue = pcValue + sp.getData();

        flagH = checkHalfCarry(pcValue, sp.getData(), false);
        flagC = checkCarry(newValue, false);
        if (flagC) {
            newValue &= 0xFFFF;
        }

        flagZ = false;
        flagN = false;
        sp.load(newValue);

    }

    /**
     * AND n - Logically AND n with A, result in A.
     *
     * n = A,B,C,D,E,H,L,(HL),#
     *
     * Flags affected:
     *
     * Z - Set if result is zero.
     *
     * N - Reset.
     *
     * H - Set.
     *
     * C - Reset.
     */
    private void and_n(Register regOne, Register regTwo) {
        int value;

        if (regTwo == null) {
            value = regOne.getData();
        } else {
            value = readCombinedRegisters(regOne, regTwo);
        }

        a.load(a.getData() & value);

        flagZ = a.getData() == 0;
        flagH = true;
        flagN = false;
        flagC = false;
    }

    /**
     * BIT b,r - Test bit b in register r.
     *
     * b = 0-7, r = A,B,C,D,E,H,L,(HL)
     *
     * Flags affected:
     *
     * Z - Set if bit b of register r is 0.
     *
     * N - Reset.
     *
     * H - Set.
     *
     * C - Not affected.
     */
    private void bit_b_n(Register r, int b) {
        int value = r.getData();

        flagZ = ((value >> b) & 1) == 0;
        flagN = false;
        flagH = true;
    }

    /**
     * CALL n - Push address of next instruction onto stack and then jump to
     * address nn.
     *
     * nn = 16 bit address
     *
     * Flags affected:
     *
     * None
     */
    private void call_nn() {
        int value = getWordFromPC();

        pushToStack(value);
        pc.load(value);
    }

    /**
     * CALL cc,n - Call address nn if following condition is true:
     *
     * nn = 16 bit value
     *
     * cc = NZ, Call if Z flag is reset.
     *
     * cc = Z, Call if Z flag is set.
     *
     * cc = NC, Call if C flag is reset.
     *
     * cc = C, Call if C flag is set.
     *
     * Flags affected:
     *
     * None
     */
    private void call_cc_n(boolean flag) {
        int value = getWordFromPC();
        pc.increment();

        if (flag) {
            //condition met
            pc.load(value);
            consumeClock(24);

        } else {
            //condition not met
            consumeClock(12);
        }
    }

    /**
     *
     * CCF - Complement carry flag.
     *
     * If C flag is set then reset it.
     *
     * If C flag is reset then set it.
     *
     * Flags affected:
     *
     * Z - Not affected.
     *
     * N - Reset.
     *
     * H - Reset.
     *
     * C - Complemented.
     */
    private void ccf() {
        flagN = false;
        flagH = false;
        flagC = !flagC;
    }

    /**
     * CP n - Compare A with n.
     *
     * This is basically an A - n subtraction instruction but the results are
     * thrown away.
     *
     * n = A,B,C,D,E,H,L,(HL),#
     *
     * Flags affected:
     *
     * Z - Set if result is zero. (Set if A = n)
     *
     * N - Set.
     *
     * H - Set if no borrow from bit 4.
     *
     * C - Set for no borrow. (Set if A < n.)
     *
     */
    private void cp_n(int nValue) {
        int value = a.getData() - nValue;

        flagZ = value == 0;
        flagN = true;
        flagH = checkHalfBorrow(a.getData(), nValue, false);
        flagC = checkBorrow(a.getData(), nValue);
    }

    /**
     * CPL - Complement A register. (Flip all bits.)
     *
     * Flags affected:
     *
     * Z - Not affected.
     *
     * N - Set.
     *
     * H - Set.
     *
     * C - Not affected.
     */
    private void cpl() {
        a.load(~a.getData());

        flagN = true;
        flagH = true;
    }

    /**
     * see: https://ehaskins.com/2018-01-30%20Z80%20DAA/
     *
     * DAA - Decimal adjust register A.
     *
     * This instruction adjusts register A so that the correct representation of
     * Binary Coded Decimal (BCD) is obtained.
     *
     * Flags affected:
     *
     * Z - Set if register A is zero.
     *
     * N - Not affected.
     *
     * H - Reset.
     *
     * C - Set of reset according to operation.
     *
     */
    private void daa() {
        throw new UnsupportedOperationException("WTF is this OP!");
    }

    /**
     * DEC n - Decrement register n.
     *
     * n = A,B,C,D,E,H,L,(HL)
     *
     * Flags affected:
     *
     * Z - Set if result is zero.
     *
     * N - Set.
     *
     * H - Set if no borrow from bit 4.
     *
     * C - Not affected.
     */
    private void dec_n(Register reg) {
    }

//<editor-fold defaultstate="collapsed" desc="Util methods">
    /**
     * Read the bits from 2 registers and return them combined.
     *
     * Ex: IF: upper = 1111 and lower = 000 THEN return 1111_0000
     *
     * @param upper Upper register
     * @param lower Lower register
     *
     * @return The two registers combined
     */
    private int readCombinedRegisters(Register upper, Register lower) {
        return (upper.getData() << 8) | lower.getData();
    }

    /**
     * Load the informed value into the informed registers.
     *
     * Ex: IF newValue = 1111_0000 THEN upper.value = 1111 and lower.value =
     * 0000
     *
     * @param upper Upper register
     * @param lower Lower register
     * @param newValue Value to be loaded
     */
    private void loadCombinedRegisters(Register upper, Register lower, int newValue) {
        upper.load(newValue >> 8);
        lower.load(newValue & 0xff);
    }

    /**
     * Check if there's half carry from n-bit to y-bit when adding two values.
     *
     * @param value Value to be checked
     * @param bits16 If true then is a 16 bit sum (check carry from bit 7 to 8).
     * Else, it's a 8 bit sum (check carry from bit 3 to 4).
     *
     * @return true if carry occurs, otherwise false
     */
    private boolean checkHalfCarry(int valueA, int valueB, boolean bits16) {
        if (bits16) {
            return ((((valueA & 0xfff) + (valueB & 0xfff)) & 0x1000) == 0x1000);
        } else {
            return ((((valueA & 0xf) + (valueB & 0xf)) & 0x10) == 0x10);
        }
    }

    /**
     * Check if there's carry from n-bit to y-bit when adding two values.
     *
     * @param valueA Value to be added
     * @param valueB Value to be added
     * @param bits16 If true then is a 16 bit sum (check carry from bit 15 to
     * 16). Else, it's a 8 bit sum (check carry from bit 7 to 8).
     *
     * @return true if carry occurs, otherwise false
     */
    private boolean checkCarry(int value, boolean bits16) {
        if (bits16) {
            return value > 0xFFFF;
        } else {
            return value > 0xFFFF;
        }
    }

    /**
     * Check if there's half borrow from n-bit to y-bit when subtracting two
     * values.
     *
     * @param value Value to be checked
     * @param bits16 If true then is a 16 bit subtract (check borrow from bit 8
     * to 7). Else, it's a 8 bit subtract (check carry from bit 4 to 3).
     *
     * @return true if carry occurs, otherwise false
     */
    private boolean checkHalfBorrow(int valueA, int valueB, boolean bits16) {
        if (bits16) {
            return ((valueA & 0xFFF) < (valueB & 0xFFF));
        } else {
            return ((valueA & 0xF) < (valueB & 0xF));
        }
    }

    /**
     * Check if there's borrow from n-bit to y-bit when subtracting two values.
     *
     * @param valueA Value to be subtracted
     * @param valueB Value to be subtracted
     *
     * @return true if borrow occurs, otherwise false
     */
    private boolean checkBorrow(int valueA, int valueB) {
        return valueA < valueB;
    }

    /**
     * Transform the informed value to a signed int.
     *
     * @param value To be transformed
     * @return Signed int
     */
    private int getSignedInt(int value) {
        if (value > 127) {
            return -((~value + 1) & 0xFF);
        } else {
            return value;
        }
    }

    /**
     * Read a word from the address in PC. Read the value in the address
     * apponted in PC then increment PC and read the value again.
     *
     * @return Word read from addresses apponted in PC
     */
    private int getWordFromPC() {
        int lowerBits, upperBits;

        lowerBits = memoryController.read(pc.getData());
        pc.increment();
        upperBits = memoryController.read(pc.getData());

        return (upperBits << 8) | lowerBits;
    }

    /**
     * Push the informed value into the stack pointer.
     *
     * @param value Value to be added
     */
    private void pushToStack(int value) {
        //write upper bits
        memoryController.writeByte(sp.getData(), value >> 8);
        sp.increment();

        //write lower bits
        memoryController.writeByte(sp.getData(), value & 0xFF);
    }
//</editor-fold>

    public void printInfo() {
        System.out.println("-------------------------------");
        System.out.println("CPU INFO");
        System.out.println("");
    }

}
