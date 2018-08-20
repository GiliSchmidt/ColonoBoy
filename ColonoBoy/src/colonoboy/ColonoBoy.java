package colonoboy;

import cpu.Cpu;
import memory.Memory;
import cartridge.Cartridge;
import java.io.IOException;

/**
 *
 * @author Giliardi Schmidt
 */
public class ColonoBoy {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        String path = "C:\\Users\\gilis\\Desktop\\Tetris-USA.gb";

        Cartridge rom = new Cartridge(path);
        Memory memory = new Memory();
        Cpu cpu = new Cpu();

        //memory.write(rom.getData(), 0);

        rom.printInfo();
        cpu.printInfo();

    }

}
