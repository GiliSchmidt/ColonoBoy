package cpu;

import cpu.registers.RegisterController;

/**
 *
 * @author Giliardi Schmidt
 */
public class Cpu {

    private final RegisterController registerController;
    private final FlagController flagController;
    private final MemoryController memoryController;

    public Cpu() {
        this.registerController = new RegisterController();
        this.flagController = new FlagController();
        this.memoryController = new MemoryController();

        init();
    }

    private void init() {
        //at startup the PC is set to 0x100
        this.registerController.getPc().setData(0x100);
        //at startup the PC is set to 0xFFFE
        this.registerController.getSp().setData(0xFFFE);
    }

    public void executeCicle() {
        // fetch(); decode(); execute();
        int opcode = fetch();
        Runnable operation = decode(opcode);
        execute(operation);
    }

    private int fetch() {
        int opcode = memoryController.read(registerController.getPc().getData());
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

    public void printInfo() {
        System.out.println("-------------------------------");
        System.out.println("CPU INFO");
        flagController.printInfo();
        System.out.println("");
        registerController.printInfo();
    }
}
