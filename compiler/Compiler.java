import java.nio.file.*;
import java.util.*;

public class Compiler
{
    private final int memory_start_data;
    private int data_current;
    private final int memory_start_code;
    private int code_current;
    private List<Line> l;
    
    private class Label
    {
        String name;
        int address;
        Label(String name, int address)
        {
            this.name = name;
            this.address = address;
        }
    }
    public int get_address_code(String name)
    {
        for(int i=0;i<l_lc.size();i++)if(l_lc.get(i).name.equals(name))return l_lc.get(i).address;
        return -1;
    }
    public int get_address_data(String name)
    {
        for(int i=0;i<l_ld.size();i++)if(l_ld.get(i).name.equals(name))return l_ld.get(i).address;
        return -1;
    }
    private List<Label> l_ld;
    private List<Label> l_lc;
    private class Instruction
    {
        String contents;
        int address;
        Instruction(String contents, int address)
        {
            this.contents = contents;
            this.address = address;
        }
    }
    private List<Instruction> l_pc; 
    
    private class Line
    {
        String contents;
        int number;
    }
    
    private class Source_stream
    {
        String stream;
        List<Integer> numbers;
        Source_stream()
        {
            stream = "";
            numbers = new ArrayList<Integer>();
        }
        void append(String s, int n)
        {
            stream = stream+s;
            for(int i=0;i<s.length();i++)numbers.add(n);
        }
        void print()
        {
            if(stream.length() == 0)return;
            System.out.print(numbers.get(0)+"|");
            System.out.print(stream.charAt(0));
            for(int i=1;i<stream.length();i++)
            {
                if(numbers.get(i-1)!=numbers.get(i))System.out.print("\n"+numbers.get(i)+"|");
                System.out.print(stream.charAt(i));
            }
        }
    }
    private Source_stream code_section;
    private Source_stream data_section;
    
