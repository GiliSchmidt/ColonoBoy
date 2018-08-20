package cartridge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 *
 * @author Giliardi Schmidt
 */
public class Cartridge {

    private final int[] data;
    private final boolean isGBC;
    private final String title;
    private final boolean hasSgbFunctions;

    public Cartridge(String path) throws IOException {
        //read rom
        byte[] tempData = Files.readAllBytes(Paths.get(path));
        data = new int[tempData.length];

        for (int i = 0; i < tempData.length; i++) {
            data[i] = (tempData[i] & 0xFF);
        }

        //get title
        String tempTitle = "";
        for (int i = 0x0134; i < 0x0142; i++) {
            tempTitle += (char) this.data[i];
        }
        this.title = tempTitle;

        //is a gameboy collor game
        this.isGBC = data[0x143] == 0x80;

        //has super gameboy functions
        this.hasSgbFunctions = data[0x0146] == 0x03;
    }

    public int[] getData() {
        return data;
    }

    public void printInfo() {
        System.out.println("-------------------------------");
        System.out.println("ROM INFO");
        System.out.println(
                "Size: " + data.length
                + "\nTitle: " + title
                + "\nHas Super Gameboy function: " + hasSgbFunctions
                + "\nIs GBC: " + isGBC);
    }
}
