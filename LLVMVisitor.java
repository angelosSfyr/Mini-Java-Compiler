import syntaxtree.*;
import visitor.*;
import java.io.*;
import java.util.*;




public class LLVMVisitor extends GJDepthFirst<String, Void>{

    private File file;
    private PrintWriter writer;
    
    private int registerCounter = 0;
    private int loopCounter = 0;
    private int ifCounter = 0;
    private int andCounter = 0;
    private int arrayAllocCounter = 0;
    private int oobCounter = 0;
    // identifier visitor returns thr name else returns register(and does some stuff)
    private boolean returnIdName = true;   
    // for method calls we need the type of the primaryExpression (MessageSend)
    private boolean returnIdType = false;   
    private String  primaryExpressionType = null;

    public static int getTypeBits(String type){
        if (type.equals("int")){
            return 4;
        }else if(type.equals("boolean")){
            return 1;
        }else{
            return 8;
        }
    }

    public static String getLLtype(String type){
        if (type.equals("int")){
            return "i32";
        }else if(type.equals("boolean")){
            return "i1";
        }else if(type.equals("int[]")){
            return "i32*";
        }else if(type.equals("boolean[]")){
            return "i32*";
        }else{
            return "i8*";   // objects
        }
    }

    private String getNewRegisterName(){
        return "%_" + this.registerCounter++;
    }

    private String getNewLoopLabelName(){
        return "loop" + this.loopCounter++;
    }

    private String getNewIfLabelName(){
        return "if" + this.ifCounter++;
    }
    
    private String getNewAndLabelName(){
        return "andClause" + this.andCounter++;
    }

    private String getNewOobLabelName(){
        return "oob" + this.oobCounter++;
    }

    private String getNewArrayAllocLabelName(){
        return "arr_alloc" + this.arrayAllocCounter++;
    }

    public void LLVMFile(String fileName) throws Exception{
        // filename.java -> filename.ll
        this.file = new File(fileName.substring(0, fileName.length()-4) + "ll");
        this.file.createNewFile();
        PrintWriter writer = new PrintWriter(this.file);
        writer.print("");
        writer.close();
    }

