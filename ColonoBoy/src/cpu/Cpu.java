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

        if (newValue > 0xFF) {
            flagC = true;
            newValue &= 0b1111_1111;
        } else {
            flagC = false;
        }

        flagZ = (newValue == 0);
        flagN = false;
        flagH = ((((a.getData() & 0xf) + (nRegValue & 0xf)) & 0x10) == 0x10);

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
        newValue += flagC ? 1 : 0;

        if (newValue > 0xFF) {
            flagC = true;
            newValue &= 0b1111_1111;
        } else {
            flagC = false;
        }

        flagZ = (newValue == 0);
        flagN = false;
        flagH = ((((a.getData() & 0xf) + (nRegValue & 0xf)) & 0x10) == 0x10);

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
        flagH = ((((oldValue & 0xfff) + (nRegValue & 0xfff)) & 0x1000) == 0x1000);

        if (newValue > 0xFFFF) {
            flagC = true;
            newValue &= 0xFFFF;
        } else {
            flagC = false;
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
        throw new UnsupportedOperationException("Not implemented yet");
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
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private int readCombinedRegisters(Register upper, Register lower) {
        return (upper.getData() << 8) | lower.getData();
    }

    private void loadCombinedRegisters(Register h, Register l, int newValue) {
        h.load(newValue >> 8);
        l.load(newValue & 0xff);
    }

    public void printInfo() {
        System.out.println("-------------------------------");
        System.out.println("CPU INFO");
        System.out.println("");
    }

}
