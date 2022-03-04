
import com.sun.jdi.ArrayReference;

import java.util.*;
import java.io.*;

public class CyberKnight{
    static Database D = new Database();
    static Instruction_SET I = new Instruction_SET();
    static Extract_Reg R=new Extract_Reg();
    static Operations O = new Operations();
    public static void main(String[] args) throws FileNotFoundException {
        int i=Read("Test.txt");
        int j;
        Arrays.fill(D.Mem,(byte)0);
        for(j=0;j<(i/4);j++){
            O.IF();
            O.IDRF();
            O.EXE();
        }

        for(int k=0;k<32;k++){
            System.out.println("R"+k+"="+D.R[k]);
        }
        for(int k=0;k<400;k=k+4){
            int z=(D.Mem[k]&0xFF)<<24|(D.Mem[k+1]&0xFF)<<16|(D.Mem[k+2]&0xFF)<<8|(D.Mem[k+3]&0xFF);
            System.out.println("Memory["+k+":"+(k+3)+"] = "+z);
        }

    }
    public static int Read(String addr) throws FileNotFoundException {
        int i=0;
        File filescan = new File(addr);
        Scanner scan = new Scanner(filescan);
        String s;
        while(scan.hasNext()){
            s=scan.next();
            int k= UTIL.toDecimal(s);
            D.InstrMem[i]=(byte)(k>>24);
            D.InstrMem[i+1]=(byte)(k>>16);
            D.InstrMem[i+2]=(byte)(k>>8);
            D.InstrMem[i+3]=(byte)(k);
            i=i+4;
        }
        D.PC=0;
        return i;
    }

}
 class Database{
    int[] R=new int[32];
    int PC;
    Byte[] InstrMem=new Byte[1024];
    Byte[] Mem = new Byte[5000];
    String Inst_Type(String s){
        return switch (s) {
            case "0110011" -> "R";
            case "0010011", "0000011" -> "I";
            case "0100011" -> "S";
            case "1100011" -> "B";
            case "0110111" -> "U";
            default -> "J";
        };
    }
}
class Instruction_SET{
    public int Find_Inst(String s,String s1){
        return switch (s1) {
            case "R" -> Set_R(s);
            case "I" -> Set_I(s);
            case "S" -> Set_S(s);
            case "B" -> Set_B(s);
            case "U" -> Set_U(s);
            default -> Set_J(s);
        };
    }
    public int Set_R(String s){
        String sub =s.substring(0,7) +s.substring(17,20);
        return switch (sub) {
            case "0000000000" -> 0;
            case "0100000000" -> 1;
            case "0000000001" -> 2;
            case "0000000010" -> 3;
            case "0000000011" -> 4;
            case "0000000100" -> 5;
            case "0000000101" -> 6;
            case "0100000101" -> 7;
            case "0000000110" -> 8;
            default -> 9;
        };
    }

