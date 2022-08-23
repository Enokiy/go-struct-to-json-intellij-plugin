package com.github.enokiy;

import com.goide.psi.*;
import com.google.gson.GsonBuilder;
import com.intellij.psi.PsiElement;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created on 2021-08-28
 *
 * @author: enokiy
 */
public class Utils {

    private static final Map<String, Object> basicTypes = new HashMap<>();
    private static final String  STRUCT_TYPE = "STRUCT_TYPE";

    static {
        basicTypes.put("bool", false);
        basicTypes.put("byte", 0);
        basicTypes.put("int", 0);
        basicTypes.put("uint", 0);
        basicTypes.put("uint8", 255);
        basicTypes.put("uint16", 65535);
        basicTypes.put("uint32", 4294967295L);
        basicTypes.put("uint64", 1844674407370955161L);
        basicTypes.put("int8", -128);
        basicTypes.put("int16", -32768);
        basicTypes.put("int32", -2147483648);
        basicTypes.put("int64", -9223372036854775808L);
        basicTypes.put("uintptr", 0); //uintptr is an integer type that is large enough to hold the bit pattern of any pointer
        basicTypes.put("rune", 0);  // rune is an alias for int32 and is equivalent to int32 in all ways
        basicTypes.put("long", 0L);
        basicTypes.put("float32", 0.0F);
        basicTypes.put("float64", 0.0F);
        basicTypes.put("string", "demoString");
        basicTypes.put("time.Time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
    }

    public static boolean isBasicType(String typeName) {
        return basicTypes.containsKey(typeName);
    }

    public static String convertGoStructToJson(GoStructType goStructType) {

        Map<String, Object> map = getKVMap(goStructType);
        return new GsonBuilder().setPrettyPrinting().create().toJson(map);
    }

    /*
    // The encoding of each struct field can be customized by the format string
    // stored under the "json" key in the struct field's tag.
    // As a special case, if the field tag is "-", the field is always omitted.
    //
    //  Field int `json:"myName"` -->Field appears in JSON as key "myName".
    //  Field int `json:"myName,omitempty"` -->Field appears in JSON as key "myName" and the field is omitted from the object if its value is empty,
    //   Field int `json:"-"`    -->  Field is ignored by this package
    //   Field int `json:"-"`    --> Field is ignored by this package.
    //   Field int `json:"-,"`   --> Field appears in JSON as key "-".
     */
    private static String getJsonKeyName(String fieldName, String tagText) {
        String jsonKey = fieldName;
        if (tagText == null || tagText.equals("")) {
            return jsonKey;
        }
        String regPattern = "[json|redis]:\"([\\w\\d_,-\\.]+)\"";
        Pattern pattern = Pattern.compile(regPattern);
        Matcher matcher = pattern.matcher(tagText);
        if (matcher.find()) {
            String tmpKeyName = matcher.group(1).split(",")[0];
            if (!Objects.equals(tmpKeyName, "-")) { // for now,don't omit any field
                jsonKey = tmpKeyName;
            }
        }
        return jsonKey;
    }

    /**
     * demo struct:
     * type Person struct {
     * Name string `json:"name"`
     * Age  int    `json:"age"`
     * Addr string `json:"addr"`
     * }
     * <p>
     * type Employee struct {
     * Person
     * salary int `json:"salary"`
     * Dep    struct {
     * Number int    `json:"dep_number"`
     * Name   string `json:"dep_name"`
     * } `json:"dep"`
     * }
     *
     * @param goStructType
     * @return
     */
    private static Map<String, Object> getKVMap(GoStructType goStructType) {
        Map<String, Object> map = new LinkedHashMap<>();

        List<GoFieldDeclaration> fieldsDeclareList = goStructType.getFieldDeclarationList();

        for (GoFieldDeclaration field : fieldsDeclareList) {
            GoType fieldType = field.getType();
            if (fieldType == null) {  // to deal with Person in Employee
                GoAnonymousFieldDefinition anonymous = field.getAnonymousFieldDefinition();
                if (anonymous != null) {
                    GoTypeReferenceExpression typeRef = anonymous.getTypeReferenceExpression();
                    PsiElement resolve = typeRef != null ? typeRef.resolve() : null;
                    if (resolve instanceof GoTypeSpec) {
                        GoTypeSpec typeSpec = (GoTypeSpec) resolve;
                        GoType type = typeSpec.getSpecType().getType();
                        if (type instanceof GoStructType) {
                            Map<String, Object> tmpMap = getKVMap((GoStructType) type);
                            map.putAll(tmpMap);
                        }
                    }
                }
            } else {
                String fieldName = field.getFieldDefinitionList().get(0).getIdentifier().getText();
                String fieldTagText = field.getTagText();
                GoTypeReferenceExpression typeRef = fieldType.getTypeReferenceExpression();
                String fieldTypeStr = typeRef == null ? "NOTBASICTYPE" : typeRef.getText();

                String jsonKey = getJsonKeyName(fieldName, fieldTagText);

                if (isBasicType(fieldTypeStr)) {
                    map.put(jsonKey, basicTypes.get(fieldTypeStr));
                } else if (fieldType instanceof GoStructType) {
                    Map<String, Object> tmpMap = getKVMap((GoStructType) field.getType());
                    map.put(jsonKey, tmpMap);
                }else if (fieldType instanceof GoMapType) {
                    Map<String, Object> tmpMap = new HashMap<>();
                    // key type default to be string
                    String tmpValueType = Objects.requireNonNull(((GoMapType) fieldType).getValueType()).getText();
                    if (isBasicType(tmpValueType)) {
                        tmpMap.put("demoKey", basicTypes.get(tmpValueType));
                    } else {
                        GoTypeReferenceExpression typeRef1 = Objects.requireNonNull(((GoMapType) fieldType).getValueType()).getTypeReferenceExpression();
                        PsiElement mapValueResolve = typeRef1 != null ? typeRef1.resolve() : null;
                        if (mapValueResolve instanceof GoTypeSpec) {
                            GoTypeSpec typeSpec = (GoTypeSpec) mapValueResolve;
                            GoType type = typeSpec.getSpecType().getType();
                            if (type instanceof GoStructType) {
                                Map<String, Object> valueMap = getKVMap((GoStructType) type);
                                tmpMap.put("demoKey", valueMap);
                            }
                        }
                    }
                    map.put(jsonKey, tmpMap);
                } else if (fieldType instanceof GoArrayOrSliceType) {
                    ArrayList<Object> tmpList = new ArrayList<>();
                    String tmpStr = ((GoArrayOrSliceType) fieldType).getType().getText();
                    if (isBasicType(tmpStr)) {
                        tmpList.add(basicTypes.get(tmpStr));
                    } else {
                        GoTypeReferenceExpression goTypeRef2 = ((GoArrayOrSliceType) fieldType).getType().getTypeReferenceExpression();
                        PsiElement listResolve = goTypeRef2 != null ? goTypeRef2.resolve() : null;
                        if (listResolve instanceof GoTypeSpec) {
                            GoTypeSpec typeSpec = (GoTypeSpec) listResolve;
                            GoType type = typeSpec.getSpecType().getType();
                            if (type instanceof GoStructType) {
                                Map<String, Object> tmpMap = getKVMap((GoStructType) type);
                                tmpList.add(tmpMap);
                            }
                        }
                    }
                    map.put(jsonKey, tmpList);
                } else if (fieldType instanceof GoPointerType) {
                    // todo pointer type
                    
                    map.put(jsonKey, new HashMap<>());
                }else if (fieldType instanceof GoInterfaceType){
                    map.put(jsonKey, new HashMap<>());
                }
            }
        }
        return map;
    }
}