import java.nio.file.*;
import java.util.*;

public class Compiler
{
    private final int memory_start_data;
    private int data_current;
    private final int memory_start_code;
    private int code_current;
    private List<Line> l;
    
    private int start_code;
    private int end_code;
    private int start_data;
    private int end_data;
    
    private class Label_data
    {
        String name;
        int address;
        Label_data(String name, int address)
        {
            this.name = name;
            this.address = address;
        }
    }
    private List<Label_data> l_ld;
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
    Compiler(int memory_start_data, int memory_start_code)
    {
        this.memory_start_data = memory_start_data;
        this.memory_start_code = memory_start_code;
        this.data_current = memory_start_data;
        this.code_current = memory_start_code;
        this.l = new ArrayList<Line>();
        this.l_ld = new ArrayList<Label_data>();
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
        locate_blocs();
        System.out.println("data:"+start_data+" to "+end_data);
        System.out.println("code:"+start_code+" to "+end_code);
        print();
        process_data();
        for(int i=0;i<l_ld.size();i++)
        {
            System.out.println(l_ld.get(i).name+"refers to location:"+l_ld.get(i).address);
        }
        for(int i=0;i<l_pc.size();i++)
        {
            System.out.println(l_pc.get(i).address+"|"+l_pc.get(i).contents);
        }
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
        List<Integer> d = new ArrayList<Integer>();
        List<Integer> c = new ArrayList<Integer>();
        int end = l.get(l.size()-1).number;
        for(int i=0;i<l.size();i++)
        {
            if(Constants.DATA.is_contained_in(l.get(i).contents))d.add(l.get(i).number);
            if(Constants.CODE.is_contained_in(l.get(i).contents))c.add(l.get(i).number);
        }
        if(c.size() == 0)throw new Exception("text section missing");
        if(c.size() > 1)throw new Exception("multiple text sections");
        if(d.size() > 1)throw new Exception("multiple data sections");
        if(d.size() == 0)
        {
            start_data = 0;
            end_data = 0;
            start_code = c.get(0);
            end_code = end;
        }
        else
        {
            start_data = d.get(0);
            start_code = c.get(0);
            if(start_data < start_code)
            {
                end_data = start_code;
                end_code = end;
            }
            else if(start_data > start_code)
            {
                end_code = start_data;
                end_data = end;
            }
            else
            {
                end_code = start_code;
                end_data = start_data;
            }
        }
    }
    
    public void process_data() throws Exception
    {
        System.out.println("functon called");
        if(start_data == 0)return;
        System.out.println("data exists");
        int i_start;
        for(i_start = 0;;i_start++)if(l.get(i_start).number == start_data)break;
        int i_end;
        for(i_end = 0;;i_end++)if(l.get(i_end).number == end_data)break;
        
        for(int i=i_start;i<i_end;i++)
        {
            String c = l.get(i).contents;
            if(i == i_start)
            {
                int START = c.indexOf("data");
                if(START == -1)throw new Exception("Syntax error");
                c = c.substring(START+4);
            }
            else if(i == i_end)
            {
                int START = c.indexOf("text");
                if(START == -1)throw new Exception("Syntax error");
                c = c.substring(0,START);
            }
            
            Scanner sc = new Scanner(c);
            while(sc.hasNext())
            {
                String t = sc.next();
                if(t.charAt(t.length()-1) == Constants.LABEL_TERMINATOR)
                {
                    String label = t.substring(0,t.length()-1);
                    if(!Constants.is_identifier(label))throw new Exception("Incorrect identifier");
                    this.l_ld.add(new Label_data(label,data_current));
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
                            l_pc.add(new Instruction(Commands.lui(5,initial_value),code_current));
                            code_current++;
                            l_pc.add(new Instruction(Commands.sw(5,0,data_current),code_current));
                            code_current++;
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
                    else
                    {
                        throw new Exception("unrecognized type");
                    }
                }
            }
        }
    }
    public static void test() throws Exception,java.io.IOException
    {
        Compiler c = new Compiler(0,0);
        c.compile(Paths.get("test.s"),null);
    }
}
