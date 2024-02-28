package com.atguigu.spzx.manager.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.CircleCaptcha;
import com.atguigu.spzx.manager.service.ValidationCodeService;
import com.atguigu.spzx.model.vo.system.ValidateCodeVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class ValidationCodeServiceImpl implements ValidationCodeService {

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    // 生成图片验证码
    @Override
    public ValidateCodeVo generateValidationCode() {
        // 1 通过一个工具生成图片验证码
        // hutool
        CircleCaptcha circleCaptcha = CaptchaUtil.createCircleCaptcha(150,48,4,2);
        String code = circleCaptcha.getCode(); // 4-digit code
        String imageBase64 = circleCaptcha.getImageBase64();
        //2 把验证码存储到redis里面，设置redis的key: uuid redis的value:验证码值
        String key = UUID.randomUUID().toString().replaceAll("-","");
        redisTemplate.opsForValue().set("user:validate" +key,
                                        code,
                                        5,
                                        TimeUnit.MINUTES);
        //3 返回ValidateCodeVo对象
        ValidateCodeVo validateCodeVo = new ValidateCodeVo();
        validateCodeVo.setCodeKey(key);
        validateCodeVo.setCodeValue("data:image/png;base64," + imageBase64);
        return validateCodeVo;
    }
}
