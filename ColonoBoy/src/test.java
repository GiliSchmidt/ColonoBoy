
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
        System.out.println((((( a& 0xf) + (b & 0xf)) & 0x10) == 0x10));
        System.out.println(Integer.toBinaryString(b+a));
        
        int d = 0b11111111_11111111;
        
        //System.out.println(Integer.toBinaryString(d <<8));
        
        int value = 150;
        
        value  = -((~value + 1) & 255);
        
        System.out.println(value);
        System.out.println(Integer.toBinaryString(value));
        
    }

    public static int readCombinedRegisters(int upper, int lower) {
        return (upper << 8) | lower;
    }
}
