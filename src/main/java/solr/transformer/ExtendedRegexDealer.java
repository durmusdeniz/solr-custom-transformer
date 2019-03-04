package solr.transformer;

import org.apache.log4j.Logger;
import org.apache.solr.handler.dataimport.Context;
import org.apache.solr.handler.dataimport.DataImporter;
import org.apache.solr.handler.dataimport.Transformer;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ExtendedRegexDealer extends Transformer
{


    private static final Logger logger = Logger.getLogger(ExtendedRegexDealer.class.getName());
    private Map<String, Pattern> PATTERN_CACHE = new HashMap<>();

    public static final String REGEX = "regex";
    public static final String REPLACE_WITH = "replaceWith";
    public static final String SPLIT_BY = "splitBy";
    public static final String SRC_COL_NAME = "sourceColName";
    public static final String GROUP_NAMES = "groupNames";
    public static final String EXTENDED_REGEX = "extendedRegex";




    private Object process(String col,
                           String reStr,
                           String splitBy,
                           String replaceWith,
                           String value,
                           String groupNames,
                           String extendedRegex){

        if(extendedRegex != null){
            return specialParser(extendedRegex, value);
        }else if(splitBy != null){
            return readBySplit(splitBy, value);
        }else if(replaceWith != null){
            Pattern p = getPattern(reStr);
            Matcher m = p.matcher(value);
            return m.find()?m.replaceAll(replaceWith):value;
        }else{
            return readRexEx(reStr, value, col, groupNames);
        }

    }


    private List<String> specialParser(String customRegex, String value){

        List<String> matchingList = new ArrayList<>();
        Pattern p = Pattern.compile(customRegex);
        Matcher m = p.matcher(value);
        while(m.find()){
            matchingList.add(m.group(1));
        }
        return matchingList;

    }

    private List<String> readBySplit(String splitter, String value){
        return Arrays.asList(value.split(splitter));
    }

    private Pattern getPattern(String reStr){
        Pattern res = PATTERN_CACHE.get(reStr);
        if(res == null){
            PATTERN_CACHE.put(reStr, res = Pattern.compile(reStr));
        }
        return res;
    }

    private Object readRexEx(String reStr, String value, String colName, String groupNames){

        String[] gNames = null;

        if(groupNames != null && groupNames.trim().length() > 0){
            gNames = groupNames.split(",");
        }

        Pattern regexp = getPattern(reStr);
        Matcher m = regexp.matcher(value);

        if(m.find() && m.groupCount() > 0){
            if(m.groupCount() > 1){
                List l = null;
                Map<String, String> map = null;
                if(gNames == null){
                    l = new ArrayList();
                }else{
                    map = new HashMap<>();
                }
                for(int i = 1; i <=m.groupCount(); i++){
                    try{
                        if(l != null){
                            l.add(m.group(i));
                        }else if(map != null){
                            if(i <= gNames.length){
                                String nameOfGroup = gNames[i-1];
                                if(nameOfGroup!=null && nameOfGroup.trim().length()>0){
                                    map.put(nameOfGroup, m.group(i));
                                }
                            }
                        }
                    }catch (Exception e){
                        logger.warn("Parsing Failure on Field: " + colName, e);
                    }
                }
                return l == null ? map:l;
            }else{
                return m.group(1);
            }
        }

        return null;

    }

    @Override
    public Object transformRow(Map<String, Object> map, Context context) {

        List<Map<String, String>> fields = context.getAllEntityFields();
        for(Map<String, String> field : fields){
            String col = field.get(DataImporter.COLUMN);
            String reStr = context.replaceTokens(field.get(REGEX));
            String splitBy = context.replaceTokens(field.get(SPLIT_BY));
            String replaceWith = context.replaceTokens(field.get(REPLACE_WITH));
            String groupNames = context.replaceTokens(field.get(GROUP_NAMES));
            String extendedRegex = context.replaceTokens(field.get(EXTENDED_REGEX));


            if(reStr != null || splitBy != null || extendedRegex !=  null){

                String srcColName = field.get(SRC_COL_NAME);
                if(srcColName == null){
                    srcColName = col;
                }
                Object tmpVal = map.get(srcColName);

                if(tmpVal == null)
                    continue;

                if(tmpVal instanceof List){
                    List<String> inputs = (List<String>) tmpVal;
                    List results = new ArrayList();
                    Map<String, List> otherVars  = null;

                    for(String input : inputs){
                        Object o = process(col, reStr,splitBy, replaceWith, input, groupNames, extendedRegex);
                        if(o!= null){
                            if(o instanceof Map){
                                Map oMap = (Map)o;
                                for(Object e : oMap.entrySet()){
                                    Map.Entry<String, Object> entry = (Map.Entry<String, Object>) e;
                                    List l = results;

                                    if(!col.equals(entry.getKey())){
                                        if(otherVars == null){
                                            otherVars = new HashMap<>();
                                        }
                                        if(l == null){
                                            l = new ArrayList();
                                            otherVars.put(entry.getKey(), l);
                                        }
                                    }

                                    if(entry.getValue() instanceof Collection){
                                        l.addAll((Collection)entry.getValue());
                                    }else{
                                        l.add(entry.getValue());
                                    }




                                }
                            }else{
                                if(o instanceof Collection){
                                    results.addAll((Collection)o);
                                }else{
                                    results.add(o);
                                }
                            }
                        }
                    }


                    map.put(col, results);
                    if(otherVars!=null) map.putAll(otherVars);
                }else{
                    String value = tmpVal.toString();
                    Object o = process(col, reStr, splitBy, replaceWith, value, groupNames, extendedRegex);
                    if(o != null){
                        if(o instanceof Map){
                            map.putAll((Map)o);
                        }else{
                            map.put(col, o);
                        }
                    }
                }

            }

        }
        return map;
    }
}
