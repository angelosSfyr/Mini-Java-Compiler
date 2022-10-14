import java.util.*; 


class MiniJavaExtendedClass extends MiniJavaClass {


    public MiniJavaExtendedClass(String name, MiniJavaClass parentClass){
        super(name);
        this.myParentName = parentClass.className;
        this.size = parentClass.size;
        this.methodNames = new ArrayList<String>(parentClass.methodNames);
        this.insertParentFields(parentClass);
        this.insertParentMethods(parentClass);
    }

    private void insertParentFields(MiniJavaClass parentClass){
        Iterator <Map.Entry<String, String>> itr = parentClass.fields.entrySet().iterator();
        Map.Entry <String, String> entry = null;
        while(itr.hasNext()){
            entry = itr.next();
            String fieldName = entry.getKey();
            String fieldType = entry.getValue();
            this.fields.put(fieldName, fieldType);
        }
    }

    private void insertParentMethods(MiniJavaClass parentClass){
        Iterator <Map.Entry<String, MiniJavaMethod>> itr = parentClass.methods.entrySet().iterator();
        Map.Entry <String, MiniJavaMethod> entry = null;
        while(itr.hasNext()){
            entry = itr.next();
            String methodName = entry.getKey();
            MiniJavaMethod method= entry.getValue();
            this.methods.put(methodName, method);
        }
    }

    public String toString() {
        String strMethods = "";
        Iterator<Map.Entry<String, MiniJavaMethod>> itr = this.methods.entrySet().iterator();
        Map.Entry<String, MiniJavaMethod> entry = null;
        while(itr.hasNext()){
            entry = itr.next();
            strMethods += "\t"+entry.getValue()+"\n";
        }
        return this.className + " " + fields + "\n" + strMethods; // add fields and methods
    }
}