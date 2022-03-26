package compiler;

import java.nio.file.*;
import java.util.*;

public class Compiler
{
    private int memory_start_data;
    private int data_current;
    private int memory_start_code;
    private int code_current;
    private List<Line> l;
    private StringBuilder transcript;
    
    private static final String separator = "-----------------------------------------------------------------";
    
    private static class Label
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
        for (Label label : l_lc) if (label.name.equals(name)) return label.address;
        return -1;
    }
    public int get_address_data(String name)
    {
        for (Label label : l_ld) if (label.name.equals(name)) return label.address;
        return -1;
    }
    private List<Label> l_ld;
    private List<Label> l_lc;
    private static class Instruction
    {
        String contents;
        int address;
        String comment;
        Instruction(String contents, int address, String comment)
        {
            this.contents = contents;
            this.address = address;
            this.comment = comment;
        }
    }
    private List<Instruction> l_pc;
    
    private static class Line
    {
        String contents;
        int number;
    }
    
    private class Source_stream
    {
        StringBuilder stream;
        List<Integer> numbers;
        Source_stream()
        {
            stream = new StringBuilder();
            numbers = new ArrayList<>();
        }
        void append(String s, int n)
        {
            stream.append(s);
            for(int i=0;i<s.length();i++)numbers.add(n);
        }
        String stream()
        {
            return this.stream.toString();
        }
        void log()
        {
            if(stream.length() == 0)return;
            transcript.append(numbers.get(0)).append("|");
            transcript.append((stream.charAt(0)));
            for(int i=1;i<stream.length();i++)
            {
                if(numbers.get(i-1)!=numbers.get(i)) transcript.append("\n").append(numbers.get(i)).append("|");
                transcript.append((stream.charAt(i)));
            }
        }
    }
    private Source_stream code_section;
    private Source_stream data_section;
    
    public Compiler(int memory_start_data, int memory_start_code)
    {
        reset(memory_start_data, memory_start_code);
    }
    public void reset(int memory_start_data, int memory_start_code)
    {
        this.transcript = new StringBuilder();
        this.memory_start_data = memory_start_data;
        this.memory_start_code = memory_start_code;
        this.data_current = memory_start_data;
        this.code_current = memory_start_code;
        this.l = new ArrayList<>();
        this.code_section = new Source_stream();
        this.data_section = new Source_stream();
        this.l_ld = new ArrayList<>();
        this.l_lc = new ArrayList<>();
        this.l_pc = new ArrayList<>();
    }
    private void check_clashing_labels()throws Exception
    {
        for(int i=0;i<l_lc.size();i++)
        {
            for(int j=i+1;j<l_lc.size();j++) // within code
            {
                if(l_lc.get(i).name.equals(l_lc.get(j).name))throw new Exception("Label name reused: "+l_lc.get(i).name+" (code section)");
            }
            for(int j=0;j<l_ld.size();j++)  // between code and data
            {
                if(l_lc.get(i).name.equals(l_ld.get(j).name))throw new Exception("Label name reused: "+l_lc.get(i).name+" (code and data section)");
            }
        }
        for(int i=0;i<l_ld.size();i++) //within data
        {
            for(int j=i+1;j<l_ld.size();j++)
            {
                if(l_ld.get(i).name.equals(l_ld.get(j).name))throw new Exception("Label name reused: "+l_ld.get(i).name+" (data section)");
            }
        }
    }
    public String get_transcript()
    {
        return this.transcript.toString();
    }
    public void compile(Path source, Path binary) throws Exception {
        List<String> l_raw = Files.readAllLines(source);
        this.l = new ArrayList<>();
        for(int i = 0;i< l_raw.size();i++)
        {
            Line li = new Line();
            li.number = i+1;
            li.contents = l_raw.get(i);
            l.add(li);
        }
        transcript.append("\n"+separator+"\nOriginal source code:\n");
        log();
        scrub();
        transcript.append("\n"+separator+"\nScrubbed source code:\n");
        log();
        transcript.append("\n"+separator+"\nCompilation:\n");
        locate_blocs();
        process_data();
        process_code();
        transcript.append("\n"+separator+"\ndata labels:\n");
        for (Label label : l_ld) {
            transcript.append("\n").append(label.name).append(" refers to data location: ").append(label.address);
        }
        transcript.append("\n"+separator+"\ncode labels:\n");
        for (Label label : l_lc) {
            transcript.append("\n").append(label.name).append(" refers to PC location: ").append(label.address);
        }
        transcript.append("\n"+separator);
        transcript.append("\nBinary:");
        transcript.append("\nPC  |              code              |purpose");

        for (Instruction instruction : l_pc) {
            transcript.append("\n").append(instruction.address).append("\t|").append(instruction.contents).append("|").append(instruction.comment);
        }
        //writing file
        StringBuilder file = new StringBuilder();
        for (Instruction instruction : l_pc) {
            file.append(instruction.contents).append(" ");
        }
        Files.write(binary, file.toString().getBytes());
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
            int start = c.indexOf(Syntax.MULTI_LINE_START);
            int end = c.indexOf(Syntax.MULTI_LINE_END);
            int size_start = Syntax.MULTI_LINE_START.length();
            int size_end = Syntax.MULTI_LINE_END.length();
            if(f && end==-1)
            {
                l.get(i).contents = "";
            }
            if(start == -1 && end == -1)
            {
                continue;   
            }
            else if(start != -1 && end == -1)
            {
                f = true;
                l.get(i).contents = c.substring(0,start);
            }
            else if(start == -1 && end != -1)
            {
                f = false;
                l.get(i).contents = c.substring(end +size_end);
            }
            else
            {
                int next_end = c.indexOf(Syntax.MULTI_LINE_END, start);
                if(next_end != -1)
                {
                    l.get(i).contents = c.substring(0,start)+c.substring(end+size_end);
                    i--;
                }
                else
                {
                    f=true;
                    l.get(i).contents = c.substring(end +size_end,start);
                }
            }
        }
    }
    private void remove_single_line_comments()
    {
        for (Line line : l) {
            String c = line.contents;
            int start = c.indexOf(Syntax.SINGLE_LINE);
            if (start != -1) {
                line.contents = c.substring(0, start);
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
        for (Line line : l) {
            StringBuilder s = new StringBuilder();
            String c = line.contents;
            for (int j = 0; j < c.length(); j++) {
                char d = c.charAt(j);
                if (d == '\t') s.append(Syntax.WHITESPACE);
                else s.append(d);
            }
            line.contents = s.toString();
        }
    }
    private void trim()
    {
        for (Line line : l) {
            String c = line.contents;
            c = c.trim();
            line.contents = c;
        }
    }
    public void log()
    {
        for (Line line : l) {
            transcript.append("\n").append(line.number).append("\t|").append(line.contents).append("|");
        }
    }
    private void locate_blocs() throws Exception
    {
        Source_stream source_full = new Source_stream();
        for (Line line : l) {
            source_full.append(line.contents + Syntax.WHITESPACE, line.number);
        }
        
        int data_start = Syntax.DATA.start_of_first_instance_in(source_full.stream());
        int data_end = Syntax.DATA.end_of_first_instance_in(source_full.stream());
        int code_start = Syntax.CODE.start_of_first_instance_in(source_full.stream());
        int code_end = Syntax.CODE.end_of_first_instance_in(source_full.stream());
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
    
    private void process_data() throws Exception
    {
        transcript.append("\n"+"data section: ");
        if(data_section.stream.length()==0)return;
            Scanner sc = new Scanner(data_section.stream());
            sc.useDelimiter("("+Syntax.WHITESPACE+")+");
            while(sc.hasNext())
            {
                String t = sc.next();
                transcript.append("\n>").append(t);
                if(Syntax.LABEL_TERMINATOR.equals(""+t.charAt(t.length()-1)))
                {
                    String label = t.substring(0,t.length()-1);
                    if(!Syntax.is_identifier(label))throw new Exception("Incorrect identifier");
                    transcript.append("\n" + "label identified: ").append(label);
                    this.l_ld.add(new Label(label, data_current));
                }
                else if(Syntax.DATATYPE_INITIATOR.equals(""+t.charAt(0)))
                {
                    String type = t.substring(1);// leading . is removed
                    if(Syntax.WORD.contains(type))
                    {
                        if(data_current%4!=0)throw new Exception("misaligned memory");
                        transcript.append("\n").append(Syntax.WORD.words[0]).append(" allocated");
                        if(sc.hasNextInt())
                        {
                            long initial_value = sc.nextInt();
                            l_pc.add(new Instruction(Binary.addi(0, 5, initial_value), code_current, Syntax.WORD.words[0] + " " + Syntax.ADDI.words[0]));
                            code_current+=4;
                            l_pc.add(new Instruction(Binary.sw(5, 0, data_current), code_current, Syntax.WORD.words[0] + " " + Syntax.SW.words[0]));
                            code_current+=4;
                            l_pc.add(new Instruction(Binary.andi(0, 5, 0), code_current, Syntax.WORD.words[0] + " " + Syntax.ANDI.words[0]));
                            code_current+=4;
                            transcript.append("\n" + "value initialized as: ").append(initial_value);
                        }
                        data_current+=4;
                    }
                    else if(Syntax.BYTE.contains(type))
                    {
                        //memory is always aligned for BYTE since it is byte addressable memory
                        transcript.append("\n").append(Syntax.BYTE.words[0]).append(" allocated");
                        if(sc.hasNextInt())
                        {
                            long initial_value = sc.nextInt();
                            l_pc.add(new Instruction(Binary.addi(0, 5, initial_value), code_current, Syntax.BYTE.words[0] + " " + Syntax.ADDI.words[0]));
                            code_current+=4;
                            l_pc.add(new Instruction(Binary.sb(5, 0, data_current), code_current, Syntax.BYTE.words[0] + " " + Syntax.SB.words[0]));
                            code_current+=4;
                            l_pc.add(new Instruction(Binary.andi(0, 5, 0), code_current, Syntax.BYTE.words[0] + " " + Syntax.ANDI.words[0]));
                            code_current+=4;
                            transcript.append("\n" + "value initialized as: ").append(initial_value);
                        }
                        data_current+=1;
                    }
                    else if(Syntax.SHORT.contains(type))
                    {
                        if(data_current%2!=0)throw new Exception("misaligned memory");
                        transcript.append("\n").append(Syntax.SHORT.words[0]).append(" allocated");
                        if(sc.hasNextInt())
                        {
                            long initial_value = sc.nextInt();
                            l_pc.add(new Instruction(Binary.addi(0, 5, initial_value), code_current, Syntax.SHORT.words[0] + " " + Syntax.ADDI.words[0]));
                            code_current+=4;
                            l_pc.add(new Instruction(Binary.sh(5, 0, data_current), code_current, Syntax.SHORT.words[0] + " " + Syntax.SH.words[0]));
                            code_current+=4;
                            l_pc.add(new Instruction(Binary.andi(0, 5, 0), code_current, Syntax.SHORT.words[0] + " " + Syntax.ANDI.words[0]));
                            code_current+=4;
                            transcript.append("\n" + "value initialized as: ").append(initial_value);
                        }
                        data_current+=2;
                    }
                    else if(Syntax.SPACE.contains(type))
                    {
                        try
                        {
                            int n = sc.nextInt();
                            data_current+=n;
                            transcript.append("\n").append(n).append(" bytes of space in memory allocated");
                        }
                        catch(Exception e)
                        {
                            throw new Exception("literal missing");
                        }
                    }
                    else if(Syntax.ALIGN.contains(type))
                    {
                        try
                        {
                            int n = sc.nextInt();
                            int p = (int)Math.pow(2,n);
                            while(data_current%p!=0)data_current++;
                            transcript.append("\n" + "data pointer aligned to nearest multiple of 2^").append(n);
                        }
                        catch(Exception e)
                        {
                            throw new Exception("literal missing");
                        }
                    }
                    else if(Syntax.ASCII.contains(type)||Syntax.ASCIIZ.contains(type))
                    {
                        try
                        {
                            String s = sc.next();
                            String value = s.substring(1,s.length()-1);//change value, not s for wildcards
                            if(Syntax.ASCIIZ.contains(type))s+='\u0000';
                            int n = value.length();
                            for(int i=0;i<n;i++)
                            {
                                char d = value.charAt(i);
                                l_pc.add(new Instruction(Binary.addi(0, 5, d), code_current, "'" + d + "' " + Syntax.ADDI.words[0]));
                                code_current+=4;
                                l_pc.add(new Instruction(Binary.sb(5, 0, data_current), code_current, "'" + d + "' " + Syntax.SB.words[0]));
                                code_current+=4;
                                l_pc.add(new Instruction(Binary.andi(0, 5, 0), code_current, "'" + d + "' " + Syntax.ANDI.words[0]));
                                code_current+=4;
                                data_current+=1;
                            }
                            transcript.append("\n" + "space allocated and string initialized as: ").append(s);
                            if(Syntax.ASCIIZ.contains(type))transcript.append(" with null character termination");
                        }
                        catch(Exception e)
                        {
                            throw new Exception("string not rendered correctly");
                        }
                    }
                    else
                    {
                        throw new Exception("unrecognized type");
                    }
                }
            }
        transcript.append("\n");
    }
    private void process_code() throws Exception
    {
        transcript.append("\n"+"code section: ");
        Scanner sc = new Scanner(code_section.stream());
        sc.useDelimiter("("+Syntax.WHITESPACE+")+");
        int PC = code_current;
        while(sc.hasNext())
        {
            String token = sc.next();
            if(Syntax.is_command(token))
            {
                PC+=Syntax.get_n_of_command(token)*4;
            }
            else if(Syntax.is_label(token))
            {
                l_lc.add(new Label(token.substring(0, token.length() - 1), PC));
                transcript.append("\n" + "label identified: ").append(token);
            }
        }
        check_clashing_labels();
        boolean main_present = false;
        for (Label item : l_lc) {
            if (Syntax.MAIN.contains(item.name)) {
                main_present = true;
                for(Label item_ : l_lc)item_.address+=4; //inserting jump to main requires all other code labels to shift up
                l_pc.add(new Instruction(Binary.jal(1, item.address), code_current, Syntax.JAL.words[0] + " " + Syntax.MAIN.words[0]));
                code_current+=4;
            }
        }
        if(!main_present)transcript.append("\nWARNING: no main label found");

        sc = new Scanner(code_section.stream());
        sc.useDelimiter("(("+Syntax.WHITESPACE+")*"+Syntax.ARGUMENT_SEPARATOR+"("+Syntax.WHITESPACE+")*)|("+Syntax.WHITESPACE+")+");
        while(sc.hasNext())
        {
            String token = sc.next();
            transcript.append("\n").append(">").append(token);
            if(Syntax.is_command(token))
            {
                if(Syntax.get_input_type_of_command(token)==Syntax.RRR)
                {
                    try
                    {
                        String dest = sc.next();
                        String src1 = sc.next();
                        String src2 = sc.next();
                        transcript.append("\nsource register 1: ").append(src1);
                        transcript.append("\nsource register 2: ").append(src2);
                        transcript.append("\ndestination register: ").append(dest);
                        int dest_add = Syntax.address_of_register(dest);
                        int src1_add = Syntax.address_of_register(src1);
                        int src2_add = Syntax.address_of_register(src2);
                        if(dest_add == -1 || src1_add == -1 || src2_add == -1)
                        {
                            throw new Exception("arguments incorrect, registers do not exist");
                        }
                        // add sub and or xor sll (sla) srl sra slt sltu sgt sgtu
                        if(Syntax.ADD.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.add(src1_add, src2_add, dest_add), code_current, Syntax.ADD.words[0]));
                        }
                        else if(Syntax.SUB.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.sub(src1_add, src2_add, dest_add), code_current, Syntax.SUB.words[0]));
                        }
                        else if(Syntax.AND.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.and(src1_add, src2_add, dest_add), code_current, Syntax.AND.words[0]));
                        }
                        else if(Syntax.OR.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.or(src1_add, src2_add, dest_add), code_current, Syntax.OR.words[0]));
                        }
                        else if(Syntax.XOR.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.xor(src1_add, src2_add, dest_add), code_current, Syntax.XOR.words[0]));
                        }
                        else if(Syntax.SLL.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.sll(src1_add, src2_add, dest_add), code_current, Syntax.SLL.words[0]));
                        }
                        else if(Syntax.SRL.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.srl(src1_add, src2_add, dest_add), code_current, Syntax.SRL.words[0]));
                        }
                        else if(Syntax.SRA.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.sra(src1_add, src2_add, dest_add), code_current, Syntax.SRA.words[0]));
                        }
                        else if(Syntax.SLT.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.slt(src1_add, src2_add, dest_add), code_current, Syntax.SLT.words[0]));
                        }
                        else if(Syntax.SLTU.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.sltu(src1_add, src2_add, dest_add), code_current, Syntax.SLTU.words[0]));
                        }
                        else if(Syntax.SGT.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.slt(src2_add, src1_add, dest_add), code_current, Syntax.SGT.words[0]+" "+Syntax.SLT.words[0]));
                        }
                        else if(Syntax.SGTU.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.sltu(src2_add, src1_add, dest_add), code_current, Syntax.SGTU.words[0]+" "+Syntax.SLTU.words[0]));
                        }
                        else throw new Exception("command type mismatch");
                        code_current+=4*Syntax.get_n_of_command(token);
                    }
                    catch(Exception e)
                    {
                        transcript.append("\nERROR: ").append(e.getMessage());
                        throw new Exception("missing arguments");
                    }
                }
                else if(Syntax.get_input_type_of_command(token)==Syntax.RRi)
                {
                    try
                    {
                        String src1 = sc.next();
                        String src2 = sc.next();
                        transcript.append("\n" + "source register 1: ").append(src1);
                        transcript.append("\n" + "source register 2: ").append(src2);
                        long value ;
                        int src1_add = Syntax.address_of_register(src1);
                        int src2_add = Syntax.address_of_register(src2);
                        if(src1_add == -1 || src2_add == -1)
                        {
                            throw new Exception("arguments missing");
                        }
                        if(sc.hasNextLong())
                        {
                            value = sc.nextLong();
                            transcript.append("\nimmediate value: ").append(value);
                        }
                        else
                        {
                            String label = sc.next();
                            transcript.append("\n" + "target label: ").append(label);
                            int value_c = get_address_code(label);
                            int value_d = get_address_data(label);
                            if(value_c == -1 && value_d == -1)throw new Exception("unidentified label");
                            else if(value_c == -1)
                            {
                                transcript.append(" (data)");
                                value = value_d;
                            }
                            else if(value_d == -1)
                            {
                                transcript.append(" (code)");
                                value = value_c;
                            }
                            else throw new Exception("multiple labels matching");
                        }
                        // (addi subi xori ori andi) (slli slri srai (slai)) (slti sltiu) (beq bne blt bge bltu bgeu ble bgt bleu bgtu)
                        if(Syntax.ADDI.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.addi(src2_add, src1_add, value), code_current, Syntax.ADDI.words[0]));
                        }
                        else if(Syntax.SUBI.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.addi(src2_add, src1_add, -value), code_current, Syntax.SUBI.words[0]+" "+Syntax.ADDI.words[0]));
                        }
                        else if(Syntax.ANDI.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.andi(src2_add, src1_add, value), code_current, Syntax.ANDI.words[0]));
                        }
                        else if(Syntax.ORI.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.ori(src2_add, src1_add, value), code_current, Syntax.ORI.words[0]));
                        }
                        else if(Syntax.XORI.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.xori(src2_add, src1_add, value), code_current, Syntax.XORI.words[0]));
                        }
                        else if(Syntax.SLTI.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.slti(src2_add, src1_add, value), code_current, Syntax.SLTI.words[0]));
                        }
                        else if(Syntax.SLTIU.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.sltiu(src2_add, src1_add, value), code_current, Syntax.SLTIU.words[0]));
                        }
                        else if(Syntax.SLLI.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.slli(src2_add, src1_add, value), code_current, Syntax.SLLI.words[0]));
                        }
                        else if(Syntax.SRLI.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.srli(src2_add, src1_add, value), code_current, Syntax.SRLI.words[0]));
                        }
                        else if(Syntax.SRAI.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.srai(src2_add, src1_add, value), code_current, Syntax.SRAI.words[0]));
                        }
                        else if(Syntax.BEQ.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.beq(src1_add, src2_add, value-code_current), code_current, Syntax.BEQ.words[0]));
                        }
                        else if(Syntax.BNE.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.bne(src1_add, src2_add, value-code_current), code_current, Syntax.BNE.words[0]));
                        }
                        else if(Syntax.BLT.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.blt(src1_add, src2_add, value-code_current), code_current, Syntax.BLT.words[0]));
                        }
                        else if(Syntax.BLTU.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.bltu(src1_add, src2_add, value-code_current), code_current, Syntax.BLTU.words[0]));
                        }
                        else if(Syntax.BGE.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.bge(src1_add, src2_add, value-code_current), code_current, Syntax.BGE.words[0]));
                        }
                        else if(Syntax.BGEU.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.bgeu(src1_add, src2_add, value-code_current), code_current, Syntax.BGEU.words[0]));
                        }
                        else if(Syntax.BLE.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.bge(src2_add, src1_add, value-code_current), code_current, Syntax.BLE.words[0]+" "+Syntax.BGE.words[0]));
                        }
                        else if(Syntax.BGT.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.blt(src2_add, src1_add, value-code_current), code_current, Syntax.BGT.words[0]+" "+Syntax.BLT.words[0]));
                        }
                        else if(Syntax.BLEU.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.bgeu(src2_add, src1_add, value-code_current), code_current, Syntax.BLEU.words[0]+" "+Syntax.BGEU.words[0]));
                        }
                        else if(Syntax.BGTU.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.bltu(src2_add, src1_add, value-code_current), code_current, Syntax.BGTU.words[0]+" "+Syntax.BLTU.words[0]));
                        }
                        else throw new Exception("command type mismatch");
                        code_current+=4*Syntax.get_n_of_command(token);
                    }
                    catch(Exception e)
                    {
                        e.printStackTrace();
                        throw new Exception("missing arguments");
                    }
                }
                else if(Syntax.get_input_type_of_command(token)==Syntax.Ri_R_)
                {
                    try
                    {
                        String r_c = sc.next();
                        transcript.append("\nfirst register: ").append(r_c);
                        int r_c_add = Syntax.address_of_register(r_c);
                        if(r_c_add==-1)throw new Exception("no such register");
                        String ir = sc.next();
                        int r_o_add = 0;
                        long immediate;
                        String imm;
                        if(ir.contains(Syntax.IMMEDIATE_CLOSE)) //second register is used
                        {
                            imm = ir.substring(0,ir.indexOf(Syntax.IMMEDIATE_OPEN));
                            String r_o = ir.substring(ir.indexOf(Syntax.IMMEDIATE_OPEN)+1,ir.indexOf(Syntax.IMMEDIATE_CLOSE));
                            transcript.append("\n second register: ").append(r_o);
                            r_o_add = Syntax.address_of_register(r_o);
                        }
                        else
                        {
                            transcript.append("\n no second register");
                            imm = ir;
                        }
                        if(new Scanner(imm).hasNextLong())
                        {
                            immediate = Long.parseLong(imm);
                        }
                        else
                        {
                            transcript.append("\nlabel: ").append(imm);
                            int value_c = get_address_code(imm);
                            int value_d = get_address_data(imm);
                            if(value_c == -1 && value_d == -1)throw new Exception("unidentified label");
                            else if(value_c == -1)
                            {
                                transcript.append(" (data)");
                                immediate = value_d;
                            }
                            else if(value_d == -1)
                            {
                                transcript.append(" (code) [WARNING]");
                                immediate = value_c;
                            }
                            else throw new Exception("multiple labels matching");
                        }
                        // (lw lb lh lhu lbu) (sw sb sh)
                        if(Syntax.LW.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.lw(r_o_add,r_c_add,immediate), code_current, Syntax.LW.words[0]));
                        }
                        else if(Syntax.LB.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.lb(r_o_add,r_c_add,immediate), code_current, Syntax.LB.words[0]));
                        }
                        else if(Syntax.LH.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.lh(r_o_add, r_c_add,immediate), code_current, Syntax.LH.words[0]));
                        }
                        else if(Syntax.LBU.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.lbu(r_o_add, r_c_add,immediate), code_current, Syntax.LBU.words[0]));
                        }
                        else if(Syntax.LHU.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.lhu(r_o_add, r_c_add,immediate), code_current, Syntax.LHU.words[0]));
                        }
                        else if(Syntax.SW.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.sw(r_o_add, r_c_add, immediate), code_current, Syntax.SW.words[0]));
                        }
                        else if(Syntax.SB.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.sw(r_o_add, r_c_add, immediate), code_current, Syntax.SB.words[0]));
                        }
                        else if(Syntax.SH.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.sw(r_o_add, r_c_add, immediate), code_current, Syntax.SH.words[0]));
                        }
                        else throw new Exception("command type mismatch");
                        code_current+=4*Syntax.get_n_of_command(token);
                    }
                    catch(Exception e)
                    {
                        transcript.append("\nERROR: ").append(e.getMessage());
                        throw new Exception("missing arguments");
                    }
                }
                else if(Syntax.get_input_type_of_command(token)==Syntax.RR)
                {
                    try
                    {
                        String dest = sc.next();
                        String source = sc.next();
                        transcript.append("\n source register: ").append(source);
                        transcript.append("\n destination register: ").append(dest);
                        int dest_add = Syntax.address_of_register(dest);
                        int src_add = Syntax.address_of_register(source);
                        if(dest_add==-1||src_add==-1)throw new Exception("no such register");
                        // (inc dec) (mv swp) (not neg) (seqz snez sltz sgtz)
                        if(Syntax.INC.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.add(src_add, dest_add, dest_add), code_current,Syntax.INC.words[0]+" "+Syntax.ADD.words[0]));
                        }
                        else if(Syntax.DEC.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.sub(src_add, dest_add, dest_add), code_current,Syntax.DEC.words[0]+" "+Syntax.SUB.words[0]));
                        }
                        else if(Syntax.MV.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.add(src_add, 0, dest_add), code_current,Syntax.MV.words[0]+" "+Syntax.ADD.words[0]));
                        }
                        else if(Syntax.NOT.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.xori(src_add, dest_add, -1), code_current,Syntax.NOT.words[0]+" "+Syntax.XORI.words[0]));
                        }
                        else if(Syntax.NEG.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.sub(0, src_add, dest_add), code_current,Syntax.NEG.words[0]+" "+Syntax.SUB.words[0]));
                        }
                        else if(Syntax.SEQZ.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.sltiu(src_add, dest_add, 1), code_current,Syntax.SEQZ.words[0]+" "+Syntax.SLTIU.words[0]));
                        }
                        else if(Syntax.SNEZ.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.sltu(0, src_add, dest_add), code_current,Syntax.SNEZ.words[0]+" "+Syntax.SLTU.words[0]));
                        }
                        else if(Syntax.SLTZ.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.slt(src_add, 0, dest_add), code_current,Syntax.SLTZ.words[0]+" "+Syntax.SLT.words[0]));
                        }
                        else if(Syntax.SGTZ.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.slt(0, src_add, dest_add), code_current,Syntax.SGTZ.words[0]+" "+Syntax.SLT.words[0]));
                        }
                        else if(Syntax.SWP.contains(token))
                        {
                            /* src=a, dest = b, * is bitwise xor
                            src = src * dest        //src = a*b
                            dest = dest * src       //dest=b*(a*b)=a
                            src = src * dest        //src=(a*b)*a=b
                             */
                            l_pc.add(new Instruction(Binary.xor(dest_add, src_add, src_add), code_current,Syntax.SWP.words[0]+" "+Syntax.XOR.words[0]));
                            l_pc.add(new Instruction(Binary.xor(src_add, dest_add, dest_add), code_current+4,Syntax.SWP.words[0]+" "+Syntax.XOR.words[0]));
                            l_pc.add(new Instruction(Binary.xor(dest_add, src_add, src_add), code_current+8,Syntax.SWP.words[0]+" "+Syntax.XOR.words[0]));
                        }
                        else throw new Exception("command type mismatch");
                        code_current+=4*Syntax.get_n_of_command(token);
                    }
                    catch(Exception e)
                    {
                        transcript.append("\nERROR: ").append(e.getMessage());
                        throw new Exception("missing arguments");
                    }
                }
                else if(Syntax.get_input_type_of_command(token)==Syntax.Ri)
                {
                    try
                    {
                        String src = sc.next();
                        transcript.append("\nsource register: ").append(src);
                        long value;
                        int src_add = Syntax.address_of_register(src);
                        if(src_add == -1)
                        {
                            throw new Exception("arguments missing");
                        }
                        if(sc.hasNextLong())
                        {
                            value = sc.nextLong();
                            transcript.append("\nimmediate value: ").append(value);
                        }
                        else
                        {
                            String label = sc.next();
                            transcript.append("\n" + "target label: ").append(label);
                            int value_c = get_address_code(label);
                            int value_d = get_address_data(label);
                            if(value_c == -1 && value_d == -1)throw new Exception("unidentified label");
                            else if(value_c == -1)
                            {
                                transcript.append(" (data)");
                                value = value_d;
                            }
                            else if(value_d == -1)
                            {
                                transcript.append(" (code)");
                                value = value_c;
                            }
                            else throw new Exception("multiple labels matching");
                        }
                        // (inci deci) li (beqz bnez bltz bgez blez bgtz) (lui auipc)
                        if(Syntax.LI.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.addi(0,src_add,value), code_current,Syntax.LI.words[0]+" "+Syntax.ADDI.words[0]));
                        }
                        else if(Syntax.INCI.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.addi(src_add,src_add,value), code_current,Syntax.INCI.words[0]+" "+Syntax.ADDI.words[0]));
                        }
                        else if(Syntax.DECI.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.addi(src_add,src_add,-value), code_current,Syntax.DECI.words[0]+" "+Syntax.ADDI.words[0]));
                        }
                        else if(Syntax.BEQZ.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.beq(0,src_add,value-code_current), code_current,Syntax.BEQZ.words[0]+" "+Syntax.BEQ.words[0]));
                        }
                        else if(Syntax.BNEZ.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.bne(0,src_add,value-code_current), code_current,Syntax.BNEZ.words[0]+" "+Syntax.BNE.words[0]));
                        }
                        else if(Syntax.BLTZ.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.blt(src_add,0,value-code_current), code_current,Syntax.BLTZ.words[0]+" "+Syntax.BLT.words[0]));
                        }
                        else if(Syntax.BGTZ.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.blt(0,src_add,value-code_current), code_current,Syntax.BGTZ.words[0]+" "+Syntax.BLT.words[0]));
                        }
                        else if(Syntax.BLEZ.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.bge(0,src_add,value-code_current), code_current,Syntax.BLEZ.words[0]+" "+Syntax.BGE.words[0]));
                        }
                        else if(Syntax.BGEZ.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.bge(src_add,0,value-code_current), code_current,Syntax.BGEZ.words[0]+" "+Syntax.BGE.words[0]));
                        }
                        else if(Syntax.LUI.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.lui(src_add,value), code_current,Syntax.LUI.words[0]));
                        }
                        else if(Syntax.AUIPC.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.auipc(src_add,value), code_current,Syntax.AUIPC.words[0]));
                        }
                        else throw new Exception("command type mismatch");
                        code_current+=4*Syntax.get_n_of_command(token);
                    }
                    catch(Exception e)
                    {
                        transcript.append("\nERROR: ").append(e.getMessage());
                        throw new Exception("missing arguments");
                    }
                }
                else if(Syntax.get_input_type_of_command(token)==Syntax.R)
                {
                    try
                    {
                        String r = sc.next();
                        transcript.append("\nregister: ").append(r);
                        int r_add = Syntax.address_of_register(r);
                        if(r_add == -1)
                        {
                            throw new Exception("arguments missing");
                        }
                        // clr noti jr
                        if(Syntax.CLR.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.add(0,0,r_add), code_current,Syntax.CLR.words[0]+" "+Syntax.AND.words[0]));
                        }
                        else if(Syntax.NOTI.contains(token))  // feeding in -1 makes every bit of value 1, a xor 1 = ~a
                        {
                            l_pc.add(new Instruction(Binary.xori(r_add,r_add,-1), code_current,Syntax.NOTI.words[0]+" "+Syntax.XORI.words[0]));
                        }
                        else if(Syntax.JR.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.jalr(r_add,0,0), code_current,Syntax.JR.words[0]+" "+Syntax.JALR.words[0]));
                        }
                        code_current+=4*Syntax.get_n_of_command(token);
                    }
                    catch(Exception e)
                    {
                        transcript.append("\nERROR: ").append(e.getMessage());
                        throw new Exception("missing arguments");
                    }
                }
                else if(Syntax.get_input_type_of_command(token)==Syntax.i)
                {
                    try
                    {
                        long value;
                        if(sc.hasNextLong())
                        {
                            value = sc.nextLong();
                            transcript.append("\nimmediate value: ").append(value);
                        }
                        else
                        {
                            String label = sc.next();
                            transcript.append("\n" + "target label: ").append(label);
                            int value_c = get_address_code(label);
                            int value_d = get_address_data(label);
                            if(value_c == -1 && value_d == -1)throw new Exception("unidentified label");
                            else if(value_c == -1)
                            {
                                transcript.append(" (data)");
                                value = value_d;
                            }
                            else if(value_d == -1)
                            {
                                transcript.append(" (code)");
                                value = value_c;
                            }
                            else throw new Exception("multiple labels matching");
                        }
                        if(Syntax.J.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.jal(0,value-code_current), code_current,Syntax.J.words[0]+" "+Syntax.JAL.words[0]));
                        }
                        else throw new Exception("command type mismatch");
                        code_current+=4*Syntax.get_n_of_command(token);
                    }
                    catch(Exception e)
                    {
                        transcript.append("\nERROR: ").append(e.getMessage());
                        throw new Exception("missing arguments");
                    }
                }
                else if(Syntax.get_input_type_of_command(token)==Syntax.$)
                {
                    try
                    {
                        // nop
                        if(Syntax.NOP.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.addi(0,0,0), code_current,Syntax.NOP.words[0]+" "+Syntax.ADDI.words[0]));
                        }
                        else if(Syntax.RET.contains(token))
                        {
                            l_pc.add(new Instruction(Binary.jalr(1,0,0), code_current,Syntax.RET.words[0]+" "+Syntax.JALR.words[0]));
                        }
                        else throw new Exception("command type mismatch");
                        code_current+=4*Syntax.get_n_of_command(token);
                    }
                    catch(Exception e)
                    {
                        transcript.append("\nERROR: ").append(e.getMessage());
                        throw new Exception("missing arguments");
                    }
                }
                else if(Syntax.get_input_type_of_command(token)==Syntax.POLY)
                {
                    if(Syntax.JAL.contains(token))//TODO fill this up
                    {
                        // r,i or i
                    }
                    else if(Syntax.JALR.contains(token))//TODO fill this up
                    {
                        // r or r,i or r,i(r)
                    }
                    else throw new Exception("command type mismatch");
                    code_current+=4*Syntax.get_n_of_command(token);
                }
            }
            else if(Syntax.is_label(token))transcript.append("\ncode label");
            else throw new Exception("unrecognized token");
        }
    }

}
