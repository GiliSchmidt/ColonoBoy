package cpu;

/**
 *
 * @author Giliardi Schmidt
 */
public class InterruptController {

    private static InterruptController instance;

    private InterruptController() {

    }
    
    public void enableInterrupt(){
        //TODO: everything
    }

    public static InterruptController getInstance() {
        if (instance == null) {
            instance = new InterruptController();
        }

        return instance;
    }

    void disableInterrupt() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
