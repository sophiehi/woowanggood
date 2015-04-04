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
        FileInputStream fis = new FileInputStream("./movie.ts");
        byte [] data = new byte[188];

        for(int i=0; i< 100; i++){
            System.out.println("file size(?): " + fis.getChannel().size());
            fis.read(data, 0, 188);
            System.out.printf("%d th packet\n", i);
            System.out.println("(188 bytes per packet)");

            //ex) 11번째 바이트의 (왼쪽에) 6번째 비트가 1인지 확인
            //ex) (data[10] >>> 5) && 0x01;

            //헤더의 10번째 자리수의 비트: 2번째 바이트의 2번째 비트
            int payloadUnitStartIndicator = (data[1] >>> 1) & 0x01;
            System.out.println("start?: "+ payloadUnitStartIndicator);

            //헤더의 29-32번째 자리수의 비트: 3번째 바이트의 5-8번째 비트
            int continuityCounter = data[2] & 0x0F;
            System.out.println("continue?: "+ continuityCounter);

            StringBuffer rawBit = new StringBuffer();
            char[] hexChars = new char[data.length * 2];
            for ( int j = 0; j < data.length; j++  ) {
                int v = data[j] & 0xFF;
                hexChars[j * 2] = hexArray[v >>> 4];
                hexChars[j * 2 + 1] = hexArray[v & 0x0F];
                if( j < 4 ){
                    String binaryString = String.format("%8s", Integer.toBinaryString(data[j] & 0xFF)).replace(' ', '0');
                    rawBit.append(binaryString + " ");//
                }
            }

            for(int j=0; j<data.length*2; j+=2){
                if( j == 8 ) {
                    System.out.println("\n"+rawBit+"\n");
                }
                System.out.printf("%c%c ", hexChars[j], hexChars[j+1]);
            }

            System.out.println("");
            System.out.println("");
        }
    }
}