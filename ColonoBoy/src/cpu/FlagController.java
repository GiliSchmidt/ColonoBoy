package cpu;

/**
 *
 * @author Giliardi Schmidt
 *
 */
public class FlagController {

    // _________________________________
    // | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
    // | Z | N | H | C |   |   |   |   |
    // |___|___|___|___|___|___|___|___|
    // Z = Zero: set if last operation resulted in zero
    // N = Operation: set if last operation was subtraction
    // H = Half-carry: set if last operation's result's lower half overflowed past 15
    // C = Carry: set if last operation produced result greater than 255 for adds or less than 0 for subtracts
    //
    //Each register is 1 bit!
    private boolean z;
    private boolean n;
    private boolean h;
    private boolean c;

    public void setZ() {
        z = true;
    }

    public void clearZ() {
        z = false;
    }

    public void setN() {
        n = true;
    }

    public void clearN() {
        n = false;
    }

    public void setH() {
        h = true;
    }

    public void clearH() {
        h = false;
    }

    public void setC() {
        c = true;
    }

    public void clearC() {
        c = false;
    }

    public void printInfo() {
        System.out.println("FLAGS"
                + "\n Z: " + z
                + "\n N: " + n
                + "\n H: " + h
                + "\n C: " + c
        );
    }

}
