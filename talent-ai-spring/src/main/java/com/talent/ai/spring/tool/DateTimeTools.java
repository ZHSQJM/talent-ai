package com.talent.ai.spring.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author watson
 * @date 2026年/11月/04日/17:01
 */

@Component
@Slf4j
public class DateTimeTools {


    @Tool(description = "获取用户在指定时区的当前日期和时间，用于回答需要实时时间的问题")
    public String getCurrentDateTime(){
        ZoneId zoneId = LocaleContextHolder.getTimeZone().toZoneId();
        ZonedDateTime zonedDateTime = LocalDateTime.now().atZone(zoneId);
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(zonedDateTime);
    }


    @Tool(description = "设置闹钟，调用次工具可在指定时间触发提醒，时间参数必须是ISO-8601格式，例如：2026-05-03 15：30：00")
    public void setAlarm(@ToolParam(description = "闹钟的出发时间，标准格式:yyyy-MM-dd HH:mm:ss")String dateTime){

        log.info("闹钟已设置，在{}提醒用户",dateTime);
    }

}