    Compiler(int memory_start_data, int memory_start_code) throws Exception
    {
        this.memory_start_data = memory_start_data;
        if(memory_start_code%4!=0)throw new Exception("Misaligned memory");
        this.memory_start_code = memory_start_code;
        this.data_current = memory_start_data;
        this.code_current = memory_start_code;
        this.l = new ArrayList<Line>();
        this.code_section = new Source_stream();
        this.data_section = new Source_stream();
        this.l_ld = new ArrayList<Label>();
        this.l_lc = new ArrayList<Label>();
        this.l_pc = new ArrayList<Instruction>();
    }
    public void compile(Path source, Path binary) throws Exception,java.io.IOException
    {
        List<String> l_raw = new ArrayList<String>();
        l_raw = Files.readAllLines(source);
        this.l = new ArrayList<Line>();
        for(int i = 0;i< l_raw.size();i++)
        {
            Line li = new Line();
            li.number = i+1;
            li.contents = l_raw.get(i);
            l.add(li);
        }
        print();
        scrub();
        print();
        
        locate_blocs();
        
        System.out.println("\n\ndata segment:");
        data_section.print();
        System.out.println("\n\ncode segment:");
        code_section.print();
        
        System.out.println("-------------------------------------------------------------");
        process_data();
        process_code();
        for(int i=0;i<l_ld.size();i++)
        {
            System.out.println(l_ld.get(i).name+" refers to data location:"+l_ld.get(i).address);
        }
        for(int i=0;i<l_lc.size();i++)
        {
            System.out.println(l_lc.get(i).name+" refers to PC location:"+l_lc.get(i).address);
        }
        System.out.println("-------------------------------------------------------------");
        for(int i=0;i<l_pc.size();i++)
        {
            System.out.println(l_pc.get(i).address+"|"+l_pc.get(i).contents);
        }
        
        //writing file
        String file = "";
        for(int i=0;i<l_pc.size();i++)
        {
            file+=l_pc.get(i).contents+" ";
        }
        Files.write(binary,file.getBytes());
    }
    private void scrub()
    {
        remove_multi_line_comments();
        remove_single_line_comments();
        replace_tab_with_space();
        trim();
        remove_empty_lines();
    }
    private void remove_multi_line_comments()
    {
        boolean f = false;
        for(int i = 0; i<l.size(); i++)
        {
            String c = l.get(i).contents;
            int start = c.indexOf(Constants.MULTI_LINE_START);
            int end = c.indexOf(Constants.MULTI_LINE_END);
            int size_start = Constants.MULTI_LINE_START.length();
            int size_end = Constants.MULTI_LINE_END.length();
            if(f && end==-1)
            {
                l.get(i).contents = "";
                System.out.println("clear");
            }
            if(start == -1 && end == -1)
            {
                continue;   
            }
            else if(start != -1 && end == -1)
            {
                f = true;
                String s = "";
                for(int j=0;j<start;j++)
                {
                    s+=c.charAt(j);
                }
                l.get(i).contents = s;
            }
            else if(start == -1 && end != -1)
            {
                int last_end = end;
                f = false;
                String s = "";
                for(int j=last_end+size_end;j<c.length();j++)
                {
                    s+=c.charAt(j);
                }
                l.get(i).contents = s;
            }
            else
            {
                int next_end = c.indexOf(Constants.MULTI_LINE_END, start);
                if(next_end != -1)
                {  
                    String s = "";
                    for(int j=0;j<start;j++)s+=c.charAt(j);
                    for(int j=end+size_end;j<c.length();j++)s+=c.charAt(j);
                    l.get(i).contents = s;
                    i--;
                }
                else
                {
                    f=true;
                    int last_end = end;
                    String s = "";
                    for(int j=last_end+size_end;j<start;j++)s+=c.charAt(j);
                    l.get(i).contents = s;
                }
            }
        }
    }
    private void remove_single_line_comments()
    {
        for(int i=0;i<l.size();i++)
        {
            String c = l.get(i).contents;
            int start = c.indexOf(Constants.SINGLE_LINE);
            if(start != -1)
            {
                String s="";
                for(int j=0;j<start;j++)s+=c.charAt(j);
                l.get(i).contents = s;
            }
        }
    }
    private void remove_empty_lines()
    {
        for(int i=0;i<l.size();i++)
        {
            if(l.get(i).contents.equals(""))
            {
                l.remove(i);
                i--;
            }
        }
    }
    private void replace_tab_with_space()
    {
        for(int i = 0;i<l.size();i++)
        {
            String s = "";
            String c = l.get(i).contents;
            for(int j=0;j<c.length();j++)
            {
                char d = c.charAt(j);
                if(d=='\t')s+=" ";
                else s+=d;
            }
        }
    }
    private void trim()
    {
        for(int i = 0;i<l.size();i++)
        {
            String c = l.get(i).contents;
            c = c.trim();
            l.get(i).contents = c;
        }
    }
    public void print()
    {
        for(int i=0;i<l.size();i++)
        {
            System.out.println((l.get(i).number<10?"0":"")+(l.get(i).number)+"|"+l.get(i).contents+"|");
        }
    }
    public void locate_blocs() throws Exception
    {
        Source_stream source_full = new Source_stream();
        for(int i=0;i<l.size();i++)
        {
            source_full.append(l.get(i).contents+" ",l.get(i).number);
        }
        
        int data_start = Constants.DATA.start_of_first_instance_in(source_full.stream);
        int data_end = Constants.DATA.end_of_first_instance_in(source_full.stream);
        int code_start = Constants.CODE.start_of_first_instance_in(source_full.stream);
        int code_end = Constants.CODE.end_of_first_instance_in(source_full.stream);
        if(code_start == -1)throw new Exception("missing code");
        if(data_start == -1)//no data segment
        {
            for(int i=code_end;i<source_full.stream.length();i++)code_section.append(source_full.stream.charAt(i)+"",source_full.numbers.get(i));
        }
        else
        {
            if(data_start < code_start)//data section before code section
            {
                for(int i=data_end;i<code_start;i++)data_section.append(source_full.stream.charAt(i)+"",source_full.numbers.get(i));
                for(int i=code_end;i<source_full.stream.length();i++)code_section.append(source_full.stream.charAt(i)+"",source_full.numbers.get(i));
            }
            else if(code_start < data_start)// code section before data section
            {
                for(int i=code_end;i<data_start;i++)code_section.append(source_full.stream.charAt(i)+"",source_full.numbers.get(i));
                for(int i=data_end;i<source_full.stream.length();i++)data_section.append(source_full.stream.charAt(i)+"",source_full.numbers.get(i));
            }
            else throw new Exception("Coinciding section error: unable to tell data and code apart");
        }
    }
    
