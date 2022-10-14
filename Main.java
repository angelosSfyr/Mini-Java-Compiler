import syntaxtree.*;
import visitor.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Main {
    public static SymbolTable ST;
    public static void main(String[] args) throws Exception {
        
        if (args.length == 0) {
            System.err.println("Usage: java Main filename1 filename2 ....");
            System.exit(1);
        }
        FileInputStream fis = null;
        for (String fileName : args) {
            try{
                ST = new SymbolTable();
                fis = new FileInputStream(fileName);
                MiniJavaParser parser = new MiniJavaParser(fis);
                MyVisitor eval = new MyVisitor();
                CheckingVisitor checkingVisitor = new CheckingVisitor();
                LLVMVisitor llvm = new LLVMVisitor();
                llvm.LLVMFile(fileName);
                Goal root = parser.Goal(); 
                try{
                    try {
                        root.accept(eval, null);
                        root.accept(checkingVisitor, null);
                        root.accept(llvm, null);
                    } catch (Exception ex) {    
                        System.err.println(ex.getMessage());
                    }
                } catch (Exception ex){
                    System.out.println(ex.getMessage());
                }
            }catch (ParseException ex) {// end big try
                System.out.println(ex.getMessage());
            } catch (FileNotFoundException ex) {
                System.err.println(ex.getMessage());
            } finally {
                try {
                    if (fis != null) fis.close();
                } catch (IOException ex) {
                    System.err.println(ex.getMessage());
                }
            }
        }  
        
    }
}