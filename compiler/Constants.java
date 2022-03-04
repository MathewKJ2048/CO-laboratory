

public class Constants
{
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
    }
    public static final String MULTI_LINE_START = "#*";
    public static final String MULTI_LINE_END = "*#";
    public static final String SINGLE_LINE = "#";
    public static final Keyword DATA = new Keyword(new String[]{"data"});
    public static final Keyword CODE = new Keyword(new String[]{"code","text"});
    public static final char LABEL_TERMINATOR = ':';
    public static final char DATATYPE_INITIATOR = '.';
    //
    public static final String WORD = "word";
    public static final String SHORT = "half";
    public static final String BYTE = "byte";
    public static final String SPACE = "space";
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
}
