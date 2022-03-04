
public class Commands
{
    
    
    public static String sw(int source_register_address, int destination_register_address, int offset)
    {
        String s = "";
        String o = to_binary(offset,12);
        s = o.substring(0,7)+to_binary(source_register_address,5)+to_binary(destination_register_address,5)+"100"+o.substring(7,12)+"0000011";
        return s;
    }
    public static String lui(int address, long value)
    {
        String s = "001011"; //opcode
        s = to_binary(value,21)+to_binary(address,5)+s;
        return s;
    }
    /*
    public static String add()
    {
        
    }
    public static String sub()
    {
        
    }
    public static String j()
    {
    
    }*/
    static String to_binary(long n, int length)
    {
        long p = (long)Math.pow(2,length);
        long N = (p + n)%p;
        String bin = Long.toBinaryString(N);
        String b = bin;
        for(int i=1;i<=length-bin.length();i++)b="0"+b;
        return b;
    }
    /*
    public static void test()
    {
        for(int i=-4; i<=4; i++)System.out.println(i+":"+to_binary(i,3));
    }*/
}