    public int Set_I(String s){
        String sub =  s.substring(17,20);
        if(s.startsWith("0010011", 25)) {
            switch (sub) {
                case "000":
                    return 10;
                case "010":
                    return 11;
                case "011":
                    return 12;
                case "100":
                    return 13;
                case "110":
                    return 14;
                case "111":
                    return 15;
                case "001":
                    return 16;
                default:
                    if (s.startsWith("000000")) {
                        return 17;
                    } else {
                        return 18;
                    }
            }
        }
        else {
            return switch (sub) {
                case "000" -> 19;
                case "001" -> 20;
                case "010" -> 21;
                case "100" -> 22;
                default -> 23;
            };

        }

    }
    public int Set_S(String s){
        String sub=s.substring(17,20);
        if(sub.equals("000")){
            return 24;
        }
        else if(sub.equals("001")){
            return 25;
        }
        else{
            return 26;
        }

    }
    public int Set_B(String s){
        String sub=s.substring(17,20);
        return switch (sub) {
            case "000" -> 27;
            case "001" -> 28;
            case "100" -> 29;
            case "101" -> 30;
            case "110" -> 31;
            default -> 32;
        };

    }
    public int Set_U(String s){
        return 33;

    }
    public int Set_J(String s){
        String sub=s.substring(17,20);
        if(sub.equals("000")){
            return 34;
        }
        else{
            return 35;
        }
    }
}
class Extract_Reg{
    public void Get_Reg(String s,String s1,int[] Reg){
        switch (s1) {
            case "R" -> Reg_R(Reg, s);
            case "I" -> Reg_I(Reg, s);
            case "S" -> Reg_S(Reg, s);
            case "B" -> Reg_B(Reg, s);
            case "U" -> Reg_U(Reg, s);
            default -> Reg_J(Reg, s);
        }
    }
    public void Reg_R(int[] Reg,String s){
        Reg[0] = UTIL.toDecimal(s.substring(20,25));
        Reg[1] = UTIL.toDecimal(s.substring(7,12));
        Reg[2] = UTIL.toDecimal(s.substring(12,17));
    }
    public void Reg_I(int[] Reg,String s){
        Reg[0] = UTIL.toDecimal(s.substring(20,25));
        Reg[2] = UTIL.toDecimal(s.substring(12,17));
    }
    public void Reg_S(int[] Reg,String s){
        Reg[1] = UTIL.toDecimal(s.substring(7,12));
        Reg[2] = UTIL.toDecimal(s.substring(12,17));
    }
    public void Reg_B(int[] Reg,String s){
        Reg[1] = UTIL.toDecimal(s.substring(7,12));
        Reg[2] = UTIL.toDecimal(s.substring(12,17));
    }
    public void Reg_U(int[] Reg,String s){
        Reg[0] = UTIL.toDecimal(s.substring(20,25));
    }
    public void Reg_J(int[] Reg,String s){
        Reg[0] = UTIL.toDecimal(s.substring(20,25));
    }
}
class Operations{
    Database D = CyberKnight.D;
    Instruction_SET I = CyberKnight.I;
    Extract_Reg R = CyberKnight.R;
    String IF_BUFF;
    int EXE_BUFF,MEM_BUFF;
    int[] IDRF_BUFF= new int[4];
    int[] Reg = new int[3];
    String S;
    public void IF(){

        IF_BUFF= new String();
        for(int i=3;i>=0;i--){
            for(int j=0;j<8;j++){
                if(((D.InstrMem[D.PC+i]>>j)&1)==1){
                    IF_BUFF='1'+IF_BUFF;
                }
                else {
                    IF_BUFF = '0' + IF_BUFF;
                }
            }
        }

    }
    public void IDRF(){
        S = IF_BUFF;
        String Type = D.Inst_Type(S.substring(25,32));
        IDRF_BUFF[0]=I.Find_Inst(S,Type);
        R.Get_Reg(S,Type,Reg);
        IDRF_BUFF[1]=Reg[0];
        IDRF_BUFF[2]=Reg[1];
        IDRF_BUFF[3]=Reg[2];
    }
    public void EXE(){
        int op=IDRF_BUFF[0];
        int rs1=D.R[IDRF_BUFF[3]];
        int rs2=D.R[IDRF_BUFF[2]];
        int offset=UTIL.toDecimal(S.substring(0,12));
        int offset2=UTIL.toDecimal(S.substring(0,7)+S.substring(20,25));
        int Bimm=4*UTIL.SignToDecimal(S.charAt(0)+S.charAt(24)+S.substring(1,7)+S.substring(20,24));
        if(op==0){
            D.R[IDRF_BUFF[1]]=rs1+rs2;
            D.PC=D.PC+4;
        }
        else if(op==1){
            D.R[IDRF_BUFF[1]]=rs1-rs2;
            D.PC=D.PC+4;
        }
        else if(op==2){
            D.R[IDRF_BUFF[1]]=rs1<<(rs2)/32;
            D.PC=D.PC+4;
        }
        else if(op==3){
            if(rs1<rs2){
                D.R[IDRF_BUFF[1]]=1;
                D.PC=D.PC+4;
            }
            else{
                D.R[IDRF_BUFF[1]]=0;
                D.PC=D.PC+4;
            }
        }
        else if(op==4){
            if(rs1<rs2){
                D.R[IDRF_BUFF[1]]=1;
                D.PC=D.PC+4;
            }
            else{
                D.R[IDRF_BUFF[1]]=0;
                D.PC=D.PC+4;
            }
        }

        else if(op==5){
            D.R[IDRF_BUFF[1]] = rs1^rs2;
            D.PC=D.PC+4;
        }
        else if(op==6){
            D.R[IDRF_BUFF[1]]=rs1>>(rs2)/32;
            D.PC=D.PC+4;
        }
        else if(op==7){
            if(D.R[IDRF_BUFF[1]]>0){
                D.R[IDRF_BUFF[1]]=rs1>>(rs2)/32;

            }
            else{
                D.R[IDRF_BUFF[1]]=-(rs1>>(rs2)/32);
            }
            D.PC=D.PC+4;
        }
        else if(op==8){
            D.R[IDRF_BUFF[1]]=rs1|rs2;
            D.PC=D.PC+4;
        }
        else if(op==9){
            D.R[IDRF_BUFF[1]]=rs1&rs2;
            D.PC=D.PC+4;
        }
        else if(op==10){
            D.R[IDRF_BUFF[1]]=rs1+UTIL.SignToDecimal(S.substring(0,12));
            D.PC=D.PC+4;
        }
        else if(op==11){
            if(rs1<UTIL.SignToDecimal(S.substring(0,12))){
                D.R[IDRF_BUFF[1]]=1;
            }
            else{
                D.R[IDRF_BUFF[1]]=0;
            }
            D.PC=D.PC+4;
        }
        else if(op==12){
            if(rs1<UTIL.toDecimal(S.substring(0,12))){
                D.R[IDRF_BUFF[1]]=1;
            }
            else{
                D.R[IDRF_BUFF[1]]=0;
            }
            D.PC=D.PC+4;
        }
        else if(op==13){
            D.R[IDRF_BUFF[1]]= rs1^Integer.parseInt(S.substring(0,12));
            D.PC=D.PC+4;
        }
        else if(op==14){
            D.R[IDRF_BUFF[1]]= rs1|Integer.parseInt(S.substring(0,12));
            D.PC=D.PC+4;
        }
        else if(op==15){
            D.R[IDRF_BUFF[1]]= rs1&Integer.parseInt(S.substring(0,12));
            D.PC=D.PC+4;
        }
        else if(op==16){
            D.R[IDRF_BUFF[1]]= rs1<<UTIL.toDecimal(S.substring(7,12));
            D.PC=D.PC+4;
        }
        else if(op==17){
            D.R[IDRF_BUFF[1]]= rs1>>UTIL.toDecimal(S.substring(7,12));
            D.PC=D.PC+4;
        }
        else if(op==18){
            if(D.R[IDRF_BUFF[1]]>0){
                D.R[IDRF_BUFF[1]]=rs1>>UTIL.toDecimal(S.substring(7,12));
            }
            else{
                D.R[IDRF_BUFF[1]]=-(rs1>>UTIL.toDecimal(S.substring(7,12)));
            }
            D.PC=D.PC+4;
        }
        else if(op==19){
            if(D.Mem[offset+rs1]>0) {
                D.R[IDRF_BUFF[1]] = D.Mem[offset + rs1];
            }
            else{
                D.R[IDRF_BUFF[1]] = D.Mem[offset + rs1];
                D.R[IDRF_BUFF[1]]=-D.R[IDRF_BUFF[1]];
            }
            D.PC=D.PC+4;
        }
        else if(op==20){
            if(D.Mem[offset+rs1]>0) {
                D.R[IDRF_BUFF[1]] = (D.Mem[offset + rs1]&0xFF) << 8 | D.Mem[offset + rs1 + 1]&0xFF;
            }
            else{
                D.R[IDRF_BUFF[1]] = (D.Mem[offset + rs1]&0xFF) << 8 | D.Mem[offset + rs1 + 1]&0xFF;
                D.R[IDRF_BUFF[1]]=-D.R[IDRF_BUFF[1]];
            }
            D.PC=D.PC+4;
        }
        else if(op==21){
            D.R[IDRF_BUFF[1]]=(D.Mem[offset+rs1]&0xFF)<<24|(D.Mem[offset+rs1+1]&0xFF)<<16|(D.Mem[offset+rs1+2]&0xFF)<<8|D.Mem[offset+rs1+3]&0xFF;
            D.PC=D.PC+4;
        }
        else if(op==22){
            D.R[IDRF_BUFF[1]]=D.Mem[offset+rs1];
            D.PC=D.PC+4;
        }
        else if(op==23){
            D.R[IDRF_BUFF[1]]=(D.Mem[offset+rs1]&0xFF)<<8|D.Mem[offset+rs1+1];
            D.PC=D.PC+4;
        }
        else if(op==24){
            D.Mem[offset2+rs1]=(byte)rs2;
            D.PC=D.PC+4;
        }
        else if(op==25){
            D.Mem[offset2+rs1]=(byte)(rs2>>8);
            D.Mem[offset2+rs1+1]=(byte)(rs2);
            D.PC=D.PC+4;
        }
        else if(op==26){
            D.Mem[offset2+rs1]=(byte)(rs2>>24);
            D.Mem[offset2+rs1+1]=(byte)(rs2>>16);
            D.Mem[offset2+rs1+2]=(byte)(rs2>>8);
            D.Mem[offset2+rs1+3]=(byte)(rs2);
            D.PC=D.PC+4;
        }
        else if(op==27){
            if(rs1==rs2){
                D.PC=D.PC+Bimm;
            }
        }
        else if(op==28){
            if(rs1!=rs2){
                D.PC=D.PC+Bimm;
            }
        }
        else if(op==29){
            if(rs1<rs2){
                D.PC=D.PC+Bimm;
            }
        }
        else if(op==30){
            if(rs1>=rs2){
                D.PC=D.PC+Bimm;
            }
        }
        else if(op==31){
            if(rs1<rs2){
                D.PC=D.PC+Bimm;
            }
        }
        else if(op==32){
            if(rs1>=rs2){
                D.PC=D.PC+Bimm;
            }
        }
        else if(op==33){
            D.R[IDRF_BUFF[1]]=UTIL.SignToDecimal(S.substring(0,20));
            D.PC=D.PC+4;
        }
        else if(op==34){
            D.R[IDRF_BUFF[1]]=D.PC+4;
            D.PC=D.PC+4*UTIL.SignToDecimal(S.charAt(0)+S.substring(12,20)+S.charAt(11)+S.substring(1,11));
        }
        else{
            D.PC=D.PC+4*UTIL.SignToDecimal(S.charAt(0)+S.substring(10,21)+S.charAt(9)+S.substring(1,9)+"000000000000");
        }
    }
}
class UTIL{
     static int toDecimal(String s){
         int sum=0;
         for(int i=0;i<s.length();i++){
             sum=sum*2+s.charAt(i)-'0';
         }
         return sum;
     }
     static int SignToDecimal(String s){
         int sum=0;
         int sum1;
         for(int i=0;i<s.length();i++){
             sum=sum*2+s.charAt(i)-'0';
         }
         sum1=sum - (s.charAt(0)-'0')*(int)Math.pow(2,s.length()-1);
         return sum1;
     }
}
