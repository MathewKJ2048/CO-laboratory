import java.util.List;
import java.util.ArrayList;


public class Constants
{
    //keywords
    static class Keyword
    {
        public String[] words;
        public Keyword(String[] w)
        {
            words = w;
        }
        public boolean contains(String token)
        {
            for(int i=0;i<words.length;i++)if(words[i].equals(token))return true;
            return false;
        }
        public boolean is_contained_in(String line)
        {
            for(int i=0;i<words.length;i++)if(line.indexOf(words[i]) != -1)return true;
            return false;
        }
        public int start_of_first_instance_in(String line)
        {
            int m_id = -1;
            for(int i=0;i<words.length;i++)
            {
                int id = line.indexOf(words[i]);
                if(id!=-1)
                {
                    if(m_id==-1)m_id=id;
                    else if(m_id>id)m_id=id;
                }
            }
            return m_id;
        }
        public int end_of_first_instance_in(String line)
        {
            int m_id = -1;
            int length = 0;
            for(int i=0;i<words.length;i++)
            {
                int id = line.indexOf(words[i]);
                if(id!=-1)
                {
                    if(length == 0  ||  m_id>id)
                    {
                        length = words[i].length();
                        m_id = id;
                    }
                }
            }
            return m_id+length;
        }
    }
    //
    public static final String MULTI_LINE_START = "#*";
    public static final String MULTI_LINE_END = "*#";
    public static final String SINGLE_LINE = "#";
    public static final Keyword DATA = new Keyword(new String[]{".data"});
    public static final Keyword CODE = new Keyword(new String[]{".code",".text"});
    public static final char LABEL_TERMINATOR = ':';
    public static final char DATATYPE_INITIATOR = '.';
    public static final char ARGUMENT_SEPARATOR = ',';
    //
    public static final String WORD = "word";
    public static final String SHORT = "half";
    public static final String BYTE = "byte";
    public static final String SPACE = "space";
    public static final String ALIGN = "align";
    //
    static class Command
    {
        String name;
        char type;
        public Command(String name, char type)
        {
            this.name = name;
            this.type = type;
        }
    }
    public static List<Command> commands = get_commands();
    private static List<Command> get_commands()
    {
        List<Command> l = new ArrayList<Command>();
        //R-type
        l.add(new Command(ADD,R_TYPE));
        l.add(new Command(SUB,R_TYPE));
        l.add(new Command(AND,R_TYPE));
        l.add(new Command(OR,R_TYPE));
        l.add(new Command(SLT,R_TYPE));
        l.add(new Command(SLL,R_TYPE));
        l.add(new Command(SLTU,R_TYPE));
        l.add(new Command(XOR,R_TYPE));
        l.add(new Command(SRL,R_TYPE));
        l.add(new Command(SRA,R_TYPE));
        
        //I-type
        l.add(new Command(ADDI,I_TYPE));
        l.add(new Command(ANDI,I_TYPE));
        l.add(new Command(ORI,I_TYPE));
        l.add(new Command(SLTIU,I_TYPE));
        l.add(new Command(SLTI,I_TYPE));
        l.add(new Command(XORI,I_TYPE));
        l.add(new Command(LW,I_TYPE));
        
        //B-type
        l.add(new Command(BEQ,B_TYPE));
        l.add(new Command(BNE,B_TYPE));
        l.add(new Command(BLT,B_TYPE));
        l.add(new Command(BGE,B_TYPE));
        l.add(new Command(BGEU,B_TYPE));
        l.add(new Command(BLTU,B_TYPE));
        
        //J-type
        l.add(new Command(JAL,J_TYPE));
        
        //S-type
        l.add(new Command(SW,S_TYPE));
        
        //pseudo
        l.add(new Command(LI,PSEUDO_TYPE));
        l.add(new Command(MOV,PSEUDO_TYPE));
        
        return l;
    }
    public static boolean is_command(String s)
    {
        for(int i=0;i<commands.size();i++)if(commands.get(i).name.equals(s))return true;
        return false;
    }
    public static char get_type(String command)
    {
        for(int i=0;i<commands.size();i++)if(commands.get(i).name.equals(command))return commands.get(i).type;
        return 0;
    }
    //
    public static final char R_TYPE = 'R';
    public static final String ADD = "add";
    public static final String SUB = "sub";
    public static final String AND = "and";
    public static final String OR = "or";
    public static final String SRA = "sra";
    public static final String SRL = "srl";
    public static final String XOR = "xor";
    public static final String SLTU = "sltu";
    public static final String SLT = "slt";
    public static final String SLL = "sll";
    
