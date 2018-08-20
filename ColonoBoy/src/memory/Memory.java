package memory;

/**
 *
 * @author Giliardi Schmidt
 */
public class Memory {

    byte[] memory = new byte[0x2000];

    public void write(byte[] data, int pos) {
        for (int i = 0; i < data.length; i++) {
            memory[pos++] = data[i];
        }
    }

    public void write(byte data, int pos) {
        memory[pos] = data;
    }

    public byte read(int pos) {
        return memory[pos];
    }

    
}
