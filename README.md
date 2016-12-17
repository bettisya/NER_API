# 实体检测工具API
### 1. 介绍
利用spring boot将Dnorm(疾病)、tmChem(药物)、stanford_ner（一般实体）三个实体检测工具封装成web services。
### 2. 使用
    整个API打包成一个jar文件,位置为target/demo-0.0.1-SNAPSHOT.jar:
    java -jar target/demo-0.0.1-SNAPSHOT.jar
### 3. 配置
    spring boot + maven
    run.sh将工具所需要的第三方依赖包添加进maven依赖包
### 4. API调用(JSON格式)
    1) HTTP调用：
    
    POST /submit HTTP/1.1
    Host: 10.214.155.245:1123
    Content-Type: application/json
    Cache-Control: no-cache
    
    {"text":"text sentences", "subType": "Disease"}
    
    2) java调用：
    
    OkHttpClient client = new OkHttpClient();
    MediaType mediaType = MediaType.parse("application/json");
    RequestBody body = RequestBody.create(mediaType, "{\"text\":\"test sentences\", \"subType\": \"Disease\"}");
    Request request = new Request.Builder()
    .url("http://10.214.155.245:1123/submit")
    .post(body)
    .addHeader("content-type", "application/json")
    .addHeader("cache-control", "no-cache")
    .build();

    Response response = client.newCall(request).execute();
    
    3) Python调用：
    
    import requests

    url = "http://10.214.155.245:1123/submit"

    payload = "{\"text\":\"test sentences\", \"subType\": \"Disease\"}"
    headers = {
    'content-type': "application/json",
    'cache-control': "no-cache",
    }

    response = requests.request("POST", url, data=payload, headers=headers)

    print(response.text)
### 5. 三个工具的具体使用参考各工具内说明文档。
