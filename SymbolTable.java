import java.util.*; 
import java.io.*;

public class SymbolTable {

    // offset stuff
    private int fieldOffset;
    private int methodOffset;
    // <class name, offset of last field/method>
    private Hashtable<String, Integer> lastClassFieldOffset;    
    private Hashtable<String, Integer> lastClassMethodOffset;
    private Hashtable <String, Integer> offsets;


    private Hashtable <String, MiniJavaClass> classes;
    private String currentClass;
    private String currentMethod;


    SymbolTable()
    {
        this.fieldOffset = 0;
        this.methodOffset = 0;
        this.lastClassFieldOffset = new Hashtable<String, Integer>();
        this.lastClassMethodOffset = new Hashtable<String, Integer>();
        this.offsets = new Hashtable <String, Integer>();

        this.classes = new Hashtable <String, MiniJavaClass>();
        this.currentClass= "";
        this.currentMethod = "";
    }


    public void enterClass(String name){
        this.currentClass = name;
    }
    // fix
    public String enterMethod(String name){
        this.currentMethod = name;
        return this.currentMethod; // return real class name
    }
    public void exitMethod(){
        this.currentMethod = "";
    }
    
    public void insertClass()
    {   
        this.fieldOffset = 0;
        this.methodOffset = 0;
        this.lastClassFieldOffset.put(this.currentClass, 0);
        this.lastClassMethodOffset.put(this.currentClass, 0);

        MiniJavaClass classObject = new MiniJavaClass(this.currentClass);
        this.classes.put(this.currentClass, classObject);
    }

    public void insertExtendedClass(String parentName) throws Exception{
        MiniJavaClass parent = this.classes.get(parentName);
        if (parent == null){
            throw new Exception("ClassExtendsDeclaration: Class"+parentName+"has not been declared yet"+"("+Main.ST.getCurrentMethodName()+")");
        }
        
        this.fieldOffset = this.lastClassFieldOffset.get(parentName);
        this.methodOffset = this.lastClassMethodOffset.get(parentName);
        this.lastClassFieldOffset.put(this.currentClass, this.fieldOffset);
        this.lastClassMethodOffset.put(this.currentClass, this.methodOffset);
        
        MiniJavaExtendedClass child = new MiniJavaExtendedClass(this.currentClass, parent);
        this.classes.put(this.currentClass, child);
    }

    
    public void insertMethod(String type)throws Exception{
        boolean parentHasMethod=false;
        MiniJavaMethod method = new MiniJavaMethod(this.currentClass, this.currentMethod, type);
        MiniJavaClass methodClass = this.classes.get(this.currentClass);
        // check if the parent has method with the same name [RETURN TYPE CHECK]
        String parentclassName = methodClass.getParentClassName();
        if (parentclassName != ""){
            MiniJavaClass parentClass = this.classes.get(parentclassName);
            if (parentClass.hasMethod(this.currentMethod)){
                parentHasMethod=true;
                String parentReturnType = parentClass.getMethodReturnType(this.currentMethod);
                if (parentReturnType != type){
                    throw new Exception("MethodDeclaration: Different return type with parent "+"("+Main.ST.getCurrentMethodName()+")");
                }
            }
        }
        if (!parentHasMethod){
            this.offsets.put(this.currentClass+"."+this.currentMethod+"()", this.methodOffset);
            this.methodOffset += 8;
            this.lastClassMethodOffset.put(this.currentClass, this.methodOffset);
        }
        methodClass.insertClassMethod(method);
    }

    public void insertMethodArgs(String args) throws Exception {
        MiniJavaClass methodClass = this.classes.get(this.currentClass);
        // check if the parent has method with the same name [ARGS CHECK]
        String parentclassName = methodClass.getParentClassName();
        if (parentclassName != ""){
            MiniJavaClass parentClass = this.classes.get(parentclassName);
            if (parentClass.hasMethod(this.currentMethod)){
                String parentArgs= parentClass.getMethodArgs(this.currentMethod);
                if (!args.equals(parentArgs)){
                    throw new Exception("MethodDeclaration: Different arguments with parent "+"("+Main.ST.getCurrentMethodName()+")");
                }
            }
        }
        methodClass.insertArgs(this.currentMethod, args);
    }
    
    private int getOffestForType(String type){
        if (type=="int"){
            return 4;
        }else if(type == "boolean"){
            return 1;
        }
        return 8;
    }

    public void insertPrimitive(String idName, String idType)
    {
        MiniJavaClass c = this.classes.get(this.currentClass);
        if (this.currentMethod == ""){
            int offset = this.getOffestForType(idType);
            this.offsets.put(this.currentClass+"."+idName, this.fieldOffset);
            this.fieldOffset += offset;
            this.lastClassFieldOffset.put(this.currentClass, this.fieldOffset);
            c.insertField(idName, idType);
        }else{
            c.insertMethodVariable(this.currentMethod, idName, idType);
        }
    }

    public void print(){
        Iterator<Map.Entry<String, MiniJavaClass>> itr = this.classes.entrySet().iterator();
        Map.Entry<String, MiniJavaClass> entry = null;
        System.out.println("---------------------------------------------------------");
        while(itr.hasNext()){
            entry = itr.next();
            System.out.println( entry.getKey() + " --> " + entry.getValue());
            System.out.println("---------------------------------------------------------");
        }
    }

