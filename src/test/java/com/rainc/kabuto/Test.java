package com.rainc.kabuto;

import cn.hutool.core.date.DateUtil;
import com.rainc.kabuto.async.Kabuto;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * @author rainc
 * @date 2023/4/11
 */
@Component
public class Test {
    @Kabuto
    public void  asyncTest(Date asyncDate,String businessType,String businessId){
        System.out.println("触发时间:"+DateUtil.date(asyncDate));
        System.out.println("运行时间:"+DateUtil.date());
    }
}
