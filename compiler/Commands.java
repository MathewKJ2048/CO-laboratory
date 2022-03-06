
public class Commands
{
    public static String sw(int source_register_address, int destination_register_address, long offset)
    {
        String opcode = "0000011";
        String funct3 = "100";
        String immediate = to_binary(offset,12);
        String source = to_binary(source_register_address,5);
        String destination = to_binary(destination_register_address,5);
        String s = extract_bits(11,5,immediate,11,0)+source+destination+funct3+extract_bits(4,0,immediate,11,0)+opcode; 
        return s;
    }
    //
    public static String I_type(int source_register_address, int destination_register_address, long value, String funct3)
    {
        String opcode = "0010011";
        String immediate = to_binary(value,12);
        String source = to_binary(source_register_address,5);
        String destination = to_binary(destination_register_address,5);
        String s = immediate+source+funct3+destination+opcode;
        return s;
    }
    public static String addi(int source_register_address, int destination_register_address, long value)
    {
        return I_type(source_register_address,destination_register_address,value,"000");
    }
    public static String ori(int source_register_address, int destination_register_address, long value)
    {
        return I_type(source_register_address,destination_register_address,value,"110");
    }
    public static String andi(int source_register_address, int destination_register_address, long value)
    {
        return I_type(source_register_address,destination_register_address,value,"111");
    }
    public static String lw(int source_register_address, int destination_register_address, long value)
    {
        return I_type(source_register_address,destination_register_address,value,"010");
    }
    //
    public static String jal(int destination_register_address, long offset)
    {
        String opcode = "1101111";
        String destination = to_binary(destination_register_address,5);
        String immediate = to_binary(offset,20);
        String s = extract_bits(20,20,immediate,20,1)+extract_bits(10,1,immediate,20,1)+extract_bits(11,11,immediate,20,1)+extract_bits(19,12,immediate,20,1)
                    +destination+opcode;
        return s;
    }
    
    public static String R_type(int source_register_address_1,int source_register_address_2, int destination_register_address, String funct3, String funct7)
    {
        String opcode = "0110011";
        String source_1 = to_binary(source_register_address_1,5);
        String source_2 = to_binary(source_register_address_2,5);
        String destination = to_binary(destination_register_address,5);
        String s = funct7+source_1+source_2+funct3+destination+opcode;
        return s;
    }
    public static String add(int source_register_address_1,int source_register_address_2, int destination_register_address)
    {
        return R_type(source_register_address_1,source_register_address_2,destination_register_address,"000","0000000");
    }
    public static String sub(int source_register_address_1,int source_register_address_2, int destination_register_address)
    {
        return R_type(source_register_address_1,source_register_address_2,destination_register_address,"000","0100000");
    }
    public static String or(int source_register_address_1,int source_register_address_2, int destination_register_address)
    {
        return R_type(source_register_address_1,source_register_address_2,destination_register_address,"110","0000000");
    }
    public static String and(int source_register_address_1,int source_register_address_2, int destination_register_address)
    {
        return R_type(source_register_address_1,source_register_address_2,destination_register_address,"111","0000000");
    }
    
    public static String B_type(int source_register_address_1,int source_register_address_2, long value, String funct3)
    {
        String opcode = "1100011";
        String source_1 = to_binary(source_register_address_1,5);
        String source_2 = to_binary(source_register_address_2,5);
        String immediate = to_binary(value,12);
        String s = extract_bits(12,12,immediate,12,1)+extract_bits(10,5,immediate,12,1)
                    +source_2+source_1+funct3+extract_bits(4,1,immediate,12,1)+extract_bits(11,11,immediate,12,1)+opcode;
        return s;
    }
    public static String beq(int source_register_address_1,int source_register_address_2, long value)
    {
        return B_type(source_register_address_1,source_register_address_2,value,"000");
    }
    public static String bne(int source_register_address_1,int source_register_address_2, long value)
    {
        return B_type(source_register_address_1,source_register_address_2,value,"001");
    }
    public static String blt(int source_register_address_1,int source_register_address_2, long value)
    {
        return B_type(source_register_address_1,source_register_address_2,value,"100");
    }
    public static String bge(int source_register_address_1,int source_register_address_2, long value)
    {
        return B_type(source_register_address_1,source_register_address_2,value,"101");
    }
    public static String bltu(int source_register_address_1,int source_register_address_2, long value)
    {
        return B_type(source_register_address_1,source_register_address_2,value,"110");
    }
    public static String bgeu(int source_register_address_1,int source_register_address_2, long value)
    {
        return B_type(source_register_address_1,source_register_address_2,value,"111");
    }
    
    static String to_binary(long n, int length)
    {
        long p = (long)Math.pow(2,length);
        long N = (p + n)%p;
        String bin = Long.toBinaryString(N);
        String b = bin;
        for(int i=1;i<=length-bin.length();i++)b="0"+b;
        return b;
    }
    
    public static String extract_bits( int l, int r, String b, int MSB_index, int LSB_index)// returns b[l:r] where b is b[MSBi:LSBi]
    {
        String sb = "";
        for(int i=l;i>=r;i--)
        {
            sb+=b.charAt(MSB_index - i);
        }
        return sb;
        //charAt(0) gives MSBi
        //charAt(i) gives MSBi - i
        //charAt(j) gives x
        //x=MSbi - j
        // j = MSBi-x
        // thus CharAt(MSBi-x) gives xth char
    }
    /*
    public static void test()
    {
        for(int i=-4; i<=4; i++)System.out.println(i+":"+to_binary(i,3));
    }*/
}
