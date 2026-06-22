package com.xusuki;

import com.google.gson.*;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.lang.Boolean;
import java.util.LinkedHashMap;
import java.util.Map;

public class JsonManager{

    private final Path workPath;  //目录确定
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Map.class, new IntegerPreservingMapTypeAdapter())
            .create();

    /**
     * 自定义适配器：读取时强制将整数转为 Integer，避免变成 Double
     */
    private static class IntegerPreservingMapTypeAdapter implements JsonDeserializer<Map<String, Object>> {
        @Override
        public Map<String, Object> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            Map<String, Object> map = new LinkedHashMap<>();
            JsonObject jsonObject = json.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                JsonElement elem = entry.getValue();
                if (elem.isJsonPrimitive()) {
                    JsonPrimitive primitive = elem.getAsJsonPrimitive();
                    // 如果是数字，检查是否有小数点
                    if (primitive.isNumber()) {
                        String numStr = primitive.getAsString();
                        // 关键点：如果没有小数点，强制转为 Integer
                        if (numStr.indexOf('.') == -1) {
                            map.put(entry.getKey(), primitive.getAsInt());
                        } else {
                            map.put(entry.getKey(), primitive.getAsDouble());
                        }
                    } else {
                        // 非数字类型按原样放入
                        map.put(entry.getKey(), context.deserialize(elem, Object.class));
                    }
                } else {
                    map.put(entry.getKey(), context.deserialize(elem, Object.class));
                }
            }
            return map;
        }
    }

    /**
     * @param path  工作目录路径
     * @param fileName  文件名(自动添加.json后缀)
     */
    public JsonManager(String path,String fileName) {
        this(path, fileName,null);
    }

    /**
     *
     * @param path  工作目录路径
     * @param fileName  文件名(自动添加.json后缀)
     * @param topComment  信息(ket:_comment)
     */
    public JsonManager(String path,String fileName,String topComment) {  //必须传入path,filename

        if(path == null || path.trim().isEmpty()){  //判断path
            throw new IllegalArgumentException("path is null or empty");
        }

        if (fileName == null || fileName.trim().isEmpty()){
            throw new IllegalArgumentException("fileName is null or empty");
        }

        if(!fileName.toLowerCase().endsWith(".json")){
            fileName += ".json";
        }

        Path dir = Paths.get(path).normalize().toAbsolutePath();  //路径定义

        if(!Files.exists(dir)){
            try {
                Files.createDirectories(dir);  //创建目录
            } catch (IOException _) {}
        }

        this.workPath = dir.resolve(fileName);

        if (!Files.exists(this.workPath)){

            String initialContent;

            if(topComment != null && !topComment.trim().isEmpty()){
                String escapedComment = topComment.replace("\\","\\\\").replace("\"","\\\"");
                initialContent  = "{\"_comment\":\"" + escapedComment + "\"}";
            }else {
                initialContent = "{}";
            }

            try {
                Files.writeString(this.workPath, initialContent, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("Failed to write initial JSON file: " + this.workPath, e);
            }
        }
    }

    /**
     *
     * @param key  键名
     * @param value  键值
     */
    public void setValue(String key, Object value) {
        if(key == null || key.trim().isEmpty()){
            throw new IllegalArgumentException("key is null or empty");
        }
        if(!isAllowedType(value)){
            throw new IllegalArgumentException("value is not allowed type");
        }

        try{
            Map<String, Object> data = readJson();

            data.put(key, value);

            String json = gson.toJson(data);
            Files.writeString(workPath,json,StandardCharsets.UTF_8);

        }catch (Exception _){
        }
    }

    /**
     *
     * @param key  传入键
     * @return value 返回值
     */
    public Object getValue(String key) {

        if(key == null || key.trim().isEmpty()){
            throw new IllegalArgumentException("key is null or empty");
        }

        try{
            Map<String, Object> data = readJson();
            return data.get(key);
        }catch (Exception e){
            return null;
        }
    }

    private Map<String, Object> readJson() throws IOException{

        if(!Files.exists(workPath) || Files.size(workPath) == 0){
            return new LinkedHashMap<>();
        }

        String content = Files.readString(workPath, StandardCharsets.UTF_8).trim();

        return gson.fromJson(content, Map.class);

    }

    /**
     * 获取当前 JSON 文件中的所有数据。
     *
     * @return 包含所有键值对的 Map，如果文件为空或不存在则返回空 Map。
     *         如果读取失败，返回空 Map（不抛出异常）。
     */
    public Map<String, Object> getAll() {
        try {
            return readJson();
        } catch (IOException e) {
            // 读取失败时返回空 Map，保持与其他方法一致的异常处理风格
            return new LinkedHashMap<>();
        }
    }

    /**
     * 获取当前 JSON 文件的完整内容字符串（已格式化）。
     *
     * @return 格式化后的 JSON 字符串，如果文件为空或读取失败则返回 "{}"。
     */
    public String getAllAsString() {
        try {
            Map<String, Object> data = readJson();
            return gson.toJson(data);
        } catch (IOException e) {
            return "{}";
        }
    }

    private boolean isAllowedType(Object value) {
        return value == null ||
                value instanceof Integer ||
                value instanceof Long ||
                value instanceof Float ||
                value instanceof Double ||
                value instanceof Boolean ||
                value instanceof String;
    }
}