    public void printOffsets()
    {
        Iterator<Map.Entry<String, Integer>> itr = this.offsets.entrySet().iterator();
        Map.Entry<String, Integer> entry = null;
        while(itr.hasNext()){
            entry = itr.next();
            System.out.println( entry.getKey() + ": " + entry.getValue());
        }
    }

    public String lookup(String idName)
    {
        MiniJavaClass currentClass = this.classes.get(this.currentClass);
        return currentClass.lookupVariable(idName, this.currentMethod);
    }

    public String getCurrentClassName(){
        return this.currentClass;
    }

    public String getCurrentMethodName(){
        return this.currentMethod;
    }
    
    public boolean existsClass(String className){
        return this.classes.containsKey(className);
    }

    public boolean childHasParent(String childClassName, String parentclassName){
        MiniJavaClass child = this.classes.get(childClassName);
        while(child.getParentClassName() != ""){
            if (child.getParentClassName().equals(parentclassName)) return true;
            child = this.classes.get(child.getParentClassName());
        }
        return false;
    }

    public boolean classHadMethod(String className, String methodName) throws Exception {
        MiniJavaClass methodClass = this.classes.get(className);
        if (className == null){
            throw new Exception("classObjectError: class does not exist"+"("+Main.ST.getCurrentMethodName()+")");
        }
        return methodClass.hasMethod(methodName);
    }

    public String getClassMethodType(String className, String methodName){
        // certified that class and method exist
        MiniJavaClass methodClass = this.classes.get(className);
        return methodClass.getMethodReturnType(methodName);
    }
    
    public String getClassMethodArgs(String className, String methodName){
        // certified that class and method exist
        MiniJavaClass methodClass = this.classes.get(className);
        return methodClass.getMethodArgs(methodName);
    }

    public String getClassMethodOwner(String className, String methodName){
        // certified that class and method exist
        MiniJavaClass methodClass = this.classes.get(className);
        return methodClass.getMethodOwner(methodName);
    }

    public int getClassSize(String className){
        // certified that class exists
        MiniJavaClass cl = this.classes.get(className);
        return cl.getSize();
    }

    public int getNumberOfMethods(String className){
        // certified that class exists
        MiniJavaClass cl = this.classes.get(className);
        return cl.getNumberOfMethods();
    }

    public String llvmArgs(String args){
        String[] array = args.split(",");
        if (args.equals("")) return args;
        String llvmArgs= ", ";
        for (int i=0; i<array.length; i++){
            llvmArgs += LLVMVisitor.getLLtype(array[i]) + ",";
        }
        return llvmArgs.substring(0, llvmArgs.length() - 1);
    }

    public String getVtablesCode(){
        Iterator<Map.Entry<String, MiniJavaClass>> itr = this.classes.entrySet().iterator();
        Map.Entry<String, MiniJavaClass> currClass = null;
        String code = "";
        while(itr.hasNext()){
            currClass = itr.next(); 
            MiniJavaClass cl = currClass.getValue();
            if (cl.hasMethod("main")){
                continue;
            }
            String className = currClass.getKey();
            int numberOfMethods = cl.getNumberOfMethods();
            code += "@." + className + "_vtable = global [" + numberOfMethods + " x i8*] [\n";
            ArrayList<String> methodNames = cl.getMethodNames(); 
            for(int i=0; i<methodNames.size(); i++){
                String methodName = methodNames.get(i);
                String methodType = Main.ST.getClassMethodType(className, methodName);
                String methodArgs = Main.ST.getClassMethodArgs(className, methodName);
                String methodOwner = Main.ST.getClassMethodOwner(className, methodName);
                code += "\ti8* bitcast (" + LLVMVisitor.getLLtype(methodType) + " (i8* "+ llvmArgs(methodArgs) + ")* @" + methodOwner+ "." + methodName + " to i8*)";
                if (!(i == methodNames.size()-1)) code += ",";
                code +="\n";
            }
            code += "]\n";
        }
        return code;
    }

    public boolean idetifierIsClassField(String idName){
        // we are in a metod of a class at the time of execution
        MiniJavaClass currentClass = this.classes.get(this.currentClass);
        return currentClass.idetifierIsClassField(idName, this.currentMethod);
    }

    public int getFieldOffset(String className, String fieldName){
        MiniJavaClass currClass = this.classes.get(className);
        String offsetString = className + "." + fieldName;
        Integer offset = this.offsets.get(offsetString);
        while(offset == null){
            className = currClass.getParentClassName();
            currClass = this.classes.get(className);
            offsetString = className + "." + fieldName;
            offset = this.offsets.get(offsetString);
        }
        return offset;
    }

    public int getMethodOffset(String className, String methodName){
        MiniJavaClass currClass = this.classes.get(className);
        String offsetString = className + "." + methodName + "()";
        Integer offset = this.offsets.get(offsetString);
        while(offset == null){
            className = currClass.getParentClassName();
            currClass = this.classes.get(className);
            offsetString = className + "." + methodName + "()";
            offset = this.offsets.get(offsetString);
        }
        return offset;
    }
}