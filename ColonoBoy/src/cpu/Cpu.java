package cpu;

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

    private boolean pendingInterrupt;
    private boolean isHalted;

    public Cpu() {
        this.memoryController = new MemoryController();

        this.a = new Register(false);
        this.b = new Register(false);
        this.d = new Register(false);
        this.h = new Register(false);
        this.f = new Register(false);
        this.c = new Register(false);
        this.e = new Register(false);
        this.l = new Register(false);
        this.sp = new Register(true);
        this.pc = new Register(true);

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
        if (pendingInterrupt) {
            //TODO: this
        }

        int opcode = fetch();
        Runnable operation = decode(opcode);
        execute(operation);

    }

//<editor-fold defaultstate="collapsed" desc="CPU execution cicle">
    private int fetch() {
        int opcode = memoryController.readByte(this.pc.getData());
        //TODO: increment PC

        return opcode;
    }

    /**
     * WARNING! SEE JUMP AND CALL INSTRUCTIONS!!!!
     *
     * NZ and NC = !flag
     *
     * Z and C = flag
     *
     * @param opcode
     * @return
     */
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
    private void adc_A_n(int value) {
        int newValue;

        newValue = a.getData() + value;
        newValue += flagC ? 1 : 0;

        flagC = checkCarry(newValue, false);
        if (flagC) {
            newValue = newValue &= 0xFF;
        }

        flagZ = (newValue == 0);
        flagN = false;
        flagH = checkHalfCarry(a.getData(), value, false);

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
    private void add_A_n(int value) {
        int newValue;

        newValue = a.getData() + value;

        flagC = checkCarry(newValue, false);
        if (flagC) {
            newValue &= 0xFF;
        }

        flagZ = (newValue == 0);
        flagN = false;
        flagH = checkHalfCarry(value, a.getData(), false);

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
    private void add_HL_n(int value) {
        int newValue, oldValue;

        oldValue = readCombinedRegisters(h, l);
        newValue = oldValue + value;

        flagN = false;
        flagH = checkHalfCarry(oldValue, value, true);

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

        pcValue = memoryController.readByte(pc.getData());
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
    private void and_n(int value) {
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
    private void bit_b_n(int value, int bytePos) {
        flagZ = ((value >> bytePos) & 1) == 0;
        flagN = false;
        flagH = true;
    }

    /**
     * CALL n - Push address of next instruction onto stack and then jump to
     * address nn.
     *
     * nn = 16 bit address (LS byte first.)
     *
     * Flags affected:
     *
     * None
     */
    private void call_nn() {
        int value = getWordFromPClsFirst();

        pushSP(pc.getData());
        pc.load(value);
    }

    /**
     * CALL cc,n - Call address nn if following condition is true:
     *
     * nn = 16 bit value (LS first).
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
        int value = getWordFromPClsFirst();

        if (flag) {
            //condition met
            pushSP(pc.getData());

            pc.increment();
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
     * n = A,B,C,D,E,H,L
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
        flagH = checkHalfBorrow(reg.getData(), 0x01, false);

        reg.decrement();

        flagZ = reg.getData() == 0;
        flagN = true;
    }

    /**
     * DEC n - Decrement memory address (HL).
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
    private void dec_HL_pointer() {
        int oldValue, newValue, address;

        address = readCombinedRegisters(h, l);

        oldValue = memoryController.readByte(address);
        newValue = (oldValue - 1) & 0xFF;

        memoryController.writeByte(address, newValue);

        flagZ = newValue == 0;
        flagN = true;
        flagH = checkHalfBorrow(oldValue, 0x01, false);
    }

    /**
     * DEC nn - Decrement register nn.
     *
     * nn = BC,DE,HL
     *
     * Flags affected:
     *
     * None
     */
    private void dec_nn(Register regOne, Register regTwo) {
        int value = readCombinedRegisters(regOne, regTwo);
        value -= 1;

        loadCombinedRegisters(regOne, regTwo, value);
    }

    /**
     * DEC nn - Decrement register nn.
     *
     * nn = SP
     *
     * Flags affected:
     *
     * None
     */
    private void dec_SP() {
        sp.decrement();
    }

    /**
     * DI - Disable interrupts.
     *
     * Flags affected:
     *
     * None
     */
    private void di() {
        InterruptController.getInstance().disableInterrupt();
    }

    /**
     * EI - Enable interrupts.
     *
     * This instruction enables the interrupts but not immediately.
     *
     * Interrupts are enabled after the instruction after EI is executed.
     *
     *
     * Flags affected:
     *
     * None
     */
    private void ei() {
        pendingInterrupt = true;
    }

    /**
     * INC n - Increment register n.
     *
     * n = A,B,C,D,E,H,L
     *
     * Flags affected:
     *
     * Z - Set if result is zero.
     *
     * N - Reset.
     *
     * H - Set if carry from bit 3.
     *
     * C - Not affected.
     */
    private void inc_n(Register reg) {
        reg.increment();

        flagZ = checkCarry(reg.getData(), false);
        if (flagZ) {
            reg.setData(reg.getData() & 0xFF);
        }

        flagN = false;
        flagH = checkHalfCarry(reg.getData(), 0x01, false);
    }

    /**
     * INC n - Increment memory address n.
     *
     * n = (HL)
     *
     * Flags affected:
     *
     * Z - Set if result is zero.
     *
     * N - Reset.
     *
     * H - Set if carry from bit 3.
     *
     * C - Not affected.
     */
    private void inc_HL_pointer() {
        int address, newValue, oldValue;

        address = readCombinedRegisters(h, l);

        oldValue = memoryController.readByte(address);
        newValue = (oldValue + 1);

        memoryController.writeByte(address, newValue & 0xFF);

        flagZ = checkCarry(newValue, false);
        flagN = false;
        flagH = checkHalfCarry(oldValue, 0x01, false);
    }

    /**
     * INC nn - Increment register nn.
     *
     * n = BC,DE,HL,SP
     *
     * Flags affected:
     *
     * None
     */
    private void inc_nn(Register regOne, Register regTwo) {
        int value = readCombinedRegisters(regOne, regTwo);
        loadCombinedRegisters(regOne, regTwo, value + 1);
    }

    /**
     * JP n - Jump to address n.
     *
     * n = two byte immediate value. (LSByte first)
     *
     * Flags affected:
     *
     * None
     *
     */
    private void jp_n() {
        int address = getWordFromPClsFirst();
        pc.load(address);
    }

    /**
     * JP cc,n - Jump to address n if following condition is true:
     *
     * nn = two byte immediate value. (LSByte first.)
     *
     * cc = NZ, Jump if Z flag is reset.
     *
     * cc = Z, Jump if Z flag is set.
     *
     * cc = NC, Jump if C flag is reset.
     *
     * cc = C, Jump if C flag is set.
     *
     * Flags affected:
     *
     * None
     *
     */
    private void jp_cc_nn(boolean flag) {
        int address = getWordFromPClsFirst();

        if (flag) {
            pc.load(address);
            consumeClock(12);
        } else {
            consumeClock(16);
        }
    }

    /**
     * JP [HL] - Jump to address contained in HL.
     *
     * Flags affected:
     *
     * None
     */
    private void jp_HL() {
        int address = readCombinedRegisters(h, l);
        pc.load(address);
    }

    /**
     * JR n - Add n to current address and jump to it.
     *
     * n = one byte signed immediate value.
     *
     * Flags affected:
     *
     * None
     *
     */
    private void jr_r() {
        int address, addressValue;

        addressValue = memoryController.readByte(pc.getData());
        pc.increment();

        address = pc.getData();
        pc.increment();

        addressValue = getSignedInt(addressValue);
        address += addressValue;

        pc.load(address);
    }

    /**
     * JR cc,n - If following condition is true then add n to current address
     * and jump to it:
     *
     * n = one byte signed immediate value
     *
     * cc = NZ, Jump if Z flag is reset.
     *
     * cc = Z, Jump if Z flag is set.
     *
     * cc = NC, Jump if C flag is reset.
     *
     * cc = C, Jump if C flag is set.
     *
     * Flags affected:
     *
     * None
     *
     */
    private void jr_cc_n(boolean flag) {
        if (flag) {
            jr_r();
            consumeClock(12);
        } else {
            pc.increment();
            consumeClock(0);
        }
    }

    /**
     * HALT - Power down CPU until an interrupt occurs.
     *
     * Flags affected:
     *
     * None
     */
    private void halt() {
        isHalted = true;
    }

    /**
     * LD A,n - Put value n into A.
     *
     * n = A,B,C,D,E,H,L,(BC),(DE),(HL),(nnnn),#
     *
     * Flags affected:
     *
     * None
     *
     * @param value
     */
    private void ld_A_n(int value) {
        a.load(value);
    }

    /**
     * LD n,A - Put value A into n.
     *
     * n = A,B,C,D,E,H,L,(BC,(DE),(HL),(nnnn)
     *
     * Flags affected:
     *
     * None
     *
     */
    private void ld_n_A(Register regOne, Register regTwo, boolean toNextAddressInPc) {
        if (regTwo == null) {
            regOne.load(a.getData());
            consumeClock(4);
        } else {
            int address;

            if (toNextAddressInPc) {
                address = getWordFromPClsFirst();
                consumeClock(16);
            } else {
                address = readCombinedRegisters(regOne, regTwo);
                consumeClock(8);
            }

            memoryController.writeByte(address, a.getData());
        }
    }

    /**
     * LD A,[C] - Put value at address $FF00 + register C into A.
     *
     * Flags affected:
     *
     * None
     */
    private void ld_A_C() {
        int value = memoryController.readByte(c.getData() + 0xFF00);
        a.load(value);
    }

    /**
     * LD A,[HL+] - Same as LD A,[HLI].
     */
    private void ld_A_HLplus() {
        ld_A_HLi();
    }

    /**
     * LD A,[HL-] - Same as LD A,[HLD].
     */
    private void ld_A_HLminus() {
        ld_A_HLd();
    }

    /**
     * LD A,[HLI] - Put value at address HL into A. Increment HL.
     *
     * Flags affected:
     *
     * None
     */
    private void ld_A_HLi() {
        int oldValue = readCombinedRegisters(h, l);

        a.load(memoryController.readByte(oldValue));
        loadCombinedRegisters(h, l, oldValue + 1);
    }

    /**
     * LD A,[HLD] - Put value at address HL into A. Decrement HL.
     *
     * Flags affected:
     *
     * None
     */
    private void ld_A_HLd() {
        int oldValue = readCombinedRegisters(h, l);

        a.load(memoryController.readByte(oldValue));
        loadCombinedRegisters(h, l, oldValue - 1);
    }

    /**
     * LD [C],A - Put A into address $FF00 + register C.
     *
     * Flags affected:
     *
     * None
     *
     */
    private void ld_C_A() {
        memoryController.writeByte(c.getData() + 0xFF00, a.getData());
    }

    /**
     * LD [HL+],A - Same as LD [HLI],A.
     */
    private void ld_HLplus_A() {
        ld_A_HLi();
    }

    /**
     * LD [HL-],A - Same as LD [HLD],A.
     */
    private void ld_HLminus_A() {
        ld_HLd_A();
    }

    /**
     * LD [HLI],A - Put A into memory address HL. Increment HL.
     *
     * Flags affected:
     *
     * None
     */
    private void ld_HLi_A() {
        int address = readCombinedRegisters(h, l);

        memoryController.writeByte(address, a.getData());
        loadCombinedRegisters(h, l, address + 1);
    }

    /**
     * LD [HLD],A - Put A into memory address HL. Decrement HL.
     *
     * Flags affected:
     *
     * None
     *
     */
    private void ld_HLd_A() {
        int address = readCombinedRegisters(h, l);

        memoryController.writeByte(address, a.getData());
        loadCombinedRegisters(h, l, address - 1);
    }

    /**
     * TO READ/STORE FROM (HL) USE THE OTHER METHOD
     *
     * LD r1,r2 - Put value r2 into r1.
     *
     * r1,r2 = A,B,C,D,E,H,L
     *
     * Flags affected:
     *
     * None
     *
     */
    private void ld_r1_r2(Register r1, Register r2) {
        r1.load(r2.getData());
    }

    /**
     * TO READ/STORE NOT FROM (HL) USE THE OTHER METHOD
     *
     * LD r1,r2 - Put value r2 into r1.
     *
     * r1,r2 = A,B,C,D,E,H,L,(HL)
     *
     * Flags affected:
     *
     * None
     *
     */
    private void ld_r1_r2_HL_pointer(Register reg, boolean fromHL) {
        int address = readCombinedRegisters(h, l);
        if (fromHL) {
            reg.load(memoryController.readByte(address));
        } else {
            memoryController.writeByte(address, reg.getData());
        }
    }

    /**
     * LD n,nn - Put value nn into n.
     *
     * n = BC,DE,HL,SP
     *
     * nn = 16 bit immediate value
     *
     *
     *
     * Flags affected:
     *
     * None
     */
    private void ld_n_nn(Register regOne, Register regTwo) {
        int value = getWordFromPClsFirst();

        if (regTwo == null) {
            regOne.load(value);
        } else {
            loadCombinedRegisters(regOne, regTwo, value);
        }
    }

    /**
     * LD HL,[SP+n] - Put SP + n into HL.
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
     *
     */
    private void ld_HL_SPplusN() {
        int n, value;

        n = getSignedInt(memoryController.readByte(pc.getData()));
        pc.increment();

        value = sp.getData() + n;

        flagZ = false;
        flagN = false;

        flagH = checkHalfCarry(n, sp.getData(), true);
        flagC = checkCarry(value, true);

        loadCombinedRegisters(h, l, value);

    }

    /**
     * LD SP,HL - Put HL into Stack Pointer (SP).
     *
     * Flags affected:
     *
     * None
     */
    private void ld_SP_HL() {
        sp.load(readCombinedRegisters(h, l));
    }

    /**
     * LD [n],SP - Put Stack Pointer (SP) at address n.
     *
     * n = two byte immediate address
     *
     * Flags affected:
     *
     * None
     */
    private void ld_n_SP() {
        int address = getWordFromPClsFirst();

        memoryController.writeByte(address, (address & 0xFF));
        memoryController.writeByte(address + 1, address >> 8);
    }

    /**
     * LDD A,[HL] - Same as LD A,[HLD].
     */
    private void ldd_A_HL() {
        ld_A_HLd();
    }

    /**
     * LDD [HL],A - Same as LD [HLD],A.
     */
    private void ldd_HL_A() {
        ld_HLd_A();
    }

    /**
     * LDH [n],A - Put A into memory address $FF00 + n.
     *
     * n = one byte immediate value
     *
     * Flags affected:
     *
     * None
     */
    private void ldh_n_A() {
        int address = memoryController.readByte(pc.getData()) + 0xFF00;
        pc.increment();

        memoryController.writeByte(address, a.getData());
    }

    /**
     * LDH A,[n] - Put memory address $FF00 + n into A.
     *
     * n = one byte immediate value
     *
     * Flags affected:
     *
     * None
     */
    private void ldh_A_n() {
        int address = memoryController.readByte(pc.getData());
        pc.increment();

        a.load(memoryController.readByte(address + 0xFF00));
    }

    /**
     * LDHL SP,n - Same as LD HL,[SP+n]
     */
    private void ldhl_SP_n() {
        ld_HL_SPplusN();
    }

    /**
     * LDI A,[HL] - Same as LD A,[HLI].
     */
    private void ldi_A_HL() {
        ld_A_HLi();
    }

    /**
     * LDI [HL],A - Same as LD [HLI],A.
     *
     */
    private void ldi_HL_A() {
        ld_HLi_A();
    }

    /**
     * NOP - No operation.
     *
     * Flags affected:
     *
     * None
     */
    private void nop() {

    }

    /**
     * OR n - Logical OR n with register A, result in A.
     *
     * n = A,B,C,D,E,H,L,(HL),#
     *
     * Flags affected:
     *
     * Z - Set if result is zero.
     *
     * N - Reset.
     *
     * H - Reset.
     *
     * C - Reset.
     *
     */
    private void or_n(int value) {
        int result = a.getData() | value;

        flagZ = result == 0;
        flagN = false;
        flagH = false;
        flagC = false;

        a.load(value);
    }

    /**
     * POP nn - Pop two bytes off stack into register pair nn.
     *
     * Increment Stack Pointer (SP) twice.
     *
     * nn = AF,BC,DE,HL
     *
     * Flags affected:
     *
     * None
     *
     */
    private void pop_nn(Register upper, Register lower) {
        loadCombinedRegisters(upper, lower, popSP());
    }

    /**
     * PUSH nn - Push register pair nn onto stack.
     *
     * Decrement Stack Pointer (SP) twice.
     *
     *
     * nn = AF,BC,DE,HL
     *
     * Flags affected:
     *
     * None
     *
     */
    private void push_nn(Register upper, Register lower) {
        pushSP(readCombinedRegisters(upper, lower));
    }

    /**
     * RES b,r - Reset bit b in register r.
     *
     * b = 0-7, r = A,B,C,D,E,H,L
     *
     * Flags affected:
     *
     * None
     *
     */
    private void res_b_r(Register reg, int bytePos) {
        int mask, regValue;

        mask = ~(1 << bytePos);
        regValue = reg.getData();

        reg.load(mask & regValue);
    }

    /**
     * RES b,r - Reset bit b in register r.
     *
     * b = 0-7, r = (HL)
     *
     * Flags affected:
     *
     * None
     *
     */
    private void res_b_r_HL_pointer(int bytePos) {
        int mask, regValue;

        mask = ~(1 << bytePos);
        regValue = readCombinedRegisters(h, l);

        loadCombinedRegisters(h, l, regValue & mask);
    }

    /**
     * RET - Pop two bytes from stack & jump to that address.
     *
     * Flags affected:
     *
     * None
     */
    private void ret() {
        pc.load(popSP());
    }

    /**
     * RET cc - Return if following condition is true:
     *
     * cc = NZ, Return if Z flag is reset.
     *
     * cc = Z, Return if Z flag is set.
     *
     * cc = NC, Return if C flag is reset.
     *
     * cc = C, Return if C flag is set.
     *
     * Flags affected:
     *
     * None
     *
     * @param flag
     */
    private void ret_cc(boolean flag) {
        if (flag) {
            ret();
            consumeClock(20);
        } else {
            consumeClock(8);
        }
    }

    /**
     * RETI - Pop two bytes from stack & jump to that address then enable
     * interrupts.
     *
     * Flags affected:
     *
     * None
     *
     */
    private void reti() {
        pc.load(popSP());

        InterruptController.getInstance().enableInterrupt();
    }

    /**
     * RL n - Rotate n left through Carry flag.
     *
     * n = A,B,C,D,E,H,L
     *
     * Flags affected:
     *
     * Z - Set if result is zero.
     *
     * N - Reset.
     *
     * H - Reset.
     *
     * C - Contains old bit 7 data.
     *
     */
    private void rl_n(Register reg) {
        int flagCValue, oldRegValue, newRegValue;

        flagCValue = flagC ? 1 : 0;
        oldRegValue = reg.getData();
        newRegValue = ((oldRegValue << 1) | flagCValue) & 0xFF;

        flagC = (oldRegValue >> 7) == 1;

        reg.load(newRegValue);

        flagZ = newRegValue == 0;
        flagN = false;
        flagH = false;
    }

    /**
     * RL n - Rotate n left through Carry flag.
     *
     * n = (HL)
     *
     * Flags affected:
     *
     * Z - Set if result is zero.
     *
     * N - Reset.
     *
     * H - Reset.
     *
     * C - Contains old bit 7 data.
     *
     */
    private void rl_n_HL_pointer() {
        int flagCValue, oldRegValue, newValue, address;

        flagCValue = flagC ? 1 : 0;
        address = readCombinedRegisters(h, l);
        oldRegValue = memoryController.readByte(address);

        newValue = ((oldRegValue << 1) | flagCValue) & 0xFF;

        flagC = (oldRegValue >> 7) == 1;

        memoryController.writeByte(address, newValue);

        flagZ = newValue == 0;
        flagN = false;
        flagH = false;
    }

    /**
     * RLC n - Rotate n left. Old bit 7 to Carry flag.
     *
     * n = A,B,C,D,E,H,L
     *
     * Flags affected:
     *
     * Z - Set if result is zero.
     *
     * N - Reset.
     *
     * H - Reset.
     *
     * C - Contains old bit 7 data.
     */
    private void rlc_n(Register reg) {
        int bit7Value, oldRegValue, newRegValue;

        oldRegValue = reg.getData();
        bit7Value = (oldRegValue >> 7);
        newRegValue = ((oldRegValue << 1) | bit7Value) & 0xFF;

        reg.load(newRegValue);

        flagZ = newRegValue == 0;
        flagN = false;
        flagH = false;
        flagC = bit7Value == 1;
    }

    /**
     * RLC n - Rotate n left. Old bit 7 to Carry flag.
     *
     * n = A,B,C,D,E,H,L,(HL)
     *
     * Flags affected:
     *
     * Z - Set if result is zero.
     *
     * N - Reset.
     *
     * H - Reset.
     *
     * C - Contains old bit 7 data.
     */
    private void rlc_n_HL_pointer() {
        int bit7Value, oldRegValue, newValue, address;

        address = readCombinedRegisters(h, l);
        oldRegValue = memoryController.readByte(address);

        bit7Value = (oldRegValue >> 7);

        newValue = ((oldRegValue << 1) | bit7Value) & 0xFF;

        memoryController.writeByte(address, newValue);

        flagZ = newValue == 0;
        flagN = false;
        flagH = false;
        flagC = bit7Value == 1;
    }

    /**
     * RR n - Rotate n right through Carry flag.
     *
     * n = A,B,C,D,E,H,L
     *
     * Flags affected:
     *
     * Z - Set if result is zero.
     *
     * N - Reset.
     *
     * H - Reset.
     *
     * C - Contains old bit 0 data.
     */
    private void rr_n(Register reg) {
        int flagCValue, oldRegValue, newRegValue;

        flagCValue = flagC ? 1 : 0;
        oldRegValue = reg.getData();
        newRegValue = ((flagCValue << 7) | (oldRegValue >> 1)) & 0xFF;

        flagC = (oldRegValue & 0x01) == 1;

        reg.load(newRegValue);

        flagZ = newRegValue == 0;
        flagN = false;
        flagH = false;
    }

    /**
     * RR n - Rotate n right through Carry flag.
     *
     * n = (HL)
     *
     * Flags affected:
     *
     * Z - Set if result is zero.
     *
     * N - Reset.
     *
     * H - Reset.
     *
     * C - Contains old bit 0 data.
     */
    private void rr_n_HL_pointer() {
        int flagCValue, oldRegValue, newRegValue, address;

        address = readCombinedRegisters(h, l);
        oldRegValue = memoryController.readByte(address);

        flagCValue = flagC ? 1 : 0;
        newRegValue = ((flagCValue << 7) | (oldRegValue >> 1)) & 0xFF;

        memoryController.writeByte(address, newRegValue);

        flagZ = newRegValue == 0;
        flagN = false;
        flagH = false;
        flagC = (oldRegValue & 0x01) == 1;
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
        if (newValue > 0xFFFF || newValue < 0) {
            newValue &= 0xFFFF;
        }

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
     * @return true if carry NOT occurs, otherwise false
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
     * @return true if borrow NOT occurs, otherwise false
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
     * Least significant byte FIRST!
     *
     * @return Word read from addresses apponted in PC
     */
    private int getWordFromPClsFirst() {
        int lowerBits, upperBits;

        lowerBits = memoryController.readByte(pc.getData());
        pc.increment();
        upperBits = memoryController.readByte(pc.getData());
        pc.increment();

        return (upperBits << 8) | lowerBits;
    }

    /**
     * Push the informed value into the stack pointer.
     *
     * @param value Value to be added
     */
    private void pushSP(int value) {
        //write upper bits
        memoryController.writeByte(sp.getData(), value >> 8);
        sp.decrement();

        //write lower bits
        memoryController.writeByte(sp.getData(), value & 0xFF);
        sp.decrement();
    }

    /**
     * Pop a word (2x addresses combined to a 16 bit word) and increment SP by
     * 2. (LS first)
     *
     * return Value popped
     */
    private int popSP() {
        int lower, upper;

        lower = memoryController.readByte(sp.getData());
        sp.increment();

        upper = memoryController.readByte(sp.getData());
        sp.increment();

        return (upper << 8) | lower;

    }
//</editor-fold>

    public void printInfo() {
        System.out.println("-------------------------------");
        System.out.println("CPU INFO");
        System.out.println("");
    }

}
