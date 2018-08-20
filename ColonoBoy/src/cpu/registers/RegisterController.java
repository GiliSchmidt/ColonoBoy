package cpu.registers;

/**
 *
 * @author Giliardi Schmidt
 */
public class RegisterController {

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

    public RegisterController() {
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
    }

    //<editor-fold defaultstate="collapsed" desc="GETs and SETs">
    public Register getA() {
        return a;
    }

    public Register getB() {
        return b;
    }

    public Register getD() {
        return d;
    }

    public Register getH() {
        return h;
    }

    public Register getF() {
        return f;
    }

    public Register getC() {
        return c;
    }

    public Register getE() {
        return e;
    }

    public Register getL() {
        return l;
    }

    public Register getSp() {
        return sp;
    }

    public Register getPc() {
        return pc;
    }
//</editor-fold>

    public void printInfo() {
        System.out.println("REGISTERS"
                + "\n A: " + Integer.toHexString(a.getData())
                + "\n B: " + Integer.toHexString(b.getData())
                + "\n D: " + Integer.toHexString(d.getData())
                + "\n H: " + Integer.toHexString(h.getData())
                + "\n F: " + Integer.toHexString(f.getData())
                + "\n C: " + Integer.toHexString(c.getData())
                + "\n E: " + Integer.toHexString(e.getData())
                + "\n L: " + Integer.toHexString(l.getData())
                + "\n SP: " + Integer.toHexString(sp.getData())
                + "\n PC: " + Integer.toHexString(pc.getData()));
    }

}