    public void writeCode(String code) throws Exception {
        PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(this.file, true)));
        pw.print(code);
        pw.close();
    }

    public void writeHelpers() throws Exception { 

        String helpers ="declare i8* @calloc(i32, i32)\n" +
                        "declare i32 @printf(i8*, ...)\n" +
                        "declare void @exit(i32)\n" +
                        "\n" +
                        "@_cint = constant [4 x i8] c\"%d\\0a\\00\"\n" +
                        "@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"\n" +
                        "define void @print_int(i32 %i) {\n" +
                        "    %_str = bitcast [4 x i8]* @_cint to i8*\n" +
                        "    call i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n" +
                        "    ret void\n" +
                        "}\n" +
                        "\n" +
                        "define void @throw_oob() {\n" +
                        "    %_str = bitcast [15 x i8]* @_cOOB to i8*\n" +
                        "    call i32 (i8*, ...) @printf(i8* %_str)\n" +
                        "    call void @exit(i32 1)\n" +
                        "    ret void\n" + "}\n";
                        
        this.writeCode(helpers);
    }

    private void writeVtables() throws Exception {
        String vtableCode = Main.ST.getVtablesCode();
        writeCode(vtableCode);
    }
    /**
     * f0 -> MainClass()
     * f1 -> ( TypeDeclaration() )*
     * f2 -> <EOF>
     */
    public String visit(Goal n, Void argu) throws Exception {
        writeVtables();
        writeHelpers();
        n.f0.accept(this, null);
        n.f1.accept(this, null);
        return null;
    }
    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> "public"
     * f4 -> "static"
     * f5 -> "void"
     * f6 -> "main"
     * f7 -> "("
     * f8 -> "String"
     * f9 -> "["
     * f10 -> "]"
     * f11 -> Identifier()
     * f12 -> ")"
     * f13 -> "{"
     * f14 -> ( VarDeclaration() )*
     * f15 -> ( Statement() )*
     * f16 -> "}"
     * f17 -> "}"
    */
    public String visit(MainClass n, Void argu) throws Exception {
        String classname = n.f1.accept(this, null);
        Main.ST.enterClass(classname);
        Main.ST.enterMethod("main");
        writeCode("\ndefine i32 @main() {\n");
        n.f14.accept(this, null);
        n.f15.accept(this, null);
        writeCode("\n\tret i32 0\n}\n");
        Main.ST.exitMethod();
        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    public String visit(ClassDeclaration n, Void str) throws Exception {
        Main.ST.enterClass(n.f1.accept(this, null));
        n.f4.accept(this, null);
        return null;
    }
    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "extends"
     * f3 -> Identifier()
     * f4 -> "{"
     * f5 -> ( VarDeclaration() )*
     * f6 -> ( MethodDeclaration() )*
     * f7 -> "}"
     */
    public String visit(ClassExtendsDeclaration n, Void argu) throws Exception {
        Main.ST.enterClass(n.f1.accept(this, null));
        n.f6.accept(this, null);
        return null;
    }


    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    public String visit(VarDeclaration n, Void argu) throws Exception {
        String llType = getLLtype(n.f0.accept(this, null));
        String idName = n.f1.accept(this, null);
        String aloceCode =  "\t%" + idName + " = alloca " + llType + "\n";
        writeCode(aloceCode);
        return null;
    }

    public String[] argArray(String args){
        if((args==null) || args.equals(""))  return (new String[0]);
        String[] array = args.split(",");
        return array;
    }

    /**
     * f0 -> "public"
     * f1 -> Type()
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( FormalParameterList() )?
     * f5 -> ")"
     * f6 -> "{"
     * f7 -> ( VarDeclaration() )*
     * f8 -> ( Statement() )*
     * f9 -> "return"
     * f10 -> Expression()
     * f11 -> ";"
     * f12 -> "}"
     */
    public String visit(MethodDeclaration n, Void argu) throws Exception {
        String methodName = n.f2.accept(this, null);
        Main.ST.enterMethod(methodName);
        String methodType = n.f1.accept(this, null);
        String methodLLType = getLLtype(methodType);
        String methodDefine ="\ndefine "+ methodLLType + " @" + Main.ST.getCurrentClassName() + "." +  Main.ST.getCurrentMethodName() + "(i8* %this";
        // add parameters
        String[] argNames = argArray(n.f4.accept(this, null));
        String[] argTypes = argArray(Main.ST.getClassMethodArgs(Main.ST.getCurrentClassName(), methodName));
        for(int i = 0; i < argNames.length; i++){
            methodDefine += ", " + getLLtype(argTypes[i]) + " %." + argNames[i];
        }
        methodDefine += ") {\n";
        writeCode(methodDefine);
        // allocate parameters
        String paramAllocationCode = "";
        for(int i = 0; i < argNames.length; i++){
            paramAllocationCode += "\t%" + argNames[i] + " = alloca " + getLLtype(argTypes[i]) +"\n";
            paramAllocationCode += "\tstore " + getLLtype(argTypes[i]) + " %." + argNames[i] + ", " + getLLtype(argTypes[i]) + "* %" + argNames[i] + "\n";
        }
        writeCode(paramAllocationCode);
        n.f4.accept(this, null);
        n.f7.accept(this, null);
        n.f8.accept(this, null);
        String returnRegister = n.f10.accept(this, null);
        String returnCode = "\n\tret " + methodLLType + " " + returnRegister + " \n}\n";
        writeCode(returnCode);
        return null;
    }
    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    @Override
    public String visit(FormalParameterList n, Void argu) throws Exception {
        String ret = n.f0.accept(this, null);

        if (n.f1 != null) {
            ret += n.f1.accept(this, null);
        }
        return ret;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
    */
    public String visit(FormalParameterTerm n, Void argu) throws Exception {
        return n.f1.accept(this, argu);
    }

    /**
     * f0 -> ","
     * f1 -> FormalParameter()
    */
    @Override
    public String visit(FormalParameterTail n, Void argu) throws Exception {
        String ret = "";
        for ( Node node: n.f0.nodes) {
            ret += "," + node.accept(this, null);
        }
        return ret;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
    */
    @Override
    public String visit(FormalParameter n, Void argu) throws Exception{
        String identifier = n.f1.accept(this, null);
        return identifier;
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    public String visit(AssignmentStatement n, Void argu) throws Exception {
        String idName = n.f0.accept(this, null);
        String idType = Main.ST.lookup(idName);
        String llType = getLLtype(idType);
        boolean idIsField = Main.ST.idetifierIsClassField(idName);
        String code="";
        String assignmentRegister;
        if (idIsField){
            int offset = Main.ST.getFieldOffset(Main.ST.getCurrentClassName(), idName)  + 8;
            String getFieldRegister  = getNewRegisterName();
            String castRegister = getNewRegisterName();
            code += "\t" + getFieldRegister + " = getelementptr i8, i8* %this, i32 " + offset + "\n";
            code += "\t" + castRegister + " = bitcast i8* " + getFieldRegister + " to " + llType + "* \n";
            assignmentRegister = castRegister;
        }else{
            assignmentRegister = "%" + idName;
        }
        writeCode(code);
        String exprRegister = n.f2.accept(this, null);
        String storeCode = "\tstore " + llType + " " + exprRegister + ", " + llType + "* " + assignmentRegister + "\n";
        writeCode(storeCode);
        return null;
    }

    /**
     * f0 -> Identifier()
     * f1 -> "["
     * f2 -> Expression()
     * f3 -> "]"
     * f4 -> "="
     * f5 -> Expression()
     * f6 -> ";"
     */
    public String visit(ArrayAssignmentStatement n, Void argu) throws Exception {        
        // registers
        String arrayRegister;
        String arraySizeRegister = getNewRegisterName();
        String comparisonRegister = getNewRegisterName();
        String realIndexRegister = getNewRegisterName();
        String elementRegister = getNewRegisterName();
        // labels
        String assignmentLabel = getNewOobLabelName();
        String exceptionLabel = getNewOobLabelName();
        String continueLabel = getNewOobLabelName();
        //code
        String idName = n.f0.accept(this, null);
        String idType = Main.ST.lookup(idName);
        boolean idIsField = Main.ST.idetifierIsClassField(idName);
        if (idIsField){
            int offset = Main.ST.getFieldOffset(Main.ST.getCurrentClassName(), idName) + 8;
            // registers
            String getElementRegister = getNewRegisterName();
            String castRegister = getNewRegisterName();
            String loadRegister = getNewRegisterName();
            // code
            String getElementCode  = "\t" + getElementRegister + " = getelementptr i8, i8* %this, i32 "  + offset + "\n";
            String castCode = "\t" + castRegister + " = bitcast i8* " + getElementRegister + " to i32**\n";
            String loadCode = "\t" + loadRegister + " = load i32*, i32** " + castRegister + "\n";
            arrayRegister = loadRegister;
            writeCode(getElementCode+castCode+loadCode);
        }else{
            String variableRegister = getNewRegisterName();
            String loadCode = "\t" + variableRegister + " = load i32*, i32** %" + idName + "\n";
            writeCode(loadCode);
            arrayRegister = variableRegister;
        }
        String indexRegister = n.f2.accept(this, null);
        String loadSizeCode = "\t" + arraySizeRegister + " = load i32, i32* " + arrayRegister + "\n";
        String comparisonCode = "\t" + comparisonRegister + " = icmp ult i32 " + indexRegister + ", " + arraySizeRegister + "\n";
        String branchCode = "\tbr i1 " + comparisonRegister + ", label %" + assignmentLabel + ", label %" + exceptionLabel + "\n";
        String assignmentLabelCode  = "\n" + assignmentLabel + ":\n";
        writeCode(loadSizeCode
                + comparisonCode
                + branchCode
                + assignmentLabelCode);
        // assignment
        String exprRegister= n.f5.accept(this, null);
        String realIndexCode = "\t" + realIndexRegister + " = add i32 " + indexRegister + ", 1\n";
        String getElementCode = "\t" + elementRegister + " = getelementptr i32, i32* " + arrayRegister + ", i32 " + realIndexRegister + "\n";
        String storeExprCode = "\tstore i32 " + exprRegister + ", i32* " + elementRegister + "\n";
        String brConitnueCode = "\tbr label %" + continueLabel + "\n";

        // throw exception code
        String throwLabelCode = "\n" + exceptionLabel + ":\n";
        String callThrowCode = "\tcall void @throw_oob()\n";
        String obligBranchCode = "\tbr label %" + continueLabel + "\n";
        String continueLabelCode = "\n" + continueLabel + ":\n";
        writeCode(realIndexCode
                + getElementCode
                + storeExprCode
                + brConitnueCode
                + throwLabelCode
                + callThrowCode
                + obligBranchCode
                + continueLabelCode);
        return null;
    }

    /**
     * f0 -> "("
     * f1 -> Expression()
     * f2 -> ")"
     */
    public String visit(BracketExpression n, Void argu) throws Exception {
        String registerName = n.f1.accept(this, null);
        return registerName;
    }

    /**
     * f0 -> BooleanArrayType()
     *       | IntegerArrayType()
    */
    public String visit(ArrayType n, Void argu) throws Exception {
        String type = n.f0.accept(this, null);
        return type;
    }

    public String visit(IntegerArrayType n, Void argu) throws Exception {
        return "int[]";
    }

    public String visit(BooleanArrayType n, Void argu) throws Exception {
        return "boolean[]";
    }

    @Override
    public String visit(BooleanType n, Void argu) {
        return "boolean";
    }

    @Override
    public String visit(IntegerType n, Void argu) {
        return "int";
    }

    /**
        * f0 -> <IDENTIFIER>
    */
    @Override
    public String visit(Identifier n, Void argu) throws Exception {
        String idName =  n.f0.toString();
        if (this.returnIdName){
         // case 1: another visitor needs just the name
            return idName;
        }else{
            this.returnIdName = true;
        }
        // case 2: we need the pseudo assembly code for the identifier
        String idType = Main.ST.lookup(idName);
        String llvmType = getLLtype(idType);
        // set method class for method call
        if (this.returnIdType){
            this.primaryExpressionType = idType;
            this.returnIdType = false;
        }
        boolean idIsField = Main.ST.idetifierIsClassField(idName);
        if (idIsField) {
            String currentClassName = Main.ST.getCurrentClassName();
            int offset = Main.ST.getFieldOffset(currentClassName, idName) + 8;
            // registers
            String getFieldRegister = getNewRegisterName();
            String castRegister = getNewRegisterName();
            String loadRegister = getNewRegisterName();
            //code
            String getFieldCode = "\t" + getFieldRegister + " = getelementptr i8, i8* %this, i32 "  + offset + "\n";
            String castCode = "\t" + castRegister + " = bitcast i8* " + getFieldRegister + " to " + llvmType + "*\n";
            String loadCode = "\t" + loadRegister + " = load " + llvmType + ", " + llvmType + "* " + castRegister + "\n";
            writeCode(getFieldCode
                    + castCode
                    + loadCode);
            return loadRegister;
        } else {
            String loadRegister = getNewRegisterName();
            String loadCode = "\t" + loadRegister + " = load " + llvmType + ", " + llvmType + "* %" + idName + "\n";
            writeCode(loadCode);
            return loadRegister;
        }
    } 

    /*
     * f0 -> "new"
     * f1 -> Identifier()
     * f2 -> "("
     * f3 -> ")"
    */
    public String visit(AllocationExpression n, Void argu) throws Exception {
        this.returnIdName = true;
        String className = n.f1.accept(this, argu);
        if (this.returnIdType){
            this.primaryExpressionType = className;
            this.returnIdType = false;
        }
        int classSize = Main.ST.getClassSize(className);
        int numberOfMethods = Main.ST.getNumberOfMethods(className);
        // vtable is a an array of methods --> pointer**
        // thus we need a triple pointer(***) to store the vtable there
        // so we cast the calloc adress to triple pointer in order to store there the adress of vtable
        String callocRegister = getNewRegisterName();
        String castRegister =  getNewRegisterName();
        String vtableRegister = getNewRegisterName();
        //this.registerTypes.put(reg1, className);
        String callocCode = "\n\t" + callocRegister + " = call i8* @calloc(i32 1, i32 " + classSize + ")\n";
        String castCode = "\t" + castRegister + " = bitcast i8* " + callocRegister + " to i8***\n";
        String vtableCode = "\t" + vtableRegister + " = getelementptr [" + numberOfMethods + " x i8*], [" + numberOfMethods + " x i8*]* @." + className + "_vtable, i32 0, i32 0\n";
        String storeCode = "\tstore i8** " + vtableRegister + ", i8*** " + castRegister + "\n";

        writeCode(callocCode + castCode + vtableCode + storeCode);
        return callocRegister;
    }

    /**
        * f0 -> "new"
        * f1 -> "int"
        * f2 -> "["
        * f3 -> Expression()
        * f4 -> "]"
    */

    @Override
    public String visit(IntegerArrayAllocationExpression n, Void argu) throws Exception {
        // registers
        String compareRegister = getNewRegisterName();
        String realSizeRegister = getNewRegisterName(); // we need one more element to store the size in order to check for array look ups
        String callocRegister = getNewRegisterName();
        String castRegister = getNewRegisterName();
        String exprRegister =  n.f3.accept(this, null);
        // labels
        String throwExLabel = getNewArrayAllocLabelName();
        String allocateMemLabel = getNewArrayAllocLabelName();
        // code
        String compareCode = "\t" + compareRegister + " = icmp slt i32 " + exprRegister + ", 0\n";
        String branchCode = "\tbr i1" + compareRegister + ", label %" + throwExLabel + ", label %" + allocateMemLabel + "\n";
        String throwLabelCode = "\n" + throwExLabel + ":\n";
        String callThrowCode = "\tcall void @throw_oob()\n";
        String obligBranchCode = "\tbr label %" + allocateMemLabel + "\n";

        String allocLabelCode = "\n" + allocateMemLabel + ":\n";
        String getRealSizeCode = "\t" + realSizeRegister + " = add i32 " + exprRegister + ", 1\n";
        String callCallocCode = "\t" + callocRegister + " = call i8* @calloc(i32 4, i32 " + realSizeRegister + ")\n";
        String castCode = "\t" + castRegister + " = bitcast i8* " + callocRegister + " to i32*\n";
        String storeCode = "\tstore i32 " + exprRegister + ", i32* " + castRegister + "\n";
        writeCode(compareCode
                + branchCode
                + throwLabelCode
                + callThrowCode
                + obligBranchCode
                + allocLabelCode
                + getRealSizeCode
                + callCallocCode
                + castCode
                + storeCode);
        return castRegister;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
    */
    public String visit(ArrayLookup n, Void argu) throws Exception {
        // registers
        String arraySizeRegister = getNewRegisterName();
        String realIndexRegister = getNewRegisterName();
        String comparisonRegister = getNewRegisterName();
        String elementRegister = getNewRegisterName();
        String returnRegister = getNewRegisterName();
        // labels
        String lookupLabel = getNewOobLabelName();
        String exceptionLabel = getNewOobLabelName();
        String continueLabel = getNewOobLabelName();
        // code
        String primaryRegister1 = n.f0.accept(this, null);
        // first element of the array is the size
        String loadArraySizeCode = "\t" + arraySizeRegister + " = load i32, i32* " + primaryRegister1 + "\n";
        String primaryRegister2 = n.f2.accept(this, null);
        String compareCode = "\t" + comparisonRegister + " = icmp ult i32 " + primaryRegister2 + ", " + arraySizeRegister + "\n";
        String branchCode = "\tbr i1 " + comparisonRegister + ", label %" + lookupLabel + ", label %" + exceptionLabel + "\n";
        writeCode(loadArraySizeCode
                + compareCode
                + branchCode);
        // lookup Code
        String lookupLabelCode = "\n" + lookupLabel + ":\n";
        String realIndexCode = "\t" + realIndexRegister + " = add i32 " + primaryRegister2 + ", 1\n";
        String getElementCode = "\t" + elementRegister + " = getelementptr i32, i32* " + primaryRegister1 + ", i32 " + realIndexRegister + "\n";
        String loadElementCode = "\t" + returnRegister + " = load i32, i32* " + elementRegister + "\n";
        String branchContinueCode = "\tbr label %" + continueLabel + "\n";
        writeCode(lookupLabelCode
                + realIndexCode
                + getElementCode
                + loadElementCode
                + branchContinueCode);
        // throw error code
        String exLabelCode = "\n" + exceptionLabel + ":\n";
        String throwOobCode = "\tcall void @throw_oob()\n";
        String continueCode = "\tbr label %" + continueLabel + "\n"; // llvm requires this although throw exits the programm
        String continueLabelCode = "\n" + continueLabel + ":\n";
        writeCode(exLabelCode
                + throwOobCode
                + continueCode
                + continueLabelCode);
        return returnRegister;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
    */
    public String visit(ArrayLength n, Void argu) throws Exception {
        String lengthRegister = getNewRegisterName();
        String firstElement = n.f0.accept(this, null);
        String code = "\t" + lengthRegister + " = load i32, i32* " + firstElement + "\n";
        writeCode(code);
        return lengthRegister;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( ExpressionList() )?
     * f5 -> ")"
    */
    public String visit(MessageSend n, Void argu) throws Exception {
        // primaryExpressionType private var contains the type os f0
        // Visit PrimaryExpression
        this.returnIdType = true;   // if expression an identifier and its actually a subclass we cannot call methods parent does not have
        String primaryRegister = n.f0.accept(this, null);
        this.returnIdName = true;
        String methodName = n.f2.accept(this, null);
        String methodReturnType = Main.ST.getClassMethodType(this.primaryExpressionType, methodName);
        String llMethodReturnType = getLLtype(methodReturnType);
        int methodOffset = Main.ST.getMethodOffset(this.primaryExpressionType, methodName) / 8;
        // registers
        String castRegister = getNewRegisterName();
        String loadRegister = getNewRegisterName();
        String methodRegister = getNewRegisterName();
        String load2Register = getNewRegisterName();
        String cast2Register = getNewRegisterName();
        String callRegister = getNewRegisterName();
        // code
        String castCode = "\t" + castRegister + " = bitcast i8* " + primaryRegister + " to i8***\n";
        String loadCode = "\t" + loadRegister + " = load i8**, i8*** " + castRegister + "\n";
        String getMethodCode = "\t" + methodRegister + " = getelementptr i8*, i8** " + loadRegister + ", i32 " + methodOffset + "\n";
        String load2Code = "\t" + load2Register + " = load i8*, i8** " + methodRegister + "\n";
        String cast2Code = "\t" + cast2Register + " = bitcast i8* " + load2Register + " to " + llMethodReturnType + "(i8*";

                
        String[] argTypes = argArray(Main.ST.getClassMethodArgs(this.primaryExpressionType, methodName));
        for(int i = 0; i < argTypes.length; i++){
            String llArgType = getLLtype(argTypes[i]);
            cast2Code += ", " + llArgType;
        }
        cast2Code += ")*\n";
        writeCode(castCode
                + loadCode
                + getMethodCode
                + load2Code
                + cast2Code);

        String[] argumentArray = argArray(n.f4.present() ? n.f4.accept(this, null) : "");
        String arguments = "";
        for(int i = 0; i < argumentArray.length; i++){
            String llArgType = getLLtype(argTypes[i]);
            arguments += ", " + llArgType + " " + argumentArray[i];
        }
        String callCode = "\t" + callRegister + " = call " + llMethodReturnType + " " + cast2Register + "(i8* " + primaryRegister + arguments + ")\n";
        writeCode(callCode);
        return callRegister;
    }

    /**
    * f0 -> Expression()
    * f1 -> ExpressionTail()
    */
    @Override
    public String visit(ExpressionList n, Void argu) throws Exception {
        return n.f0.accept(this, null) + n.f1.accept(this, null);
    }

    /**
        * f0 -> ( ExpressionTerm() )*
    */
    @Override
    public String visit(ExpressionTail n, Void argu) throws Exception {
        String ret = "";
        for (Node node: n.f0.nodes) {
            ret += "," + node.accept(this, null);
        }
        return ret;
    }
    /**
    * f0 -> ","
    * f1 -> Expression()
    */
    @Override
    public String visit(ExpressionTerm n, Void argu) throws Exception {
        String exprReg = n.f1.accept(this, null);
        return exprReg;
    }

    /**
     * f0 -> Clause()
     * f1 -> "&&"
     * f2 -> Clause()
    */
    public String visit(AndExpression n, Void str) throws Exception {
        // registers
        String phiRegigster = getNewRegisterName();
        // labels
        String label1 = getNewAndLabelName();
        String label2 = getNewAndLabelName();
        String label3 = getNewAndLabelName();
        String label4 = getNewAndLabelName();
        //code
        String clauseRegister1 = n.f0.accept(this, null);
        writeCode("\tbr label %" + label1 + "\n"
                + "\n" + label1 + ":\n"
                + "\tbr i1 " + clauseRegister1 + ", label %" + label2 + ", label %" + label3 + "\n"
                + "\n" + label2 + ":\n");
        String clauseRegister2 = n.f2.accept(this, null);
        writeCode("\tbr label %" + label3 + "\n"
                + "\n" + label3 + ":\n"
                + "\tbr label %" + label4 + "\n"
                + "\n" + label4 + ":\n"
                + "\t" + phiRegigster + " = phi i1 [ 0, %" + label1 + " ], [ " + clauseRegister2 + ", %" + label3 + " ]\n");
        return phiRegigster;
    }

    /**
     * f0 -> "!"
     * f1 -> Clause()
    */
    public String visit(NotExpression n, Void str) throws Exception {
        String clauseRegister = n.f1.accept(this, null);
        String notRegister = getNewRegisterName();
        String notCode = "\t" + notRegister + " = xor i1 1, " + clauseRegister + "\n";
        writeCode(notCode);
        return notRegister;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
    */ 
    public String visit(CompareExpression n, Void str) throws Exception {
        String primaryRegister1 = n.f0.accept(this, null);
        String primaryRegister2 = n.f2.accept(this, null);
        String compareRegister = getNewRegisterName();
        String compareCode = "\t" + compareRegister + " = icmp slt i32 " + primaryRegister1 + ", " + primaryRegister2 + "\n";
        writeCode(compareCode);
        return compareRegister;
    }


    /**
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
    */
    public String visit(PlusExpression n, Void str) throws Exception {
        String primaryRegister1 = n.f0.accept(this, null);
        String primaryRegister2 = n.f2.accept(this, null);
        String plusRegister = getNewRegisterName();
        String plusCode = "\t" + plusRegister + " = add i32 " + primaryRegister1 + ", " + primaryRegister2 + "\n";
        writeCode(plusCode);
        return plusRegister;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
    */
    public String visit(MinusExpression n, Void str) throws Exception {
        String primaryRegister1 = n.f0.accept(this, null);
        String primaryRegister2 = n.f2.accept(this, null);
        String minusRegister = getNewRegisterName();
        String minusCode = "\t" + minusRegister + " = sub i32 " + primaryRegister1 + ", " + primaryRegister2 + "\n";
        writeCode(minusCode);
        return minusRegister;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
    */
    public String visit(TimesExpression n, Void str) throws Exception {
        String primaryRegister1 = n.f0.accept(this, null);
        String primaryRegister2 = n.f2.accept(this, null);
        String timesRegister = getNewRegisterName();
        String timesCode = "\t" + timesRegister + " = mul i32 " + primaryRegister1 + ", " + primaryRegister2 + "\n";
        writeCode(timesCode);
        return timesRegister;
    }

    /**
     * f0 -> "if"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     * f5 -> "else"
     * f6 -> Statement()
    */
    public String visit(IfStatement n, Void str) throws Exception {
        // registers
        String exprRegister = n.f2.accept(this, null);
        // labels
        String ifLabel = getNewIfLabelName();
        String elseLabel = getNewIfLabelName();
        String exitLabel = getNewIfLabelName();
        //code
        // BUGGG
        String branchConditionCode = "\tbr i1 " + exprRegister + ", label %" + ifLabel + ", label %" + elseLabel + "\n";
        String ifLabelCode = "\n" + ifLabel + ":\n";
        writeCode(branchConditionCode
                + ifLabelCode);
        // write if statements
        n.f4.accept(this, null);
        // if statements have been written
        String exitIfCode = "\n\tbr label %" + exitLabel + "\n";
        String elseLabelCode = "\n" + elseLabel + ":\n";
        writeCode(exitIfCode
                + elseLabelCode);
        // write else statements
        n.f6.accept(this, null);
        // else statements have been written
        String exitCode = "\n\tbr label %" + exitLabel + "\n";
        String exitLabelCode = "\n" + exitLabel + ":\n";
        writeCode(exitCode
                + exitLabelCode);
        return null;
    }

    /**
     * f0 -> "while"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
    */
    public String visit(WhileStatement n, Void argu) throws Exception {
        // registers

        // labels
        String whileLabel = getNewLoopLabelName();
        String continueLabel = getNewLoopLabelName();
        String breakLabel = getNewLoopLabelName();
        //code
        String branchWhileCode = "\n\tbr label %" + whileLabel + "\n";
        String whileLabelCode   ="\n" + whileLabel + ":\n";
        writeCode(branchWhileCode
                + whileLabelCode);
        String exprRegister = n.f2.accept(this, null);
        String branchConditionCode = "\tbr i1 " + exprRegister + ", label %" + continueLabel + ", label %" + breakLabel + "\n";
        String continueLabelCode = "\n" + continueLabel + ":\n";
        writeCode(branchConditionCode 
                + continueLabelCode);
        // write while code
        n.f4.accept(this, null);
        // statements have been written
        String branchContinueCode = "\n\tbr label %" + whileLabel + "\n";
        String breakLabelCode = "\n" + breakLabel + ":\n"; 
        writeCode(branchContinueCode
                + breakLabelCode);
        return null;
    }

    /**
     * f0 -> "System.out.println"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> ";"
    */
    public String visit(PrintStatement n, Void str) throws Exception {
        String exprRegister = n.f2.accept(this, null);
        String printCode = "\tcall void (i32) @print_int(i32 " + exprRegister + ")\n";
        writeCode(printCode);
        return null;
    }

    /**
     * f0 -> IntegerLiteral()
     * | TrueLiteral()
     * | FalseLiteral()
     * | Identifier()
     * | ThisExpression()
     * | ArrayAllocationExpression()
     * | AllocationExpression()
     * | BracketExpression()
    */
    public String visit(PrimaryExpression n, Void argu) throws Exception {
        this.returnIdName = false; // we want identifier visitor to return refister 
        String expression = n.f0.accept(this, argu);
        this.returnIdName = true;
        if (expression.matches("-?\\d+")) {
            return expression;
        }else  if (expression.equals("true")) {
            return "1";
        } else if (expression.equals("false")) {
            return "0";
        } else if (expression.equals("this")) {
            if (this.returnIdType){
                this.primaryExpressionType = Main.ST.getCurrentClassName();
                this.returnIdType = false;
            }
            return "%this";
        }
        return expression;
    }

    /**
     * f0 -> <INTEGER_LITERAL>
    */
    public String visit(IntegerLiteral n, Void argu) throws Exception {
        return n.f0.toString();
    }

    /**
     * f0 -> "true"
    */
    public String visit(TrueLiteral n, Void argu) throws Exception {
      return "true";
    }

    /**
     * f0 -> "false"
    */
    public String visit(FalseLiteral n, Void argu) throws Exception {
        return "false";
    }
    /**
     * f0 -> "this"
    */
    public String visit(ThisExpression n, Void argu) throws Exception {
        return "this";
    }
}