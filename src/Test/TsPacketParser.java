//package Test;

/**
 * Created by SophiesMac on 15. 4. 4..
 */
import java.io.*;

public class TsPacketParser{
    //ref (TS: CBR) : http://soominho.tistory.com/214
    //ref (PES: ??)

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static void main(String[] args) throws Exception{
        System.out.println("");
        //FileInputStream fis = new FileInputStream("./movie_new.ts");
        FileInputStream fis = new FileInputStream( "/Users/swchoi06/IdeaProjects/woowanggood/movie.ts");

        byte [] data = new byte[188];
        char [] charArray = new char[33];
        for(int i=0; i< 200; i++){
            //System.out.println("file size(?): " + fis.getChannel().size());
            fis.read(data, 0, 188);
            System.out.println("Packet number: "+ i);

            //헤더의 10번째 자리수의 비트: 2번째 바이트의 2번째 비트
            int payloadUnitStartIndicator = (data[1] >>> 6) & 0x01;
            System.out.println("Start indicator : "+ payloadUnitStartIndicator);

            //헤더의 29-32번째 자리수의 비트: 4번째 바이트의 5-8번째 비트 
            int continuityCounter = data[3] & 0x0F; 
            System.out.println("Continuity counter : "+ continuityCounter);

            StringBuffer rawBit = new StringBuffer();
            char[] hexChars = new char[data.length * 2];
            for ( int j = 0; j < data.length; j++  ) {
                int v = data[j] & 0xFF;
                hexChars[j * 2] = hexArray[v >>> 4];
                hexChars[j * 2 + 1] = hexArray[v & 0x0F];
                //print header which is first 4 bytes
                if( j < 4 ){
                    String binaryString = String.format("%8s", Integer.toBinaryString(data[j] & 0xFF)).replace(' ', '0');
                    rawBit.append(binaryString + "");//
                }
            }
            //print header in binary .
            System.out.println("S(8)|err(1)|start(1)|pri(1)|pid(13)|scr(2)|ada(2)|continue(4)");
            //System.out.println("\n"+rawBit+" (header, 4 bytes)"); 
            //if ada == 11 , continuity doesn't increase.
            charArray = rawBit.toString().toCharArray();
            for(int k=0; k<charArray.length; k++){
                System.out.print(charArray[k]);
                if (k ==7 || k==8 || k==9 || k==10|| k==23||k==25|| k==27) {
                    System.out.print(" ");
                }
            }
            System.out.println(" (header, 4 bytes)");
            //print 2 chars per 1 bytes
            for(int j=0; j<data.length*2; j+=2){
                if (j % 48 == 0 ) System.out.println("");
                if (j == 8) System.out.print("\\");
                System.out.printf("%c%c ", hexChars[j], hexChars[j+1]);
            }
            System.out.println("(188 bytes per packet)");
            System.out.println("");
            System.out.println("");
        }
    }

}