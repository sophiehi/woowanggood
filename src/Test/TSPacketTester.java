package Test;

/**
 * Created by SophiesMac on 15. 5. 1..
 */
import java.io.*;
public class TSPacketTester {
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static void main(String[] args) throws Exception{
        System.out.println("");
        FileInputStream fis = new FileInputStream("./movie_new.ts");
        byte [] data = new byte[188];
        char [] charArray  = new char[32];
        char [] charArray2 = new char[48];
        char [] charArray3 = new char[32];
        for(int i=0 ; i< 200 ; i++){
            //System.out.println("file size(?): " + fis.getChannel().size());
            fis.read(data, 0, 188);
            System.out.println("Packet number: "+ i);

            //10th bit from TS packet header : 2번째 바이트의 2번째 비트
            int payloadUnitStartIndicator = (data[1] >>> 6) & 0x01;
            System.out.println("Start indicator : "+ payloadUnitStartIndicator);

            //29-32th bit from TS packet header: 4번째 바이트의 5-8번째 비트
            int continuityCounter = data[3] & 0x0F;
            System.out.println("Continuity counter : "+ continuityCounter);

            int pid = ((data[1] & 0x1F) * 256) + (data[2] & 0xFF) ;
            System.out.println("PID : "+ pid);

            StringBuffer rawBit = new StringBuffer();
            StringBuffer pesHeader = new StringBuffer();
            StringBuffer pesPayload = new StringBuffer();
            char[] hexChars = new char[data.length * 2];
            for ( int j = 0; j < data.length; j++  ) {
                int v = data[j] & 0xFF;
                hexChars[j * 2] = hexArray[v >>> 4];
                hexChars[j * 2 + 1] = hexArray[v & 0x0F];
                //print TS header which is first 4 bytes
                if( j < 4 ){
                    String binaryString = String.format("%8s", Integer.toBinaryString(data[j] & 0xFF)).replace(' ', '0');
                    rawBit.append(binaryString + "");//
                }

                // print PES header which is second (4~9th) 6 byes.
                if( j > 3  && j < 10){
                    String binaryString = String.format("%8s", Integer.toBinaryString(data[j] & 0xFF)).replace(' ', '0');
                    pesHeader.append(binaryString + "");//
                }

                // print SOME of PES payloads (10~ 13th) , 4 bytes.
                if( j > 9  && j < 14){
                    String binaryString = String.format("%8s", Integer.toBinaryString(data[j] & 0xFF)).replace(' ', '0');
                    pesPayload.append(binaryString + "");//
                }

            }
            //print header in binary .
            System.out.println("S(8)|err(1)|start(1)|pri(1)|pid(13)|scr(2)|ada(2)|continue(4) : TS Header(4 bytes)");
            //System.out.println("\n"+rawBit+" (header, 4 bytes)");
            //if ada == 11 , continuity doesn't increase.
            charArray = rawBit.toString().toCharArray();
            for(int k=0; k<charArray.length; k++){
                System.out.print(charArray[k]);
                if (k ==7 || k==8 || k==9 || k==10|| k==23||k==25|| k==27) {
                    System.out.print(" ");
                }
            }
            System.out.println(" (TS header, 4 bytes)");

            // PRINT PES HEADER
            charArray2 = pesHeader.toString().toCharArray();
            for(int k=0; k<charArray2.length ; k++){
                System.out.print(charArray2[k]);
            }
            System.out.println(" (PES header, 6 bytes)");

            // PRINT PES PAYLOAD
            charArray3 = pesPayload.toString().toCharArray();
            for(int k=0; k<charArray3.length; k++){
                System.out.print(charArray[k]);
            }
            System.out.println(" (ES(= PES payloads), 4 bytes)\n");


            //print 2 chars per 1 bytes
            for(int j=0; j<data.length*2; j+=2){
                if (j % 48 == 0 ) System.out.println("");
                if (j == 8) System.out.print("\\");
                if (j == 20) System.out.print("\\");
                System.out.printf("%c%c ", hexChars[j], hexChars[j+1]);
            }
            System.out.println("(188 bytes per packet)");
            System.out.println("");
            System.out.println("");
        }
    }
}