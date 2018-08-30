
package cpu;

/**
 *
 * @author Giliardi Schmidt
 */
public class Register {

    private int data;

    public int getData() {
        return data;
    }

    public void setData(int data) {
        this.data = data;
    }
    
    public void add(int value){
        this.data += value;
    }
    
    public void load(int value){
        this.data = value;
    }
}
