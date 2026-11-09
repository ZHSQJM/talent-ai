package com.talent.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * @author watson
 * @date 2026年/11月/05日/13:46
 */
@Component
@Slf4j
public class WeatherMcpTools {


    @McpTool(name = "getWeatherForecast",
    description = "根据城市名称查询模拟天气预报",
    generateOutputSchema = true)
    public String getWeatherForecast(@McpToolParam(description = "城市的名称，例如 上海，北京",required = true)
                                     String city){

        log.info("执行到天气遇到的mcp-server");
        return city +"天气很好";
    }
}
