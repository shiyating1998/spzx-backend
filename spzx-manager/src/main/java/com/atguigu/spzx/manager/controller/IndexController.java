package com.atguigu.spzx.manager.controller;


import com.atguigu.spzx.manager.service.SysMenuService;
import com.atguigu.spzx.manager.service.SysUserService;
import com.atguigu.spzx.manager.service.ValidationCodeService;
import com.atguigu.spzx.model.dto.system.LoginDto;
import com.atguigu.spzx.model.entity.system.SysUser;
import com.atguigu.spzx.model.vo.common.Result;
import com.atguigu.spzx.model.vo.common.ResultCodeEnum;
import com.atguigu.spzx.model.vo.h5.UserInfoVo;
import com.atguigu.spzx.model.vo.system.LoginVo;
import com.atguigu.spzx.model.vo.system.SysMenuVo;
import com.atguigu.spzx.model.vo.system.ValidateCodeVo;
import com.atguigu.spzx.utils.AuthContextUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "用户接口")
@RestController
@CrossOrigin(origins = "http://localhost:3002")
@RequestMapping(value = "/admin/system/index")
public class IndexController {

    @Autowired
    private SysUserService sysUserService ;

    @Autowired
    private ValidationCodeService validationCodeService;

    @Autowired
    private SysMenuService sysMenuService;

    @GetMapping("/menus")
    public Result menus() {
        List<SysMenuVo> sysMenuVoList =  sysMenuService.findUserMenuList() ;
        return Result.build(sysMenuVoList , ResultCodeEnum.SUCCESS) ;
    }

    @GetMapping("/logout")
    public Result logout(@RequestHeader(name = "token") String token){
        sysUserService.logout(token);
        return Result.build(null, ResultCodeEnum.SUCCESS);
    }

//    @GetMapping("/getUserInfo")
//    public Result getUserInfo(@RequestHeader(name = "token") String token){
//        SysUser sysUser = sysUserService.getUserInfo(token);
//        return Result.build(sysUser,ResultCodeEnum.SUCCESS);
//    }

    @GetMapping("/getUserInfo")
    public Result getUserInfo(){
        System.out.println(AuthContextUtil.get());
        return Result.build(AuthContextUtil.get(),ResultCodeEnum.SUCCESS);
    }

    @GetMapping("/generateValidationCode")
    public Result<ValidateCodeVo> generateValidateCode(){
        ValidateCodeVo validateCodeVo = validationCodeService.generateValidationCode();

        return Result.build(validateCodeVo, ResultCodeEnum.SUCCESS);
    }

    @Operation(summary = "登录接口(swaggerT)")
    @PostMapping(value = "/login")
    public Result<LoginVo> login(@RequestBody LoginDto loginDto) {
        //loginDto得到用户提交的表单数据

        //call service 的login，得到loginVo
        LoginVo loginVo = sysUserService.login(loginDto) ;

        //返回Result，一个封装的返回类，适用于所有的API返回
        return Result.build(loginVo , ResultCodeEnum.SUCCESS) ;
    }

}