    public void process_data() throws Exception
    {
        if(data_section.stream.equals(""))return;
            Scanner sc = new Scanner(data_section.stream);
            while(sc.hasNext())
            {
                String t = sc.next();
                if(t.charAt(t.length()-1) == Constants.LABEL_TERMINATOR)
                {
                    String label = t.substring(0,t.length()-1);
                    if(!Constants.is_identifier(label))throw new Exception("Incorrect identifier");
                    this.l_ld.add(new Label(label,data_current));
                }
                else if(t.charAt(0) == Constants.DATATYPE_INITIATOR)
                {
                    String type = t.substring(1,t.length());
                    if(type.equals(Constants.WORD))
                    {
                        if(data_current%4!=0)throw new Exception("misaligned memory");
                        if(sc.hasNextInt())
                        {
                            long initial_value = sc.nextInt();
                            l_pc.add(new Instruction(Commands.addi(0,5,initial_value),code_current));
                            code_current+=4;
                            l_pc.add(new Instruction(Commands.sw(5,0,data_current),code_current));
                            code_current+=4;
                            l_pc.add(new Instruction(Commands.andi(0,5,0),code_current));
                            code_current+=4;
                        }
                        data_current+=4;
                    }
                    else if(type.equals(Constants.BYTE))
                    {
                        //check if literal is present and load it
                        if(data_current%1!=0)throw new Exception("misaligned memory");
                        data_current+=1;
                    }
                    else if(type.equals(Constants.SHORT))
                    {
                        //check if literal is present and load it
                        if(data_current%2!=0)throw new Exception("misaligned memory");
                        data_current+=2;
                    }
                    else if(type.equals(Constants.SPACE))
                    {
                        try
                        {
                            int n = sc.nextInt();
                            data_current+=n;
                        }
                        catch(Exception e)
                        {
                            throw new Exception("literal missing");
                        }
                    }
                    else if(type.equals(Constants.ALIGN))
                    {
                        try
                        {
                            int n = sc.nextInt();
                            int p = (int)Math.pow(2,n);
                            while(data_current%p!=0)data_current++;
                        }
                        catch(Exception e)
                        {
                            throw new Exception("literal missing");
                        }
                    }
                    else
                    {
                        throw new Exception("unrecognized type");
                    }
                }
            }
        
    }
    public void process_code() throws Exception
    {
        Scanner sc = new Scanner(code_section.stream);
        int PC = code_current;
        while(sc.hasNext())
        {
            String token = sc.next();
            if(Constants.is_command(token))
            {
                PC+=4;
                if(Constants.get_type(token) == Constants.PSEUDO_TYPE)System.out.println("warning");
                if(token.equals(Constants.LI) || token.equals(Constants.MOV)){PC+=4;System.out.println("correction made");}
            }
            else if(Constants.is_label(token))
            {
                l_lc.add(new Label(token.substring(0,token.length()-1),PC));
            }
        }
        sc = new Scanner(code_section.stream);
        
        while(sc.hasNext())
        {
            String token = sc.next();
            System.out.println("---"+token+"---");
            if(Constants.is_command(token))
            {
                System.out.println("COMMAND IDENTIFIED");
                if(Constants.get_type(token) == Constants.R_TYPE)
                {
                    System.out.println("RTYPE IDENTIFIED");
                    try
                    {
                        String dest = sc.next();
                        String src1 = sc.next();
                        String src2 = sc.next();
                        int dest_add = Constants.address_of(dest);
                        int src1_add = Constants.address_of(src1);
                        int src2_add = Constants.address_of(src2);
                        if(dest_add == -1 || src1_add == -1 || src2_add == -1)
                        {
                            throw new Exception("arguments missing");
                        }
                        if(token.equals(Constants.ADD))
                        {
                            l_pc.add(new Instruction(Commands.add(src1_add,src2_add,dest_add),code_current));
                        }
                        else if(token.equals(Constants.SUB))
                        {
                            l_pc.add(new Instruction(Commands.sub(src1_add,src2_add,dest_add),code_current));
                        }
                        else if(token.equals(Constants.AND))
                        {
                            l_pc.add(new Instruction(Commands.and(src1_add,src2_add,dest_add),code_current));
                        }
                        else if(token.equals(Constants.OR))
                        {
                            l_pc.add(new Instruction(Commands.or(src1_add,src2_add,dest_add),code_current));
                        }
                        else if(token.equals(Constants.SRA))
                        {
                            l_pc.add(new Instruction(Commands.sra(src1_add,src2_add,dest_add),code_current));
                        }
                        else if(token.equals(Constants.SLL))
                        {
                            l_pc.add(new Instruction(Commands.sll(src1_add,src2_add,dest_add),code_current));
                        }
                        else if(token.equals(Constants.XOR))
                        {
                            l_pc.add(new Instruction(Commands.xor(src1_add,src2_add,dest_add),code_current));
                        }
                        else if(token.equals(Constants.SLTU))
                        {
                            l_pc.add(new Instruction(Commands.sltu(src1_add,src2_add,dest_add),code_current));
                        }
                        else if(token.equals(Constants.SLT))
                        {
                            l_pc.add(new Instruction(Commands.slt(src1_add,src2_add,dest_add),code_current));
                        }
                        else if(token.equals(Constants.SLL))
                        {
                            l_pc.add(new Instruction(Commands.sll(src1_add,src2_add,dest_add),code_current));
                        }
                        else throw new Exception("unrecognized command");
                        code_current+=4;
                    }
                    catch(Exception e)
                    {
                        e.printStackTrace();
                        throw new Exception("missing arguments");
                    }
                }
                else if(Constants.get_type(token) == Constants.I_TYPE)
                {
                    System.out.println("ITYPE IDENTIFIED");
                    try
                    {
                        String dest = sc.next();
                        String src1 = sc.next();
                        long imm = 0;
                        if(token.equals(Constants.LW) && !sc.hasNextLong())
                        {
                            String label = sc.next();
                            int address = get_address_data(label);
                            if(address == -1)throw new Exception("unidentified label");
                            imm = address;
                        }
                        else imm = sc.nextLong();
                        int dest_add = Constants.address_of(dest);
                        int src1_add = Constants.address_of(src1);
                        if(dest_add == -1 || src1_add == -1)
                        {
                            throw new Exception("arguments missing");
                        }
                        if(token.equals(Constants.ADDI))
                        {
                            l_pc.add(new Instruction(Commands.addi(src1_add,dest_add,imm),code_current));
                        }
                        else if(token.equals(Constants.ANDI))
                        {
                            l_pc.add(new Instruction(Commands.andi(src1_add,dest_add,imm),code_current));
                        }
                        else if(token.equals(Constants.ORI))
                        {
                            l_pc.add(new Instruction(Commands.ori(src1_add,dest_add,imm),code_current));
                        }
                        else if(token.equals(Constants.XORI))
                        {
                            l_pc.add(new Instruction(Commands.xori(src1_add,dest_add,imm),code_current));
                        }
                        else if(token.equals(Constants.SLTI))
                        {
                            l_pc.add(new Instruction(Commands.slti(src1_add,dest_add,imm),code_current));
                        }
                        else if(token.equals(Constants.SLTIU))
                        {
                            l_pc.add(new Instruction(Commands.sltiu(src1_add,dest_add,imm),code_current));
                        }
                        else if(token.equals(Constants.LW))
                        {
                            l_pc.add(new Instruction(Commands.lw(src1_add,dest_add,imm),code_current));
                        }
                        code_current+=4;
                    }
                    catch(Exception e)
                    {
                        e.printStackTrace();
                        throw new Exception("missing arguments");
                    }
                }
                else if(Constants.get_type(token) == Constants.B_TYPE)
                {
                    System.out.println("BTYPE IDENTIFIED");
                    try
                    {
                        String src1 = sc.next();
                        String src2 = sc.next();
                        long value = -1;
                        int src1_add = Constants.address_of(src1);
                        int src2_add = Constants.address_of(src2);
                        if(src1_add == -1 || src2_add == -1)
                        {
                            throw new Exception("arguments missing");
                        }
                        if(sc.hasNextLong())value = sc.nextLong();
                        else
                        {
                            String label = sc.next();
                            int address = get_address_code(label);
                            value = address;
                        }
                        if(value == -1)throw new Exception("unidentified label");
                        value = value-code_current;
                        if(token.equals(Constants.BEQ))
                        {
                            l_pc.add(new Instruction(Commands.beq(src1_add,src2_add,value),code_current));
                        }
                        else if(token.equals(Constants.BNE))
                        {
                            l_pc.add(new Instruction(Commands.bne(src1_add,src2_add,value),code_current));
                        }
                        else if(token.equals(Constants.BLT))
                        {
                            l_pc.add(new Instruction(Commands.blt(src1_add,src2_add,value),code_current));
                        }
                        else if(token.equals(Constants.BLTU))
                        {
                            l_pc.add(new Instruction(Commands.bltu(src1_add,src2_add,value),code_current));
                        }
                        else if(token.equals(Constants.BGE))
                        {
                            l_pc.add(new Instruction(Commands.bge(src1_add,src2_add,value),code_current));
                        }
                        else if(token.equals(Constants.BGEU))
                        {
                            l_pc.add(new Instruction(Commands.bgeu(src1_add,src2_add,value),code_current));
                        }
                        code_current+=4;
                    }
                    catch(Exception e)
                    {
                        e.printStackTrace();
                        throw new Exception("missing arguments");
                    }
                }
                else if(Constants.get_type(token) == Constants.S_TYPE)
                {
                    try
                    {
                        String src = sc.next();
                        String dest = sc.next();
                        int src_add = Constants.address_of(src);
                        int dest_add = Constants.address_of(dest);
                        long value = -1;
                        if(sc.hasNextLong())value = sc.nextLong();
                        else
                        {
                            String label = sc.next();
                            value = get_address_data(label);
                        }
                        if(value == -1 || src_add==-1 || dest_add == -1)throw new Exception("incorrect offset");
                        if(token.equals(Constants.SW))
                        {
                            l_pc.add(new Instruction(Commands.sw(src_add,dest_add,value),code_current));
                        }
                        code_current+=4;
                    }
                    catch(Exception e)
                    {
                        e.printStackTrace();
                        throw new Exception("missing arguments");
                    }
                }
                else if(Constants.get_type(token) == Constants.J_TYPE)
                {
                    try
                    {
                        String dest = sc.next();
                        int dest_add = Constants.address_of(dest);
                        long value = -1;
                        if(sc.hasNextLong())value = sc.nextLong();
                        else
                        {
                            String label = sc.next();
                            value = get_address_code(label);
                        }
                        if(value == -1 || dest_add == -1)throw new Exception("missing arguments");
                        value-=code_current;
                        if(token.equals(Constants.JAL))
                        {
                            l_pc.add(new Instruction(Commands.jal(dest_add,value),code_current));
                        }
                        code_current+=4;
                    }
                    catch(Exception e)
                    {
                        e.printStackTrace();
                        throw new Exception("missing arguments");
                    }
                }
                else if(Constants.get_type(token) == Constants.PSEUDO_TYPE)
                {
                    if(token.equals(Constants.LI))
                    {
                        try
                        {
                            String reg = sc.next();
                            long val = sc.nextInt();
                            int reg_add = Constants.address_of(reg);
                            if(reg_add == -1)throw new Exception("unrecognized register");
                            l_pc.add(new Instruction(Commands.andi(0,reg_add,0),code_current));
                            code_current+=4;
                            l_pc.add(new Instruction(Commands.addi(0,reg_add,val),code_current));
                            code_current+=4;
                        }
                        catch(Exception e)
                        {
                            e.printStackTrace();
                            throw new Exception("missing arguments");
                        }
                    }
                    else if(token.equals(Constants.MOV))
                    {
                        String dest = sc.next();
                        String src = sc.next();
                        int src_add = Constants.address_of(src);
                        int dest_add = Constants.address_of(dest);
                        if(src_add == -1 || dest_add == -1)throw new Exception("unrecognized register");
                        l_pc.add(new Instruction(Commands.andi(0,dest_add,0),code_current));
                        code_current+=4;
                        l_pc.add(new Instruction(Commands.add(src_add,dest_add,dest_add),code_current));
                        code_current+=4;
                    }
                }
            }
        }
    }
    public static void main(String args[]) throws Exception
    {
        Compiler c = new Compiler(0,0);
        c.compile(Paths.get("test.s"),Paths.get("Test.txt"));
    }
}
