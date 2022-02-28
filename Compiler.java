import java.nio.file.*;
import java.util.*;

public class Compiler
{
    private final int memory_start_data;
    private final int memory_start_code;
    private class Line
    {
        String contents;
        int number;
    }
    Compiler(int memory_start_data, int memory_start_code)
    {
        this.memory_start_data = memory_start_data;
        this.memory_start_code = memory_start_code;
    }
    public void compile(Path source, Path binary) throws java.io.IOException
    {
        List<String> l_raw = new ArrayList<String>();
        l_raw = Files.readAllLines(source);
        List<Line> l = new ArrayList<Line>();
        for(int i = 0;i< l_raw.size();i++)
        {
            Line li = new Line();
            li.number = i;
            li.contents = l_raw.get(i);
            l.add(li);
        }
        print(l);
        scrub(l);
        print(l);
    }
    private void scrub(List<Line> l)
    {
        remove_multi_line_comments(l);
        remove_single_line_comments(l);
        replace_tab_with_space(l);
        trim(l);
        remove_empty_lines(l);
    }
    private void remove_multi_line_comments(List<Line> l)
    {
        boolean f = false;
        for(int i = 0; i<l.size(); i++)
        {
            String c = l.get(i).contents;
            int start = c.indexOf(Constants.MULTI_LINE_START);
            int end = c.indexOf(Constants.MULTI_LINE_END);
            int size_start = Constants.MULTI_LINE_START.length();
            int size_end = Constants.MULTI_LINE_END.length();
            //System.out.println((l.get(i).number+1)+":"+start+","+end+" "+f);
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
    private void remove_single_line_comments(List<Line> l)
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
    private void remove_empty_lines(List<Line> l)
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
    private void replace_tab_with_space(List<Line> l)
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
    private void trim(List<Line> l)
    {
        for(int i = 0;i<l.size();i++)
        {
            String c = l.get(i).contents;
            c = c.trim();
            l.get(i).contents = c;
        }
    }
    public void print(List<Line> l)
    {
        for(int i=0;i<l.size();i++)
        {
            System.out.println((l.get(i).number+1<10?"0":"")+(l.get(i).number+1)+"|"+l.get(i).contents+"|");
        }
    }
    public static void test() throws java.io.IOException
    {
        Compiler c = new Compiler(0,0);
        c.compile(Paths.get("test.s"),null);
    }
}