    public static final char I_TYPE = 'I';
    public static final String ANDI = "andi";
    public static final String ORI = "ori";
    public static final String XORI = "xori";
    public static final String SLTI = "slti";
    public static final String SLTIU = "sltiu";
    public static final String ADDI = "addi";
    public static final String LW = "lw";
    
    public static final char S_TYPE = 'S';
    public static final String SW = "sw";
    
    public static final char U_TYPE = 'U';
    
    public static final char B_TYPE = 'B';
    public static final String BEQ = "beq";
    public static final String BNE = "bne";
    public static final String BLT = "blt";
    public static final String BGE = "bge";
    public static final String BLTU = "bltu";
    public static final String BGEU = "bgeu";
    
    public static final char J_TYPE = 'J';
    public static final String JAL = "jal";
    
    public static final char PSEUDO_TYPE = 'P';
    public static final String LI = "li";
    public static final String MOV = "mov";
    
    static class Register
    {
        String name;
        int address;
        public Register(String name, int address)
        {
            this.name = name;
            this.address = address;
        }
    }
    public static List<Register> registers = get_registers();
    private static List<Register> get_registers()
    {
        List<Register> r = new ArrayList<Register>();
        r.add(new Register("$zero",0));
        r.add(new Register("$ra",1));
        r.add(new Register("$sp",2));
        r.add(new Register("$gp",3));
        r.add(new Register("$tp",4));
        r.add(new Register("$t0",5));
        r.add(new Register("$t1",6));
        r.add(new Register("$t2",7));
        r.add(new Register("$s0",8));
        r.add(new Register("$fp",8));
        r.add(new Register("$s1",9));
        r.add(new Register("$a0",10));
        r.add(new Register("$a1",11));
        r.add(new Register("$a2",12));
        r.add(new Register("$a3",13));
        r.add(new Register("$a4",14));
        r.add(new Register("$a5",15));
        r.add(new Register("$a6",16));
        r.add(new Register("$a7",17));
        r.add(new Register("$s2",18));
        r.add(new Register("$s3",19));
        r.add(new Register("$s4",20));
        r.add(new Register("$s5",21));
        r.add(new Register("$s6",22));
        r.add(new Register("$s7",23));
        r.add(new Register("$s8",24));
        r.add(new Register("$s9",25));
        r.add(new Register("$s10",26));
        r.add(new Register("$s11",27));
        r.add(new Register("$t3",28));
        r.add(new Register("$t4",29));
        r.add(new Register("$t5",30));
        r.add(new Register("$t6",31));
        return r;
    }
    public static int address_of(String register)
    {
        for(int i=0;i<registers.size();i++)if(register.equals(registers.get(i).name))return registers.get(i).address;
        return -1;
    }
    
    
    
    
    
    
    
    
    
    
    public static boolean is_number(char ch)
    {
        if('0'<=ch && ch<='9')return true;
        return false;
    }
    public static boolean is_alphabet(char ch)
    {
        if('a'<=ch&&ch<='z' || 'A'<=ch&&ch<='Z')return true;
        return false;
    }
    public static boolean is_identifier(String s)
    {
        if(s.length() == 0)return false;
        if(is_number(s.charAt(0)))return false;
        for(int i=0;i<s.length();i++)
        {
            char d = s.charAt(i);
            if(!(is_number(d)||is_alphabet(d)||d=='_'||d=='.'))return false;
        }
        return true;
    }
    public static boolean is_label(String s)
    {
        if(s.charAt(s.length()-1) == LABEL_TERMINATOR && is_identifier(s.substring(0,s.length()-1)))return true;
        return false;
    }
}
