/**
 * Testing how to read file in Java.
 *
 * vim tips:
 * 
 * set map to compile and run.
 * nnoremap \cr :!javac -verbose %<CR>:!java -cp %:p:h %:t:r<CR>
 *
 */

import java.io.*;
import java.util.Scanner;

class FileRead{

    public static void main(String[] args) {

        try {

            System.out.println("Test reading file!");

            File file = new File("/Users/xiangchen/rd/java-sandbox/simple-java/README.md");
            //File file = new File("~/rd/java-sandbox/simple-java/README.md");

            System.out.println("File Path:");
            System.out.println(file.getPath());
            System.out.println("File AbsolutePath:");
            System.out.println(file.getAbsolutePath());

            Scanner fileReader = new Scanner(file);

            // read the first line to make sure!
            String line = fileReader.nextLine();
            System.out.println("The first line:");
            System.out.println(line);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
