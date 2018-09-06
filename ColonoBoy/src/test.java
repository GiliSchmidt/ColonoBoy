
import cpu.Register;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author gilis
 */
public class test {

    public static void main(String[] args) {
        int a = 0xFF + 4;

        System.out.println("A is " + a);

        a &= 0b1111_1111;

        System.out.println("A is " + a);

        a = 0b1111_1111;

        System.out.println(Integer.toBinaryString(a <<= 8));
        int b = 0b1111_1111;

        System.out.println(Integer.toBinaryString(a ^ b));

        System.out.println(Integer.toBinaryString(readCombinedRegisters(0b1111_1111, 0b1111_1111)));

        System.out.println("----");

        a = 16;
        b = 3;
        System.out.println(((((a & 0xf) + (b & 0xf)) & 0x10) == 0x10));
        System.out.println(Integer.toBinaryString(b + a));

        int d = 0b11111111_11111111;

        //System.out.println(Integer.toBinaryString(d <<8));
        int value = 150;

        value = -((~value + 1) & 255);

        System.out.println(value);
        System.out.println(Integer.toBinaryString(value));

        System.out.println("-------------------------");

        value = 0b1111_1111;

        System.out.println(Integer.toBinaryString(value));

        System.out.println(Integer.toBinaryString(value >> 4));

        System.out.println((value >> 4) & 0x01);

        int q = 1 << 0;

        System.out.println(Integer.toBinaryString(~q));

        System.out.println(Integer.toBinaryString(~q & value));

        value = 0b1111_1110;

        System.out.println(Integer.toBinaryString(value));

        value = ((value << 1 | value << 7) | 0x00) & 0xFF;

        System.out.println(Integer.toBinaryString(value));

        int newRegValue, oldRegValue = 0b1111_0000, flagCValue = 0;

        newRegValue = ((flagCValue << 7) | (oldRegValue >> 1)) & 0xFF;
        System.out.println(Integer.toBinaryString(newRegValue));

    }

    public static int readCombinedRegisters(int upper, int lower) {
        return (upper << 8) | lower;
    }
}
